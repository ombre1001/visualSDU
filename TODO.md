# TODO

## Refresh Token 用户反向索引与全部设备退出

### 目标

在保持当前单设备退出语义的基础上，为 refresh token 增加用户反向索引，以便后续高效实现 `logout-all`，避免扫描全站 refresh token。

### Redis 结构

```text
vsdu:auth:refresh:{refreshTokenHash} -> userId
vsdu:auth:user-refresh:{userId} -> Set<refreshTokenHash>
```

### 计划流程

- 签发 refresh token：写入 `refresh:{hash}`，同时将 `hash` 加入该用户的 `user-refresh` 集合。
- 轮换 refresh token：消费旧 token 并移除旧索引成员，再写入新 token 和新索引成员。
- 当前设备退出：消费当前 refresh token，并从用户反向索引中移除对应 hash；保留其他设备的 refresh token。
- 全部设备退出：读取该用户的全部 refresh token hash，批量删除对应 refresh key，最后删除用户反向索引。
- refresh key 自然过期后，允许索引中暂时存在孤立成员；在 `logout-all` 或定时清理时移除。

### 一致性要求

- 使用 Redis Lua 脚本或等效原子操作，确保 refresh key 与用户反向索引同步更新。
- 轮换操作并发执行时，旧 refresh token 只能成功消费一次。
- 避免使用 `KEYS` 或扫描全站 refresh token 实现 `logout-all`。

### 验收条件

- 单设备退出不会删除其他设备的 refresh token。
- `logout-all` 后，该用户此前签发的所有 refresh token 均不可再使用。
- 同一个 refresh token 并发刷新时只有一个请求成功。
- 登录、刷新或写入失败不会留下无法被 `logout-all` 找到的有效 refresh token。
- 过期 refresh token 对应的孤立索引成员能够被安全清理。

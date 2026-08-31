# visualSDU 前端接口对照文档

> 总结日期：2026-08-25
> 本文描述的是**当前代码实际行为**，不是规划中的接口。

## 1. 全局约定

### 1.1 基础地址与路径前缀

下文路径均为不包含 `BASE_URL` 的后端原始路径，例如：

```text
GET {BASE_URL}/cities
```

### 1.2 统一响应体

除 SDU Pass 登录成功回调外，Controller 正常响应和已处理异常都使用相同响应体。SDU Pass 登录成功回调返回无响应体的 HTTP `302`，详见 3.1 节。

| 字段 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `code` | number | 否 | 业务状态码；`0` 表示成功 |
| `msg` | string | 否 | 面向调用方的结果说明 |
| `data` | 由接口决定 | 是 | 业务数据；具体结构在每个接口下方单独列出 |
| `timestamp` | number | 否 | 服务器生成响应时的 Unix 毫秒时间戳 |

成功示例：

```json
{
  "code": 0,
  "msg": "成功",
  "data": {},
  "timestamp": 1786773600000
}
```

错误示例：

```json
{
  "code": 10105,
  "msg": "access token 已过期",
  "data": null,
  "timestamp": 1786773600000
}
```

注意：

- 判断业务成功应使用 `code === 0`，同时仍应先检查 HTTP 状态。
- 除 SDU Pass 登录成功回调返回 HTTP `302` 外，其他成功接口当前均返回 HTTP `200`；创建接口不是 `201`，无数据接口也不是 `204`。
- `msg` 可能是枚举默认文案，也可能被具体业务文案覆盖，不建议用文案判断错误类型。
- JSON 字段校验失败时，`data` 是 `{ [field: string]: string }`；其他错误通常为 `null`。

字段校验失败示例：

```json
{
  "code": 400,
  "msg": "请求体字段格式错误",
  "data": {
    "refreshToken": "refresh token不能为空"
  },
  "timestamp": 1786773600000
}
```

### 1.3 鉴权

访问令牌使用自定义请求头，**不是**标准的 `Authorization: Bearer ...`：

```http
token: <accessToken>
```

本文使用三种权限标记：

| 标记 | 实际含义 |
|---|---|
| 公开 | 不带 token 可访问；若主动携带了无效或过期 token，仍会被鉴权过滤器拒绝 |
| 登录 | 必须携带有效 access token |
| 管理员 | 必须携带有效 access token，且用户角色为 `ADMIN` |

令牌时效和行为：

- access token 有效期：`3600` 秒（1 小时）。
- refresh token 有效期：`604800` 秒（7 天）。
- refresh token 刷新成功后会立即作废旧 refresh token，并同时返回新的 access token 和 refresh token。
- 前端刷新成功后必须同时覆盖本地的两个 token；并发刷新应做单飞/互斥，避免旧 refresh token 被重复使用。
- 公开接口可携带有效 token，以返回用户维度的 `liked`、`favorited` 等状态。

### 1.4 请求格式、时间和 URL

- 普通请求体使用 `application/json`。
- 投稿创建、修改和头像上传使用 `multipart/form-data`；使用浏览器 `FormData` 时不要手动设置带 boundary 的 `Content-Type`。
- `LocalDateTime` 使用不带时区的 ISO 8601 字符串，例如 `2026-08-15T14:30:00`。
- ID、计数和经纬度在 JSON 中均为数字。
- 投稿文件使用同名多值字段，例如多次 `formData.append("files", file)`；创建投稿的标签 ID 使用 `tagIds` 同名多值字段，修改投稿的标签名称仍使用 `tags` 同名多值字段。
- 媒体图片、缩略图、投稿预览、用户头像、城市与地点封面和旧版下载地址由 R2 ObjectKey 生成预签名 URL，有效期为 10 分钟。不要长期缓存 URL；需要时重新请求对应详情。流式下载接口返回同源文件流，不返回 JSON。
- 城市和地点在数据库中只保存 `coverKey`；公开接口返回由 `FileStorage#getUrl` 生成的 `coverUrl`。校区当前仍直接保存并返回 URL。

## 2. 接口总览

至今开发完成的共有 96 个实际映射的接口。

| 模块 | 方法 | 路径 | 权限 | 请求格式 | `data` 类型 |
|---|---|---|---|---|---|
| 认证 | GET | `/auth/sdupass/callback` | 公开 | Query | HTTP 302，无响应体 |
| 认证 | POST | `/auth/login` | 公开 | JSON | 凭证对象 |
| 认证 | POST | `/auth/refresh` | 公开 | JSON | 凭证对象 |
| 认证 | DELETE | `/auth/logout` | 登录 | JSON | `null` |
| 个人中心 | GET | `/users/me` | 登录 | - | 个人资料对象 |
| 个人中心 | PATCH | `/users/me` | 登录 | JSON | 个人资料对象 |
| 个人中心 | PUT | `/users/me/avatar` | 登录 | Multipart | 个人资料对象 |
| 个人中心 | DELETE | `/users/me/avatar` | 登录 | - | 个人资料对象 |
| 个人中心 | PUT | `/users/me/password` | 登录 | JSON | `null` |
| 个人中心 | GET | `/users/me/history` | 登录 | Query | 浏览足迹分页对象 |
| 个人中心 | DELETE | `/users/me/history` | 登录 | - | `null` |
| 个人中心 | DELETE | `/users/me/history/{mediaId}` | 登录 | Path | `null` |
| 城市 | GET | `/cities` | 公开 | - | 城市对象数组 |
| 地点分类 | GET | `/location-categories` | 公开 | - | 地点分类选项数组 |
| 城市 | GET | `/cities/{cityId}/campuses` | 公开 | Path | 校区摘要对象数组 |
| 校区 | GET | `/campuses/{campusId}` | 公开 | Path | 校区详情对象 |
| 校区 | GET | `/campuses/{campusId}/locations` | 公开 | Query | 地点摘要对象数组 |
| 地点 | GET | `/locations/{locationId}` | 公开 | Path | 地点详情对象 |
| 地点 | GET | `/locations/{locationId}/media` | 公开 | Query | 媒体摘要分页对象 |
| 地点 | POST | `/locations/{locationId}/favorites` | 登录 | Path | 地点收藏状态对象 |
| 地点 | DELETE | `/locations/{locationId}/favorites` | 登录 | Path | 地点收藏状态对象 |
| 地图 | GET | `/map/markers` | 公开 | Query | 点位对象数组 |
| 媒体 | GET | `/media/{mediaId}` | 公开 | Path | 媒体详情对象 |
| 媒体 | POST | `/media/{mediaId}/views` | 公开 | Path | 媒体互动状态对象 |
| 媒体 | POST | `/media/{mediaId}/likes` | 登录 | Path | 媒体互动状态对象 |
| 媒体 | DELETE | `/media/{mediaId}/likes` | 登录 | Path | 媒体互动状态对象 |
| 媒体 | POST | `/media/{mediaId}/favorites` | 登录 | 可选 JSON | 媒体互动状态对象 |
| 媒体 | DELETE | `/media/{mediaId}/favorites` | 登录 | Path | 媒体互动状态对象 |
| 媒体 | POST | `/media/{mediaId}/downloads` | 登录 | Path | 下载信息对象 |
| 媒体 | POST | `/media/{mediaId}/downloads/ticket` | 登录 | Path | 下载票据字符串 |
| 媒体 | GET | `/media/downloads/stream` | 公开 | Query | 图片文件二进制流 |
| 媒体 | GET | `/media/{mediaId}/related` | 公开 | Query | 媒体摘要对象数组 |
| 举报 | GET | `/reports/reasons` | 公开 | - | 举报理由对象数组 |
| 举报 | POST | `/reports/media` | 登录 | JSON | 媒体举报提交结果对象 |
| 举报 | POST | `/reports/user` | 登录 | JSON | 用户举报提交结果对象 |
| 搜索 | GET | `/search/suggestions` | 公开 | Query | 搜索建议对象数组 |
| 搜索 | GET | `/search/media` | 公开 | Query | 媒体摘要分页对象 |
| 发现 | GET | `/discovery/home` | 公开 | Query | 发现首页聚合对象 |
| 话题 | GET | `/topics` | 公开 | - | 话题摘要对象数组 |
| 话题 | GET | `/topics/{topicId}` | 公开 | Path | 话题详情对象 |
| 话题 | GET | `/topics/{topicId}/media` | 公开 | Query | 媒体摘要分页对象 |
| 话题 | POST | `/topics/{topicId}/favorites` | 登录 | Path | 话题收藏状态对象 |
| 话题 | DELETE | `/topics/{topicId}/favorites` | 登录 | Path | 话题收藏状态对象 |
| 收藏夹 | GET | `/favorite-folders` | 登录 | - | 收藏夹对象数组 |
| 收藏夹 | POST | `/favorite-folders` | 登录 | JSON | 收藏夹对象 |
| 收藏夹 | PATCH | `/favorite-folders/{folderId}` | 登录 | JSON | 收藏夹对象 |
| 收藏夹 | DELETE | `/favorite-folders/{folderId}` | 登录 | Path | `null` |
| 收藏夹 | GET | `/favorite-folders/{folderId}/items` | 登录 | Query | 媒体摘要分页对象 |
| 收藏 | POST | `/favorites/batch` | 登录 | JSON | 批量收藏结果对象 |
| 收藏 | GET | `/favorites/locations` | 登录 | Query | 收藏地点分页对象 |
| 收藏 | GET | `/favorites/topics` | 登录 | Query | 收藏话题分页对象 |
| 时光对比 | GET | `/time-comparisons` | 登录 | Query | 时光对比摘要对象数组 |
| 时光对比 | GET | `/time-comparisons/{comparisonId}` | 登录 | Path | 时光对比详情对象 |
| 投稿 | POST | `/submissions` | 登录 | Multipart | 投稿详情对象 |
| 投稿 | GET | `/submissions/mine` | 登录 | Query | 投稿摘要分页对象 |
| 投稿 | GET | `/submissions/{submissionId}` | 登录 | Path | 投稿详情对象 |
| 投稿 | PUT | `/submissions/{submissionId}` | 登录 | Multipart | 投稿详情对象 |
| 投稿 | POST | `/submissions/{submissionId}/resubmit` | 登录 | Path | 投稿详情对象 |
| 投稿 | POST | `/submissions/{submissionId}/withdraw` | 登录 | Path | `null` |
| 管理 | PUT | `/admin/settings/submission-review` | 管理员 | JSON | 审核设置对象 |
| 管理审核 | GET | `/admin/submissions` | 管理员 | Query | 管理稿件摘要分页对象 |
| 管理审核 | GET | `/admin/submissions/{submissionId}` | 管理员 | Path | 管理稿件详情对象 |
| 管理审核 | POST | `/admin/submissions/{submissionId}/review` | 管理员 | JSON | 审核结果对象 |
| 管理审核 | POST | `/admin/submissions/reviews/batch` | 管理员 | JSON | 批量审核结果对象 |
| 管理审核 | GET | `/admin/submissions/{submissionId}/review-logs` | 管理员 | Query | 审核记录分页对象 |
| 管理用户 | GET | `/admin/users` | 管理员 | Query | 管理用户分页对象 |
| 管理用户 | GET | `/admin/users/{userId}` | 管理员 | Path | 管理用户对象 |
| 管理用户 | PATCH | `/admin/users/{userId}/role` | 管理员 | JSON | 管理用户对象 |
| 管理用户 | PATCH | `/admin/users/{userId}/status` | 管理员 | JSON | 管理用户对象 |
| 管理用户 | PATCH | `/admin/users/{userId}/permissions` | 管理员 | JSON | 管理用户对象 |
| 管理地点 | POST | `/admin/locations` | 管理员 | JSON | 管理地点对象 |
| 管理地点 | PATCH | `/admin/locations/{locationId}` | 管理员 | JSON | 管理地点对象 |
| 管理媒体 | GET | `/admin/media` | 管理员 | Query | 管理媒体分页对象 |
| 管理媒体 | PATCH | `/admin/media/{mediaId}/classification` | 管理员 | JSON | 管理媒体对象 |
| 管理媒体 | POST | `/admin/media/{mediaId}/hide` | 管理员 | Path | 管理媒体对象 |
| 管理媒体 | POST | `/admin/media/{mediaId}/restore` | 管理员 | Path | 管理媒体对象 |
| 管理媒体 | DELETE | `/admin/media/{mediaId}` | 管理员 | Path | `null` |
| 管理举报 | GET | `/admin/reports` | 管理员 | Query | 管理举报摘要分页对象 |
| 管理举报 | GET | `/admin/reports/{reportId}` | 管理员 | Path | 管理举报详情对象 |
| 管理举报 | POST | `/admin/reports/{reportId}/decision` | 管理员 | JSON | 举报处理结果对象 |
| 管理标签 | GET | `/admin/tags` | 管理员 | Query | 管理标签对象数组 |
| 管理标签 | POST | `/admin/tags` | 管理员 | JSON | 管理标签对象 |
| 管理标签 | PATCH | `/admin/tags/{tagId}` | 管理员 | JSON | 管理标签对象 |
| 管理标签 | POST | `/admin/tags/{tagId}/merge` | 管理员 | JSON | `null` |
| 管理专题 | POST | `/admin/topics` | 管理员 | JSON | 管理专题对象 |
| 管理专题 | PATCH | `/admin/topics/{topicId}` | 管理员 | JSON | 管理专题对象 |
| 管理时光对比 | POST | `/admin/time-comparisons` | 管理员 | JSON | 管理时光对比对象 |
| 公告 | GET | `/announcements` | 公开 | Query | 公告摘要分页对象 |
| 公告 | GET | `/announcements/{announcementId}` | 公开 | Path | 公告详情对象 |
| 管理公告 | GET | `/admin/announcements` | 管理员 | Query | 管理公告分页对象 |
| 管理公告 | POST | `/admin/announcements` | 管理员 | JSON | 管理公告对象 |
| 管理公告 | PATCH | `/admin/announcements/{announcementId}` | 管理员 | JSON | 管理公告对象 |
| 管理公告 | POST | `/admin/announcements/{announcementId}/status` | 管理员 | JSON | 管理公告对象 |
| 健康检查 | GET | `/ping/public` | 公开 | - | `null` |
| 健康检查 | GET | `/ping/auth` | 登录 | - | `null` |
| 健康检查 | GET | `/ping/admin` | 管理员 | - | `null` |

## 3. 认证接口

### 3.1 SDU Pass 登录回调

```http
GET /auth/sdupass/callback?code=<callbackCode>
```

权限：公开。

Query 参数：

| 参数 | 类型 | 必填 | 说明 |
|---|---:|---:|---|
| `code` | string | 是 | SDU Pass 登录完成后回调携带的临时凭证 |

已有用户完成统一认证校验，首次访问的统一认证用户会自动创建账号。校验成功后，后端生成一个有效期为 `120` 秒的一次性 `loginTicket`，返回 HTTP `302`，并将票据作为前端登录完成页的 Query 参数。该接口不直接签发 access token 和 refresh token。

成功响应示例：

```http
HTTP/1.1 302 Found
Location: http://localhost:5173/auth/return?loginTicket=<loginTicket>
Cache-Control: no-store
```

浏览器会自动跟随 `Location` 跳转。重定向基础地址来自后端配置 `vsdu.auth.frontend-return-url`；当前默认值为 `http://localhost:5173/auth/return`。

`loginTicket` 是短期、一次性凭据。前端取得后应立即调用 3.2 节的 `/auth/login` 接口兑换正式凭证，不应把它作为 access token 使用或长期保存。

主要错误：

- `10101`：SDU Pass 换取 token 失败。
- `10104`：账号被冻结或停用。
- `500`：用户创建、登录票据存储等内部过程失败。

### 3.2 一次性登录票据兑换

```http
POST /auth/login
Content-Type: application/json
```

权限：公开；不需要 access token。

请求体：

```json
{
  "loginTicket": "从登录回调取得的一次性票据"
}
```

后端会原子地读取并删除 `loginTicket`。同一票据只能成功兑换一次；无论票据已经使用还是已经过期，再次提交都按无效票据处理。票据校验成功后，后端根据对应用户的当前角色和 token 版本签发 access token 与 refresh token。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.accessToken` | string | 否 | 后续请求放入 `token` 请求头的访问令牌 |
| `data.refreshToken` | string | 否 | 用于刷新凭证的令牌 |

JSON 响应示例：

```json
{
  "code": 0,
  "msg": "成功",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "Yf8V...Q2s"
  },
  "timestamp": 1786773600000
}
```

主要错误：

- `400`：请求体缺失、不可解析或 `loginTicket` 为空。
- `10104`：账号被冻结或停用。
- `10300`：loginTicket 无效、已过期或已被使用。
- `500`：refresh token 存储等内部过程失败；loginTicket 已被消费，前端应重新发起统一认证。

### 3.3 刷新令牌

```http
POST /auth/refresh
Content-Type: application/json
```

权限：公开；不需要 access token。

请求体：

```json
{
  "refreshToken": "旧 refresh token"
}
```

刷新采用轮换语义，成功后旧 refresh token 不可再次使用。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.accessToken` | string | 否 | 新的 access token |
| `data.refreshToken` | string | 否 | 新的 refresh token；必须替换本地旧值 |

JSON 响应示例：

```json
{
  "code": 0,
  "msg": "成功",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...new",
    "refreshToken": "R4nD...new"
  },
  "timestamp": 1786773600000
}
```

主要错误：

- `400`：请求体缺失、不可解析或 `refreshToken` 为空。
- `10104`：账号被冻结或停用。
- `10200`：refresh token 无效或已过期。
- `10201`：refresh token 原子轮换失败。

### 3.4 退出登录

```http
DELETE /auth/logout
token: <accessToken>
Content-Type: application/json
```

权限：登录。

请求体：

```json
{
  "refreshToken": "当前 refresh token"
}
```

当提交的 refresh token 确实属于当前 access token 用户时，后端删除该 refresh token，并提升用户 token 版本，使旧 access token 失效。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 说明 |
|---|---|---|
| `data` | null | 该接口不返回业务数据 |

JSON 响应示例：

```json
{
  "code": 0,
  "msg": "成功",
  "data": null,
  "timestamp": 1786773600000
}
```

当前接口具有幂等倾向：refresh token 不存在或不属于当前用户时仍返回成功，但不会提升 token 版本。前端收到成功响应后应无条件清理本地凭证。

> `/auth/register` 没有路由映射，当前不能由前端直接调用。

## 4. 城市、校区、地点与地图

### 4.1 地点分类列表

```http
GET /location-categories
```

权限：公开。无参数，不分页。只返回启用分类，按后台 `sortOrder`、`id` 升序排列；无数据时返回 `[]`。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data` | array&lt;object&gt; | 否 | 地点分类选项数组 |
| `data[].code` | string | 否 | 提交给地点创建、修改接口的稳定分类编码 |
| `data[].name` | string | 否 | 分类展示名称 |

```json
{
  "code": 0,
  "msg": "查询地点分类列表成功",
  "data": [
    {"code": "BUILDING", "name": "教学及办公建筑"},
    {"code": "LIBRARY", "name": "图书馆"}
  ],
  "timestamp": 1786773600000
}
```

### 4.2 城市列表

```http
GET /cities
```

权限：公开。无参数。只返回启用城市，按后台 `sortOrder`、`id` 升序排列。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data` | array&lt;object&gt; | 否 | 城市数组；没有数据时为 `[]` |
| `data[].id` | number | 否 | 城市 ID |
| `data[].name` | string | 否 | 城市名称 |
| `data[].code` | string | 否 | 城市代码 |
| `data[].province` | string | 否 | 所属省份 |
| `data[].coverUrl` | string | 是 | 由城市 `coverKey` 生成的 10 分钟预签名封面 URL |
| `data[].description` | string | 是 | 城市描述 |
| `data[].campusCount` | number | 否 | 启用校区数量 |

JSON 响应示例：

```json
{
  "code": 0,
  "msg": "查询城市列表成功",
  "data": [
    {
      "id": 1,
      "name": "济南",
      "code": "JINAN",
      "province": "山东省",
      "coverUrl": "https://r2.example.com/cities/jinan.jpg?X-Amz-Signature=...",
      "description": "山东大学主要校区所在城市",
      "campusCount": 4
    }
  ],
  "timestamp": 1786773600000
}
```

### 4.3 城市下的校区

```http
GET /cities/{cityId}/campuses
```

权限：公开。

| 参数 | 位置 | 类型 | 必填 | 说明 |
|---|---|---:|---:|---|
| `cityId` | Path | number | 是 | 正数城市 ID |

只返回启用校区，按后台 `sortOrder`、`id` 升序排列。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data` | array&lt;object&gt; | 否 | 校区数组；没有数据时为 `[]` |
| `data[].id` | number | 否 | 校区 ID |
| `data[].cityId` | number | 否 | 所属城市 ID |
| `data[].name` | string | 否 | 校区名称 |
| `data[].shortName` | string | 是 | 校区简称 |
| `data[].address` | string | 是 | 校区地址 |
| `data[].longitude` | number | 是 | 经度 |
| `data[].latitude` | number | 是 | 纬度 |
| `data[].coverUrl` | string | 是 | 校区封面地址 |

JSON 响应示例：

```json
{
  "code": 0,
  "msg": "查询城市下校区列表成功",
  "data": [
    {
      "id": 1,
      "cityId": 1,
      "name": "中心校区",
      "shortName": "中心",
      "address": "山东省济南市山大南路27号",
      "longitude": 117.0612,
      "latitude": 36.6751,
      "coverUrl": "https://example.com/campuses/central.jpg"
    }
  ],
  "timestamp": 1786773600000
}
```

主要错误：`12000` 城市不存在或已停用。

### 4.4 校区详情

```http
GET /campuses/{campusId}
```

权限：公开。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.id` | number | 否 | 校区 ID |
| `data.cityId` | number | 否 | 所属城市 ID |
| `data.cityName` | string | 否 | 所属城市名称 |
| `data.name` | string | 否 | 校区名称 |
| `data.shortName` | string | 是 | 校区简称 |
| `data.address` | string | 是 | 校区地址 |
| `data.longitude` | number | 是 | 经度 |
| `data.latitude` | number | 是 | 纬度 |
| `data.coverUrl` | string | 是 | 校区封面地址 |
| `data.description` | string | 是 | 校区描述 |
| `data.locationCount` | number | 否 | 校区下启用地点数量 |

JSON 响应示例：

```json
{
  "code": 0,
  "msg": "查询校区详情成功",
  "data": {
    "id": 1,
    "cityId": 1,
    "cityName": "济南",
    "name": "中心校区",
    "shortName": "中心",
    "address": "山东省济南市山大南路27号",
    "longitude": 117.0612,
    "latitude": 36.6751,
    "coverUrl": "https://example.com/campuses/central.jpg",
    "description": "山东大学中心校区",
    "locationCount": 18
  },
  "timestamp": 1786773600000
}
```

主要错误：

- `12100`：校区不存在或已停用。
- `12000`：校区所属城市不存在或已停用。

### 4.5 校区下的地点

```http
GET /campuses/{campusId}/locations?categoryCode=<code>
```

权限：公开。

| 参数 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---:|---:|---|---|
| `campusId` | Path | number | 是 | - | 正数校区 ID |
| `categoryCode` | Query | string | 否 | - | 非空时按分类代码精确匹配；空字符串按未传处理 |

按后台 `sortOrder`、`id` 升序排列。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data` | array&lt;object&gt; | 否 | 地点数组；没有数据时为 `[]` |
| `data[].id` | number | 否 | 地点 ID |
| `data[].campusId` | number | 否 | 所属校区 ID |
| `data[].name` | string | 否 | 地点名称 |
| `data[].categoryCode` | string | 否 | 地点分类代码 |
| `data[].address` | string | 是 | 地点地址 |
| `data[].longitude` | number | 是 | 经度 |
| `data[].latitude` | number | 是 | 纬度 |
| `data[].coverUrl` | string | 是 | 由地点 `coverKey` 生成的 10 分钟预签名封面 URL |

JSON 响应示例：

```json
{
  "code": 0,
  "msg": "查询校区地点列表成功",
  "data": [
    {
      "id": 101,
      "campusId": 1,
      "name": "知新楼",
      "categoryCode": "BUILDING",
      "address": "中心校区内",
      "longitude": 117.0618,
      "latitude": 36.6745,
      "coverUrl": "https://r2.example.com/locations/zhixin.jpg?X-Amz-Signature=..."
    }
  ],
  "timestamp": 1786773600000
}
```

主要错误：`12100` 校区不存在或已停用。

### 4.6 地点详情

```http
GET /locations/{locationId}
```

权限：公开。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.id` | number | 否 | 地点 ID |
| `data.campusId` | number | 否 | 所属校区 ID |
| `data.campusName` | string | 否 | 所属校区名称 |
| `data.cityId` | number | 否 | 所属城市 ID |
| `data.cityName` | string | 否 | 所属城市名称 |
| `data.name` | string | 否 | 地点名称 |
| `data.categoryCode` | string | 否 | 地点分类代码 |
| `data.address` | string | 是 | 地点地址 |
| `data.longitude` | number | 是 | 经度 |
| `data.latitude` | number | 是 | 纬度 |
| `data.coverUrl` | string | 是 | 由地点 `coverKey` 生成的 10 分钟预签名封面 URL |
| `data.description` | string | 是 | 地点描述 |
| `data.favoriteCount` | number | 否 | 地点收藏数 |
| `data.favorited` | boolean | 否 | 当前正式登录用户是否已收藏；未登录或非正式账号为 `false` |

JSON 响应示例：

```json
{
  "code": 0,
  "msg": "查询地点详情成功",
  "data": {
    "id": 101,
    "campusId": 1,
    "campusName": "中心校区",
    "cityId": 1,
    "cityName": "济南",
    "name": "知新楼",
    "categoryCode": "BUILDING",
    "address": "中心校区内",
    "longitude": 117.0618,
    "latitude": 36.6745,
    "coverUrl": "https://r2.example.com/locations/zhixin.jpg?X-Amz-Signature=...",
    "description": "教学与科研建筑",
    "favoriteCount": 12,
    "favorited": true
  },
  "timestamp": 1786773600000
}
```

主要错误：

- `12200`：地点不存在或已停用。
- `12100`：所属校区不存在或已停用。
- `12000`：所属城市不存在或已停用。

### 4.7 地点媒体列表

```http
GET /locations/{locationId}/media?page=1&size=20
```

权限：公开。地点、所属校区和城市都必须启用；只返回 `status = 1` 的媒体，按拍摄时间、创建时间、ID 降序排列。

| 参数 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---:|---:|---:|---|
| `locationId` | Path | number | 是 | - | 正数地点 ID |
| `page` | Query | number | 否 | `1` | 正整数 |
| `size` | Query | number | 否 | `20` | 正整数；大于 50 时按 50 返回 |

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.total`、`data.page`、`data.size` | number | 否 | 总数、规范化页码和每页数量 |
| `data.items` | array&lt;object&gt; | 否 | 当前页媒体摘要；越界或无数据时为 `[]` |
| `data.items[].id`、`locationId` | number | 否 | 媒体 ID、地点 ID |
| `data.items[].title`、`locationName` | string | 是 | 标题、地点名称 |
| `data.items[].thumbnailUrl` | string | 否 | 缩略图预签名 URL |
| `data.items[].shotAt` | string | 是 | 拍摄时间 |
| `data.items[].viewCount`、`likeCount`、`favoriteCount` | number | 否 | 浏览、点赞、收藏数 |

JSON 响应示例：

```json
{
  "code": 0,
  "msg": "查询地点媒体成功",
  "data": {
    "total": 12,
    "page": 1,
    "size": 20,
    "items": [
      {
        "id": 501,
        "title": "知新楼晚霞",
        "locationId": 101,
        "locationName": "知新楼",
        "thumbnailUrl": "https://r2.example.com/thumb-501.jpg?X-Amz-Signature=...",
        "shotAt": "2026-08-15T18:30:00",
        "viewCount": 121,
        "likeCount": 12,
        "favoriteCount": 7
      }
    ]
  },
  "timestamp": 1787392800000
}
```

主要错误：`12200` 地点不存在或停用、`12100` 校区不存在或停用、`12000` 城市不存在或停用，以及通用路径/分页参数错误 `400`。

### 4.8 地图点位

```http
GET /map/markers?cityId=1
GET /map/markers?campusId=1
```

权限：公开。

`cityId` 和 `campusId` 必须且只能传一个：

- 传 `cityId`：返回该城市下的校区点位，`markerType === "CAMPUS"`。
- 传 `campusId`：返回该校区下的地点点位，`markerType === "LOCATION"`。
- 两个都传或两个都不传：返回 `12300`。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data` | array&lt;object&gt; | 否 | 地图点位数组；没有数据时为 `[]` |
| `data[].id` | number | 否 | 校区或地点 ID |
| `data[].markerType` | string | 否 | `CAMPUS` 或 `LOCATION` |
| `data[].campusId` | number | 否 | 校区点位时等于自身 ID；地点点位时为所属校区 ID |
| `data[].name` | string | 否 | 点位名称 |
| `data[].longitude` | number | 是 | 经度 |
| `data[].latitude` | number | 是 | 纬度 |
| `data[].coverUrl` | string | 是 | 校区点位为数据库 URL；地点点位为根据 `coverKey` 生成的 10 分钟预签名 URL |

JSON 响应示例（按城市查询校区点位）：

```json
{
  "code": 0,
  "msg": "查询地图点位成功",
  "data": [
    {
      "id": 1,
      "markerType": "CAMPUS",
      "campusId": 1,
      "name": "中心校区",
      "longitude": 117.0612,
      "latitude": 36.6751,
      "coverUrl": "https://example.com/campuses/central.jpg"
    }
  ],
  "timestamp": 1786773600000
}
```

主要错误：`12000`、`12100`、`12300`。

## 5. 媒体浏览与互动

除公开浏览外，点赞、收藏、下载还会在服务层检查用户是否为正常、未删除且具有统一认证身份的正式用户。

### 5.1 媒体详情

```http
GET /media/{mediaId}
token: <可选 accessToken>
```

权限：公开。

- 不带 token 时 `liked`、`favorited` 均为 `false`。
- 带有效正式用户 token 时返回该用户的点赞/收藏状态。
- 媒体必须处于可见状态，否则返回 `13000`。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.id` | number | 否 | 媒体 ID |
| `data.uploaderId` | number | 是 | 上传者用户 ID |
| `data.uploaderNickname` | string | 是 | 上传者昵称 |
| `data.locationId` | number | 否 | 拍摄地点 ID |
| `data.locationName` | string | 是 | 拍摄地点名称 |
| `data.title` | string | 是 | 媒体标题 |
| `data.description` | string | 是 | 媒体描述 |
| `data.imageUrl` | string | 否 | 原图预签名 URL，约 10 分钟有效 |
| `data.thumbnailUrl` | string | 否 | 缩略图预签名 URL，约 10 分钟有效 |
| `data.shotAt` | string | 是 | 拍摄时间，ISO LocalDateTime |
| `data.tags` | array&lt;string&gt; | 否 | 标签数组；无标签时为 `[]` |
| `data.viewCount` | number | 否 | 浏览次数 |
| `data.likeCount` | number | 否 | 点赞数 |
| `data.favoriteCount` | number | 否 | 收藏数 |
| `data.downloadCount` | number | 否 | 下载次数 |
| `data.liked` | boolean | 否 | 当前正式登录用户是否已点赞 |
| `data.favorited` | boolean | 否 | 当前正式登录用户是否已收藏 |
| `data.createdAt` | string | 是 | 创建时间，ISO LocalDateTime |

JSON 响应示例：

```json
{
  "code": 0,
  "msg": "成功",
  "data": {
    "id": 501,
    "uploaderId": 10001,
    "uploaderNickname": "山大摄影者",
    "locationId": 101,
    "locationName": "知新楼",
    "title": "知新楼晚霞",
    "description": "夏日晚霞下的知新楼",
    "imageUrl": "https://r2.example.com/image.jpg?X-Amz-Signature=...",
    "thumbnailUrl": "https://r2.example.com/thumb.jpg?X-Amz-Signature=...",
    "shotAt": "2026-08-15T18:30:00",
    "tags": ["晚霞", "建筑"],
    "viewCount": 121,
    "likeCount": 12,
    "favoriteCount": 7,
    "downloadCount": 3,
    "liked": true,
    "favorited": false,
    "createdAt": "2026-08-15T19:00:00"
  },
  "timestamp": 1786773600000
}
```

### 5.2 记录一次浏览

```http
POST /media/{mediaId}/views
token: <可选 accessToken>
```

权限：公开。无请求体。

- 每次调用都会增加媒体总浏览量。
- 匿名访问只增加总量；正式登录用户还会新增或更新个人浏览足迹。
- 前端应在确定要计入一次浏览时调用，避免组件重复挂载造成重复计数。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.mediaId` | number | 否 | 媒体 ID |
| `data.viewCount` | number | 否 | 增加后的浏览次数 |
| `data.likeCount` | number | 否 | 当前点赞数 |
| `data.favoriteCount` | number | 否 | 当前收藏数 |
| `data.liked` | boolean | 否 | 当前正式登录用户是否已点赞 |
| `data.favorited` | boolean | 否 | 当前正式登录用户是否已收藏 |

JSON 响应示例：

```json
{
  "code": 0,
  "msg": "成功",
  "data": {
    "mediaId": 501,
    "viewCount": 122,
    "likeCount": 12,
    "favoriteCount": 7,
    "liked": false,
    "favorited": false
  },
  "timestamp": 1786773600000
}
```

### 5.3 点赞

```http
POST /media/{mediaId}/likes
token: <accessToken>
```

权限：登录，且服务层要求正式正常用户。无请求体。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.mediaId` | number | 否 | 媒体 ID |
| `data.viewCount` | number | 否 | 当前浏览次数 |
| `data.likeCount` | number | 否 | 点赞后的点赞数 |
| `data.favoriteCount` | number | 否 | 当前收藏数 |
| `data.liked` | boolean | 否 | 点赞成功后为 `true` |
| `data.favorited` | boolean | 否 | 当前用户是否已收藏 |

JSON 响应示例：

```json
{
  "code": 0,
  "msg": "点赞成功",
  "data": {
    "mediaId": 501,
    "viewCount": 122,
    "likeCount": 13,
    "favoriteCount": 7,
    "liked": true,
    "favorited": false
  },
  "timestamp": 1786773600000
}
```

主要错误：

- `13000`：媒体不存在或不可见。
- `13001`：已经点赞过该媒体。
- `14000`：不是统一认证正式用户。

### 5.4 取消点赞

```http
DELETE /media/{mediaId}/likes
token: <accessToken>
```

权限：登录，且服务层要求正式正常用户。无请求体。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.mediaId` | number | 否 | 媒体 ID |
| `data.viewCount` | number | 否 | 当前浏览次数 |
| `data.likeCount` | number | 否 | 取消后的点赞数 |
| `data.favoriteCount` | number | 否 | 当前收藏数 |
| `data.liked` | boolean | 否 | 取消成功后为 `false` |
| `data.favorited` | boolean | 否 | 当前用户是否已收藏 |

JSON 响应示例：

```json
{
  "code": 0,
  "msg": "已取消点赞",
  "data": {
    "mediaId": 501,
    "viewCount": 122,
    "likeCount": 12,
    "favoriteCount": 7,
    "liked": false,
    "favorited": false
  },
  "timestamp": 1786773600000
}
```

主要错误：`13000`、`13002`、`14000`。

### 5.5 收藏

```http
POST /media/{mediaId}/favorites
token: <accessToken>
Content-Type: application/json
```

权限：登录，且服务层要求正式正常用户。

请求体整体可省略，也可传：

```json
{
  "folderId": 123
}
```

- 不传请求体、传 `{}` 或 `folderId: null`：使用默认收藏夹；默认收藏夹不存在时自动创建。
- 指定 `folderId`：必须是当前用户拥有的收藏夹，且为正数。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.mediaId` | number | 否 | 媒体 ID |
| `data.viewCount` | number | 否 | 当前浏览次数 |
| `data.likeCount` | number | 否 | 当前点赞数 |
| `data.favoriteCount` | number | 否 | 收藏后的收藏数 |
| `data.liked` | boolean | 否 | 当前用户是否已点赞 |
| `data.favorited` | boolean | 否 | 收藏成功后为 `true` |

JSON 响应示例：

```json
{
  "code": 0,
  "msg": "收藏成功",
  "data": {
    "mediaId": 501,
    "viewCount": 122,
    "likeCount": 12,
    "favoriteCount": 8,
    "liked": false,
    "favorited": true
  },
  "timestamp": 1786773600000
}
```

主要错误：`13000`、`13100`、`13101`、`14000`，以及字段校验错误 `400`。

### 5.6 取消收藏

```http
DELETE /media/{mediaId}/favorites
token: <accessToken>
```

权限：登录，且服务层要求正式正常用户。接口不接收 `folderId`，会删除当前用户对该媒体的所有收藏关系。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.mediaId` | number | 否 | 媒体 ID |
| `data.viewCount` | number | 否 | 当前浏览次数 |
| `data.likeCount` | number | 否 | 当前点赞数 |
| `data.favoriteCount` | number | 否 | 取消后的收藏数 |
| `data.liked` | boolean | 否 | 当前用户是否已点赞 |
| `data.favorited` | boolean | 否 | 取消成功后为 `false` |

JSON 响应示例：

```json
{
  "code": 0,
  "msg": "已取消收藏",
  "data": {
    "mediaId": 501,
    "viewCount": 122,
    "likeCount": 12,
    "favoriteCount": 7,
    "liked": false,
    "favorited": false
  },
  "timestamp": 1786773600000
}
```

主要错误：`13000`、`13102`、`14000`。

### 5.7 请求原图下载

```http
POST /media/{mediaId}/downloads
token: <accessToken>
```

权限：登录，且服务层要求正式正常用户和 `allowDownload === true`。无请求体。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.mediaId` | number | 否 | 媒体 ID |
| `data.downloadUrl` | string | 否 | 原图预签名下载 URL |
| `data.expiresInSeconds` | number | 否 | URL 剩余有效期秒数，当前固定为 `600` |

JSON 响应示例：

```json
{
  "code": 0,
  "msg": "下载地址已生成",
  "data": {
    "mediaId": 501,
    "downloadUrl": "https://r2.example.com/image.jpg?X-Amz-Signature=...",
    "expiresInSeconds": 600
  },
  "timestamp": 1786773600000
}
```

成功调用会写下载记录并增加下载次数。主要错误：`13000`、`13200`、`14000`。

### 5.8 获取原图下载票据

```http
POST /media/{mediaId}/downloads/ticket
token: <accessToken>
```

权限：登录，且服务层要求正式正常用户和 `allowDownload === true`。无请求体。

该接口用于同源流式下载前置取票。后端会校验媒体存在且可见，生成一个短期、一次性下载票据，并将票据摘要作为 Redis key、将 `userId`、`mediaId`、`objectKey` 等下载上下文保存为 JSON。当前票据有效期由 `vsdu.media.ticket-expire-seconds` 配置，默认 `300` 秒。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data` | string | 否 | 下载票据原文；前端只应短期持有，立即用于 5.9 节接口 |

JSON 响应示例：

```json
{
  "code": 0,
  "msg": "成功",
  "data": "gA0lYtD2s0VQFJ1lPV9_CkP35cW2Gu0R",
  "timestamp": 1786773600000
}
```

取票成功本身不写下载记录，也不增加下载次数；实际下载记录在票据被 5.9 节接口成功消费时写入。

主要错误：`13000`、`13200`、`14000`。

### 5.9 流式下载原图文件

```http
GET /media/downloads/stream?ticket=<downloadTicket>
```

权限：公开。该接口不依赖 `token` 请求头，鉴权信息来自一次性下载票据，因此可用于浏览器直接打开 URL 触发下载。

Query 参数：

| 参数 | 类型 | 必填 | 说明 |
|---|---:|---:|---|
| `ticket` | string | 是 | 由 5.8 节接口返回的短期一次性下载票据 |

处理规则：

- 后端使用 Redis `GETDEL` 原子消费票据；票据不存在、过期或已使用时返回业务错误。
- 票据消费后，后端根据票据中的 `userId` 重新校验用户仍为正式正常用户，且仍具有原图下载权限。
- 后端根据票据中的 `mediaId` 查询媒体，要求媒体仍存在且可见。
- 后端校验票据中的 `objectKey` 与当前媒体 `objectKey` 一致，避免票据被挪用或媒体文件变化后继续下载旧对象。
- 校验通过后，后端记录下载审计并增加媒体下载次数，再从 R2 拉取对象流并返回给浏览器。

成功响应不是统一 JSON，而是图片文件二进制流。典型响应头如下：

```http
HTTP/1.1 200 OK
Content-Type: image/jpeg
Content-Length: 2458123
Content-Disposition: attachment; filename*=UTF-8''zhixinlou-501.jpg
Cache-Control: no-cache

<图片二进制内容>
```

响应说明：

| 内容 | 说明 |
|---|---|
| 响应体 | 原图文件二进制流 |
| `Content-Type` | R2 对象元数据中的 MIME 类型，例如 `image/jpeg`、`image/png`、`image/webp` |
| `Content-Length` | R2 对象元数据中的文件大小 |
| `Content-Disposition` | `attachment` 下载响应头，文件名由媒体标题清洗后拼接媒体 ID 和扩展名生成 |

错误响应仍使用项目统一 JSON 响应体。

主要错误：`13291` 下载票据无效或已过期、`13200` 无原图下载权限、`13000` 媒体不存在或不可见、`14000` 用户不是正式正常用户。

前端推荐流程：

```js
const ticketResponse = await request.post(`/media/${mediaId}/downloads/ticket`)
const ticket = ticketResponse.data

const a = document.createElement('a')
a.href = `/media/downloads/stream?ticket=${encodeURIComponent(ticket)}`
a.click()
```

### 5.10 相关媒体

```http
GET /media/{mediaId}/related?size=12
```

权限：公开。

| 参数 | 类型 | 必填 | 默认值 | 实际处理 |
|---|---:|---:|---:|---|
| `size` | number | 否 | `12` | 小于 1 按 1，大于 30 按 30 |

候选为相同地点或命中源媒体首个标签的可见媒体，按点赞数、浏览数降序；不包含源媒体自身。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data` | array&lt;object&gt; | 否 | 相关媒体数组；没有结果时为 `[]` |
| `data[].id` | number | 否 | 媒体 ID |
| `data[].title` | string | 是 | 媒体标题 |
| `data[].locationId` | number | 否 | 地点 ID |
| `data[].locationName` | string | 是 | 地点名称 |
| `data[].thumbnailUrl` | string | 否 | 缩略图预签名 URL |
| `data[].shotAt` | string | 是 | 拍摄时间，ISO LocalDateTime |
| `data[].viewCount` | number | 否 | 浏览次数 |
| `data[].likeCount` | number | 否 | 点赞数 |
| `data[].favoriteCount` | number | 否 | 收藏数 |

JSON 响应示例：

```json
{
  "code": 0,
  "msg": "成功",
  "data": [
    {
      "id": 502,
      "title": "知新楼清晨",
      "locationId": 101,
      "locationName": "知新楼",
      "thumbnailUrl": "https://r2.example.com/thumb-502.jpg?X-Amz-Signature=...",
      "shotAt": "2026-08-10T06:30:00",
      "viewCount": 88,
      "likeCount": 20,
      "favoriteCount": 9
    }
  ],
  "timestamp": 1786773600000
}
```

## 6. 时光对比

### 6.1 时光对比列表

```http
GET /time-comparisons?locationId=1&size=30
token: <accessToken>
```

权限：登录。

| 参数 | 类型 | 必填 | 默认值 | 实际处理 |
|---|---:|---:|---:|---|
| `locationId` | number | 否 | - | 传入时按地点 ID 过滤；当前不额外校验地点是否存在 |
| `size` | number | 否 | `30` | 小于 1 按 1，大于 100 按 100 |

结果按更新时间降序。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data` | array&lt;object&gt; | 否 | 时光对比数组；没有结果时为 `[]` |
| `data[].id` | number | 否 | 时光对比 ID |
| `data[].locationId` | number | 否 | 地点 ID |
| `data[].locationName` | string | 是 | 地点名称 |
| `data[].title` | string | 是 | 标题 |
| `data[].description` | string | 是 | 描述 |
| `data[].media` | array&lt;object&gt; | 否 | 对比包含的可见媒体摘要数组 |
| `data[].media[].id` | number | 否 | 媒体 ID |
| `data[].media[].title` | string | 是 | 媒体标题 |
| `data[].media[].locationId` | number | 否 | 媒体地点 ID |
| `data[].media[].locationName` | string | 是 | 媒体地点名称 |
| `data[].media[].thumbnailUrl` | string | 否 | 缩略图预签名 URL |
| `data[].media[].shotAt` | string | 是 | 拍摄时间，ISO LocalDateTime |
| `data[].media[].viewCount` | number | 否 | 浏览次数 |
| `data[].media[].likeCount` | number | 否 | 点赞数 |
| `data[].media[].favoriteCount` | number | 否 | 收藏数 |

JSON 响应示例：

```json
{
  "code": 0,
  "msg": "成功",
  "data": [
    {
      "id": 301,
      "locationId": 101,
      "locationName": "知新楼",
      "title": "知新楼今昔",
      "description": "不同时期的知新楼影像",
      "media": [
        {
          "id": 501,
          "title": "2010年的知新楼",
          "locationId": 101,
          "locationName": "知新楼",
          "thumbnailUrl": "https://r2.example.com/old-thumb.jpg?X-Amz-Signature=...",
          "shotAt": "2010-09-01T10:00:00",
          "viewCount": 120,
          "likeCount": 18,
          "favoriteCount": 7
        }
      ]
    }
  ],
  "timestamp": 1786773600000
}
```

### 6.2 时光对比详情

```http
GET /time-comparisons/{comparisonId}
token: <accessToken>
```

权限：登录。`items` 按后台 `sortOrder`、`id` 升序，每一项包含标签和完整媒体详情。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.id` | number | 否 | 时光对比 ID |
| `data.locationId` | number | 否 | 地点 ID |
| `data.locationName` | string | 是 | 地点名称 |
| `data.title` | string | 是 | 标题 |
| `data.description` | string | 是 | 描述 |
| `data.items` | array&lt;object&gt; | 否 | 对比项数组 |
| `data.items[].label` | string | 是 | 对比项标签，如年份或时期 |
| `data.items[].media` | object | 否 | 完整媒体详情 |
| `data.items[].media.id` | number | 否 | 媒体 ID |
| `data.items[].media.uploaderId` | number | 是 | 上传者用户 ID |
| `data.items[].media.uploaderNickname` | string | 是 | 上传者昵称 |
| `data.items[].media.locationId` | number | 否 | 地点 ID |
| `data.items[].media.locationName` | string | 是 | 地点名称 |
| `data.items[].media.title` | string | 是 | 媒体标题 |
| `data.items[].media.description` | string | 是 | 媒体描述 |
| `data.items[].media.imageUrl` | string | 否 | 原图预签名 URL |
| `data.items[].media.thumbnailUrl` | string | 否 | 缩略图预签名 URL |
| `data.items[].media.shotAt` | string | 是 | 拍摄时间，ISO LocalDateTime |
| `data.items[].media.tags` | array&lt;string&gt; | 否 | 标签数组 |
| `data.items[].media.viewCount` | number | 否 | 浏览次数 |
| `data.items[].media.likeCount` | number | 否 | 点赞数 |
| `data.items[].media.favoriteCount` | number | 否 | 收藏数 |
| `data.items[].media.downloadCount` | number | 否 | 下载次数 |
| `data.items[].media.liked` | boolean | 否 | 当前用户是否已点赞 |
| `data.items[].media.favorited` | boolean | 否 | 当前用户是否已收藏 |
| `data.items[].media.createdAt` | string | 是 | 创建时间，ISO LocalDateTime |

JSON 响应示例：

```json
{
  "code": 0,
  "msg": "成功",
  "data": {
    "id": 301,
    "locationId": 101,
    "locationName": "知新楼",
    "title": "知新楼今昔",
    "description": "不同时期的知新楼影像",
    "items": [
      {
        "label": "2010年",
        "media": {
          "id": 501,
          "uploaderId": 10001,
          "uploaderNickname": "山大摄影者",
          "locationId": 101,
          "locationName": "知新楼",
          "title": "2010年的知新楼",
          "description": "旧影像",
          "imageUrl": "https://r2.example.com/old.jpg?X-Amz-Signature=...",
          "thumbnailUrl": "https://r2.example.com/old-thumb.jpg?X-Amz-Signature=...",
          "shotAt": "2010-09-01T10:00:00",
          "tags": ["历史", "建筑"],
          "viewCount": 120,
          "likeCount": 18,
          "favoriteCount": 7,
          "downloadCount": 2,
          "liked": false,
          "favorited": true,
          "createdAt": "2026-08-01T12:00:00"
        }
      }
    ]
  },
  "timestamp": 1786773600000
}
```

主要错误：

- `13300`：时光对比不存在或不可见。
- `13000`：某个关联媒体不存在或不可见。

> 服务层详情方法具备“可选用户”参数，但 Controller 当前未标记公开，因此这两个时光对比接口实际都要求登录。

## 7. 用户投稿

投稿接口除有效 access token 外，还会检查统一认证正式用户状态。创建、修改和重新提交额外要求用户 `allowUpload === true`。

### 7.1 投稿状态

| JSON 值 | 含义 |
|---|---|
| `PENDING` | 待审核 |
| `APPROVED` | 已通过/已发布 |
| `RETURNED` | 已退回修改，允许修改并重新提交 |
| `WITHDRAWN` | 已撤回 |
| `REJECTED` | 永久拒绝，不能重新提交 |

主要状态流转：

```text
创建 ──审核开启──> PENDING ──撤回──> WITHDRAWN
  │                   │
  │                   ├──后台审核通过──> APPROVED
  │                   ├──后台退回修改──> RETURNED ──重新提交──> PENDING / APPROVED
  │                   └──后台永久拒绝──> REJECTED
  │
  └──审核关闭──> APPROVED
```

### 7.2 创建投稿

```http
POST /submissions
token: <accessToken>
Content-Type: multipart/form-data
```

Multipart 字段：

| 字段 | 类型 | 必填 | 约束/说明 |
|---|---|---:|---|
| `files` | File[] | 是 | 至少 1 张，最多 9 张 |
| `locationId` | number | 是 | 正数，且地点必须已启用 |
| `shotAt` | ISO LocalDateTime | 否 | 例如 `2026-08-15T14:30:00` |
| `tagIds` | number[] | 否 | 最多 20 个；每项必须为正数，且必须是管理员已创建的现有标签 ID |
| `description` | string | 否 | 最多 2000 字符 |
| `copyrightConfirmed` | boolean | 是 | 必须为 `true` |

文件限制：

- 支持 `image/png`、`image/jpeg`、`image/webp`。
- 后端同时检查 MIME 类型和文件魔数，两者必须一致，仅改扩展名无法通过。
- 应用层单文件上限为 20 MiB；请求还可能先受到 Spring multipart 或部署网关上限限制。
- 空文件会被过滤；过滤后必须仍至少有一张有效图片。

后端会按 `tagIds` 查询现有标签，并将对应的标签名称保存到稿件中；任一 ID 不存在时返回 `17100`。重复 ID 会按首次出现顺序去重。响应中的 `data.tags` 仍为标签名称数组，而不是标签 ID 数组。

> 当前仓库只有管理员可访问的 `GET /admin/tags` 能返回标签 ID；普通用户没有获取完整可选标签列表的接口，且搜索建议中的 `TAG` 项也不返回 ID。用户端在现有接口下无法自行取得创建投稿所需的 `tagIds`。

浏览器示例：

```javascript
const form = new FormData();
for (const file of files) form.append("files", file);
form.append("locationId", String(locationId));
if (shotAt) form.append("shotAt", shotAt);
for (const tagId of tagIds) form.append("tagIds", String(tagId));
if (description !== undefined) form.append("description", description);
form.append("copyrightConfirmed", "true");

await fetch(`${baseUrl}/submissions`, {
  method: "POST",
  headers: { token: accessToken },
  body: form,
});
```

- 审核开关开启或配置记录缺失：返回 `PENDING`，`msg` 为“稿件已提交审核”。
- 审核开关关闭：立即发布，每张图片生成一条媒体记录，返回 `APPROVED`，`msg` 为“稿件已自动发布”。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.id` | number | 否 | 投稿 ID |
| `data.userId` | number | 否 | 投稿用户 ID |
| `data.locationId` | number | 否 | 拍摄地点 ID |
| `data.locationName` | string | 否 | 拍摄地点名称 |
| `data.description` | string | 是 | 投稿描述 |
| `data.shotAt` | string | 是 | 拍摄时间，ISO LocalDateTime |
| `data.tags` | array&lt;string&gt; | 否 | 标签数组；无标签时为 `[]` |
| `data.status` | string | 否 | 投稿状态，取值见 7.1 |
| `data.reviewReason` | string | 是 | 审核退回原因 |
| `data.submittedAt` | string | 是 | 最近提交时间 |
| `data.reviewedBy` | number | 是 | 审核管理员 ID；自动发布时也可能为空 |
| `data.reviewedAt` | string | 是 | 审核或自动发布时间 |
| `data.createdAt` | string | 是 | 创建时间 |
| `data.updatedAt` | string | 是 | 更新时间 |
| `data.assets` | array&lt;object&gt; | 否 | 投稿图片数组 |
| `data.assets[].id` | number | 否 | 投稿图片 ID |
| `data.assets[].originalName` | string | 是 | 上传时的原文件名 |
| `data.assets[].contentType` | string | 是 | 文件 MIME 类型 |
| `data.assets[].sizeBytes` | number | 是 | 文件大小，字节 |
| `data.assets[].sortOrder` | number | 是 | 图片排序序号，从 0 开始 |
| `data.assets[].mediaId` | number | 是 | 发布后关联的媒体 ID；待审核时通常为空 |
| `data.assets[].previewUrl` | string | 否 | 图片预览预签名 URL |

JSON 响应示例（审核开启）：

```json
{
  "code": 0,
  "msg": "稿件已提交审核",
  "data": {
    "id": 801,
    "userId": 10001,
    "locationId": 101,
    "locationName": "知新楼",
    "description": "夏日晚霞下的知新楼",
    "shotAt": "2026-08-15T18:30:00",
    "tags": ["晚霞", "建筑"],
    "status": "PENDING",
    "reviewReason": null,
    "submittedAt": "2026-08-15T19:00:00",
    "reviewedBy": null,
    "reviewedAt": null,
    "createdAt": null,
    "updatedAt": null,
    "assets": [
      {
        "id": 901,
        "originalName": "sunset.jpg",
        "contentType": "image/jpeg",
        "sizeBytes": 2457600,
        "sortOrder": 0,
        "mediaId": null,
        "previewUrl": "https://r2.example.com/submission.jpg?X-Amz-Signature=..."
      }
    ]
  },
  "timestamp": 1786773600000
}
```

主要错误：`12200`、`14000`、`14001`、`14004`、`14005`、`14007`、`14008`、`14009`、`17100`（标签不存在）、`19000` 及通用字段校验错误 `400`。

### 7.3 我的投稿

```http
GET /submissions/mine?status=PENDING&page=1&size=10
token: <accessToken>
```

权限：登录且为正式正常用户。

| 参数 | 类型 | 必填 | 默认值 | 实际处理 |
|---|---:|---:|---:|---|
| `status` | `SubmissionStatus` | 否 | - | 精确过滤状态；枚举名区分大小写 |
| `page` | number | 否 | `1` | 小于 1 按 1 |
| `size` | number | 否 | `10` | 小于 1 按 1，大于 50 按 50 |

结果按更新时间、ID 降序排列。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.total` | number | 否 | 符合条件的投稿总数 |
| `data.page` | number | 否 | 后端规范化后的当前页码 |
| `data.size` | number | 否 | 后端规范化后的每页数量 |
| `data.items` | array&lt;object&gt; | 否 | 当前页投稿摘要数组 |
| `data.items[].id` | number | 否 | 投稿 ID |
| `data.items[].locationId` | number | 否 | 地点 ID |
| `data.items[].locationName` | string | 否 | 地点名称 |
| `data.items[].description` | string | 是 | 投稿描述 |
| `data.items[].shotAt` | string | 是 | 拍摄时间，ISO LocalDateTime |
| `data.items[].status` | string | 否 | 投稿状态，取值见 7.1 |
| `data.items[].reviewReason` | string | 是 | 审核退回原因 |
| `data.items[].assetCount` | number | 否 | 投稿图片数量 |
| `data.items[].coverUrl` | string | 是 | 第一张图片的预签名预览 URL；无图片时为空 |
| `data.items[].submittedAt` | string | 是 | 最近提交时间 |
| `data.items[].updatedAt` | string | 是 | 更新时间 |

JSON 响应示例：

```json
{
  "code": 0,
  "msg": "成功",
  "data": {
    "total": 2,
    "page": 1,
    "size": 10,
    "items": [
      {
        "id": 801,
        "locationId": 101,
        "locationName": "知新楼",
        "description": "夏日晚霞下的知新楼",
        "shotAt": "2026-08-15T18:30:00",
        "status": "PENDING",
        "reviewReason": null,
        "assetCount": 1,
        "coverUrl": "https://r2.example.com/submission.jpg?X-Amz-Signature=...",
        "submittedAt": "2026-08-15T19:00:00",
        "updatedAt": "2026-08-15T19:00:00"
      }
    ]
  },
  "timestamp": 1786773600000
}
```

### 7.4 投稿详情

```http
GET /submissions/{submissionId}
token: <accessToken>
```

权限：登录。普通用户只能查看自己的投稿；管理员可查看任意投稿。不存在或无权访问统一返回 `14002`，不会泄露稿件是否存在。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.id` | number | 否 | 投稿 ID |
| `data.userId` | number | 否 | 投稿用户 ID |
| `data.locationId` | number | 否 | 拍摄地点 ID |
| `data.locationName` | string | 否 | 拍摄地点名称 |
| `data.description` | string | 是 | 投稿描述 |
| `data.shotAt` | string | 是 | 拍摄时间，ISO LocalDateTime |
| `data.tags` | array&lt;string&gt; | 否 | 标签数组 |
| `data.status` | string | 否 | 投稿状态，取值见 7.1 |
| `data.reviewReason` | string | 是 | 审核退回原因 |
| `data.submittedAt` | string | 是 | 最近提交时间 |
| `data.reviewedBy` | number | 是 | 审核管理员 ID |
| `data.reviewedAt` | string | 是 | 审核或自动发布时间 |
| `data.createdAt` | string | 是 | 创建时间 |
| `data.updatedAt` | string | 是 | 更新时间 |
| `data.assets` | array&lt;object&gt; | 否 | 投稿图片数组 |
| `data.assets[].id` | number | 否 | 投稿图片 ID |
| `data.assets[].originalName` | string | 是 | 原文件名 |
| `data.assets[].contentType` | string | 是 | 文件 MIME 类型 |
| `data.assets[].sizeBytes` | number | 是 | 文件大小，字节 |
| `data.assets[].sortOrder` | number | 是 | 图片排序序号 |
| `data.assets[].mediaId` | number | 是 | 发布后关联的媒体 ID |
| `data.assets[].previewUrl` | string | 否 | 图片预览预签名 URL |

JSON 响应示例：

```json
{
  "code": 0,
  "msg": "成功",
  "data": {
    "id": 801,
    "userId": 10001,
    "locationId": 101,
    "locationName": "知新楼",
    "description": "夏日晚霞下的知新楼",
    "shotAt": "2026-08-15T18:30:00",
    "tags": ["晚霞", "建筑"],
    "status": "RETURNED",
    "reviewReason": "请补充更准确的拍摄时间",
    "submittedAt": "2026-08-15T19:00:00",
    "reviewedBy": 20001,
    "reviewedAt": "2026-08-15T20:00:00",
    "createdAt": "2026-08-15T19:00:00",
    "updatedAt": "2026-08-15T20:00:00",
    "assets": [
      {
        "id": 901,
        "originalName": "sunset.jpg",
        "contentType": "image/jpeg",
        "sizeBytes": 2457600,
        "sortOrder": 0,
        "mediaId": null,
        "previewUrl": "https://r2.example.com/submission.jpg?X-Amz-Signature=..."
      }
    ]
  },
  "timestamp": 1786773600000
}
```

### 7.5 修改投稿

```http
PUT /submissions/{submissionId}
token: <accessToken>
Content-Type: multipart/form-data
```

仅投稿所有者可修改，且状态必须为 `PENDING` 或 `RETURNED`。Multipart 字段均为可选：

> 当前仓库的修改投稿接口尚未改用标签 ID，仍接收标签名称字段 `tags`；不要在此接口提交 `tagIds`。

| 字段 | 类型 | 默认/省略语义 | 约束/说明 |
|---|---|---|---|
| `locationId` | number | 保持原值 | 传入时必须为已启用地点 |
| `shotAt` | ISO LocalDateTime | 保持原值 | - |
| `tags` | string[] | 保持原值 | 最多 20 个，单个最多 32 字符 |
| `description` | string | 保持原值 | 最多 2000 字符 |
| `files` | File[] | 不修改图片 | 最多 9 张，并受合并后总数限制 |
| `replaceFiles` | boolean | `false` | `false` 为追加；`true` 为替换全部旧图片 |

图片处理规则：

- 默认追加，新旧图片总数不可超过 9。
- `replaceFiles=true` 时必须同时提供至少一个新文件，否则返回 `14004`。
- 当前没有按单张图片删除的接口。
- 修改 `RETURNED` 稿件不会自动重新提交，修改后仍为 `RETURNED`，需要再调用 `resubmit`。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.id` | number | 否 | 投稿 ID |
| `data.userId` | number | 否 | 投稿用户 ID |
| `data.locationId` | number | 否 | 拍摄地点 ID |
| `data.locationName` | string | 否 | 拍摄地点名称 |
| `data.description` | string | 是 | 修改后的投稿描述 |
| `data.shotAt` | string | 是 | 修改后的拍摄时间 |
| `data.tags` | array&lt;string&gt; | 否 | 修改后的标签数组 |
| `data.status` | string | 否 | 投稿状态；修改不会自动改变状态 |
| `data.reviewReason` | string | 是 | 审核退回原因 |
| `data.submittedAt` | string | 是 | 最近提交时间 |
| `data.reviewedBy` | number | 是 | 审核管理员 ID |
| `data.reviewedAt` | string | 是 | 审核时间 |
| `data.createdAt` | string | 是 | 创建时间 |
| `data.updatedAt` | string | 是 | 本次修改时间 |
| `data.assets` | array&lt;object&gt; | 否 | 修改后的完整图片数组 |
| `data.assets[].id` | number | 否 | 投稿图片 ID |
| `data.assets[].originalName` | string | 是 | 原文件名 |
| `data.assets[].contentType` | string | 是 | 文件 MIME 类型 |
| `data.assets[].sizeBytes` | number | 是 | 文件大小，字节 |
| `data.assets[].sortOrder` | number | 是 | 图片排序序号 |
| `data.assets[].mediaId` | number | 是 | 发布后关联的媒体 ID |
| `data.assets[].previewUrl` | string | 否 | 图片预览预签名 URL |

JSON 响应示例（修改被退回稿件）：

```json
{
  "code": 0,
  "msg": "稿件修改成功",
  "data": {
    "id": 801,
    "userId": 10001,
    "locationId": 101,
    "locationName": "知新楼",
    "description": "已补充拍摄时间",
    "shotAt": "2026-08-15T18:30:00",
    "tags": ["晚霞", "建筑"],
    "status": "RETURNED",
    "reviewReason": "请补充更准确的拍摄时间",
    "submittedAt": "2026-08-15T19:00:00",
    "reviewedBy": 20001,
    "reviewedAt": "2026-08-15T20:00:00",
    "createdAt": "2026-08-15T19:00:00",
    "updatedAt": "2026-08-15T20:30:00",
    "assets": [
      {
        "id": 901,
        "originalName": "sunset.jpg",
        "contentType": "image/jpeg",
        "sizeBytes": 2457600,
        "sortOrder": 0,
        "mediaId": null,
        "previewUrl": "https://r2.example.com/submission.jpg?X-Amz-Signature=..."
      }
    ]
  },
  "timestamp": 1786773600000
}
```

### 7.6 重新提交

```http
POST /submissions/{submissionId}/resubmit
token: <accessToken>
```

仅所有者、正式正常且允许投稿的用户可调用；只有 `RETURNED` 稿件可重新提交。`REJECTED` 表示永久拒绝，不能重新提交。

- 审核开启：状态变为 `PENDING`。
- 审核关闭：立即发布，状态变为 `APPROVED`。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.id` | number | 否 | 投稿 ID |
| `data.userId` | number | 否 | 投稿用户 ID |
| `data.locationId` | number | 否 | 拍摄地点 ID |
| `data.locationName` | string | 否 | 拍摄地点名称 |
| `data.description` | string | 是 | 投稿描述 |
| `data.shotAt` | string | 是 | 拍摄时间 |
| `data.tags` | array&lt;string&gt; | 否 | 标签数组 |
| `data.status` | string | 否 | 审核开启时为 `PENDING`，关闭时为 `APPROVED` |
| `data.reviewReason` | string | 是 | 重新提交后清空 |
| `data.submittedAt` | string | 是 | 本次重新提交时间 |
| `data.reviewedBy` | number | 是 | 重新提交后清空；自动发布时也为空 |
| `data.reviewedAt` | string | 是 | 审核开启时为空；自动发布时为发布时间 |
| `data.createdAt` | string | 是 | 原创建时间 |
| `data.updatedAt` | string | 是 | 更新时间 |
| `data.assets` | array&lt;object&gt; | 否 | 投稿图片数组 |
| `data.assets[].id` | number | 否 | 投稿图片 ID |
| `data.assets[].originalName` | string | 是 | 原文件名 |
| `data.assets[].contentType` | string | 是 | 文件 MIME 类型 |
| `data.assets[].sizeBytes` | number | 是 | 文件大小，字节 |
| `data.assets[].sortOrder` | number | 是 | 图片排序序号 |
| `data.assets[].mediaId` | number | 是 | 发布后关联的媒体 ID |
| `data.assets[].previewUrl` | string | 否 | 图片预览预签名 URL |

JSON 响应示例（审核开启）：

```json
{
  "code": 0,
  "msg": "稿件已重新提交审核",
  "data": {
    "id": 801,
    "userId": 10001,
    "locationId": 101,
    "locationName": "知新楼",
    "description": "已补充拍摄时间",
    "shotAt": "2026-08-15T18:30:00",
    "tags": ["晚霞", "建筑"],
    "status": "PENDING",
    "reviewReason": null,
    "submittedAt": "2026-08-15T21:00:00",
    "reviewedBy": null,
    "reviewedAt": null,
    "createdAt": "2026-08-15T19:00:00",
    "updatedAt": "2026-08-15T20:30:00",
    "assets": [
      {
        "id": 901,
        "originalName": "sunset.jpg",
        "contentType": "image/jpeg",
        "sizeBytes": 2457600,
        "sortOrder": 0,
        "mediaId": null,
        "previewUrl": "https://r2.example.com/submission.jpg?X-Amz-Signature=..."
      }
    ]
  },
  "timestamp": 1786773600000
}
```

主要错误：`14001`、`14002`、`14003`。

### 7.7 撤回投稿

```http
POST /submissions/{submissionId}/withdraw
token: <accessToken>
```

仅所有者可调用；只有 `PENDING` 稿件可撤回。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 说明 |
|---|---|---|
| `data` | null | 该接口不返回业务数据 |

JSON 响应示例：

```json
{
  "code": 0,
  "msg": "稿件已撤回",
  "data": null,
  "timestamp": 1786773600000
}
```

主要错误：`14002`、`14003`。

## 8. 管理员接口

### 8.1 更新投稿审核开关

```http
PUT /admin/settings/submission-review
token: <管理员 accessToken>
Content-Type: application/json
```

请求体：

```json
{
  "reviewEnabled": true
}
```

`reviewEnabled` 必填且不能为 `null`。成功文案为“稿件审核已开启”或“稿件审核已关闭”。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.reviewEnabled` | boolean | 否 | 更新后的投稿审核开关 |
| `data.updatedBy` | number | 是 | 本次操作的管理员用户 ID |
| `data.updatedAt` | string | 是 | 更新时间，ISO LocalDateTime |

JSON 响应示例：

```json
{
  "code": 0,
  "msg": "稿件审核已开启",
  "data": {
    "reviewEnabled": true,
    "updatedBy": 20001,
    "updatedAt": "2026-08-15T21:30:00"
  },
  "timestamp": 1786773600000
}
```

未登录或非管理员调用该接口，当前均返回 `10103 / HTTP 403`。

> 当前没有读取审核开关的 GET 接口；审核通过、退回修改和永久拒绝由下方管理员审核接口完成。

### 8.2 管理端稿件列表

```http
GET /admin/submissions?status=PENDING&keyword=图书馆&locationId=101&sort=oldest&page=1&size=20
token: <管理员 accessToken>
```

Query 参数：

| 参数 | 类型 | 必填 | 默认值 | 约束/说明 |
|---|---|---:|---:|---|
| `status` | `SubmissionStatus` | 否 | `PENDING` | 精确过滤稿件状态；枚举名区分大小写 |
| `keyword` | string | 否 | - | 最长 50 字符；模糊匹配稿件描述、标签及投稿人的手机号、CAS ID、姓名、昵称 |
| `userId` | number | 否 | - | 正数；精确过滤投稿人 |
| `locationId` | number | 否 | - | 正数；精确过滤地点 |
| `submittedFrom` | ISO LocalDateTime | 否 | - | 投稿时间下界，包含边界 |
| `submittedTo` | ISO LocalDateTime | 否 | - | 投稿时间上界，包含边界；不能早于 `submittedFrom` |
| `sort` | string | 否 | `oldest` | `oldest` 等待最久优先，`newest` 最新投稿优先；大小写不敏感 |
| `page` | number | 否 | `1` | 正整数 |
| `size` | number | 否 | `20` | 正整数；大于 100 时按 100 返回 |

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.total` | number | 否 | 符合条件的稿件总数 |
| `data.page` | number | 否 | 规范化后的页码 |
| `data.size` | number | 否 | 规范化后的每页数量 |
| `data.items` | array&lt;object&gt; | 否 | 当前页稿件摘要 |
| `data.items[].id` | number | 否 | 稿件 ID |
| `data.items[].userId` | number | 否 | 投稿人 ID |
| `data.items[].uploaderName` | string | 是 | 优先使用昵称，其次姓名、CAS ID |
| `data.items[].locationId` | number | 否 | 地点 ID |
| `data.items[].locationName` | string | 是 | 地点名称 |
| `data.items[].description` | string | 是 | 稿件描述 |
| `data.items[].shotAt` | string | 是 | 拍摄时间 |
| `data.items[].tags` | array&lt;string&gt; | 否 | 标签数组 |
| `data.items[].status` | string | 否 | 稿件状态 |
| `data.items[].reviewReason` | string | 是 | 最近审核原因 |
| `data.items[].assetCount` | number | 否 | 图片数量 |
| `data.items[].coverUrl` | string | 是 | 第一张图片的预签名 URL |
| `data.items[].submittedAt` | string | 是 | 最近投稿时间 |
| `data.items[].reviewedAt` | string | 是 | 最近审核时间 |
| `data.items[].updatedAt` | string | 是 | 更新时间 |
| `data.items[].version` | number | 否 | 当前乐观锁版本；审核时作为 `expectedVersion` 提交 |

主要错误：通用参数校验错误 `400`、非管理员 `10103`。`submittedFrom` 晚于 `submittedTo` 或 `sort` 非法时返回通用 `400`。

### 8.3 管理端稿件详情

```http
GET /admin/submissions/{submissionId}
token: <管理员 accessToken>
```

除稿件和图片信息外，响应还包含投稿人统计、当前版本以及最近 5 条审核记录：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.id` | number | 否 | 稿件 ID |
| `data.uploader` | object | 否 | 投稿人信息 |
| `data.uploader.id` | number | 否 | 投稿人 ID |
| `data.uploader.casId` | string | 是 | CAS ID |
| `data.uploader.name` | string | 是 | 姓名 |
| `data.uploader.nickname` | string | 是 | 昵称 |
| `data.uploader.avatarUrl` | string | 是 | 头像预签名 URL |
| `data.uploader.submissionCount` | number | 否 | 历史未删除稿件数 |
| `data.uploader.approvedCount` | number | 否 | 历史已通过稿件数 |
| `data.locationId` | number | 否 | 地点 ID |
| `data.locationName` | string | 是 | 地点名称 |
| `data.description` | string | 是 | 稿件描述 |
| `data.shotAt` | string | 是 | 拍摄时间 |
| `data.tags` | array&lt;string&gt; | 否 | 标签数组 |
| `data.status` | string | 否 | 稿件状态 |
| `data.reviewReason` | string | 是 | 最近审核原因 |
| `data.submittedAt` | string | 是 | 最近提交时间 |
| `data.reviewedBy` | number | 是 | 最近审核管理员 ID |
| `data.reviewerName` | string | 是 | 最近审核管理员显示名 |
| `data.reviewedAt` | string | 是 | 最近审核时间 |
| `data.createdAt` | string | 是 | 创建时间 |
| `data.updatedAt` | string | 是 | 更新时间 |
| `data.version` | number | 否 | 当前乐观锁版本 |
| `data.assets` | array&lt;object&gt; | 否 | 图片数组；字段与用户投稿详情中的 `assets` 相同 |
| `data.recentReviewLogs` | array&lt;object&gt; | 否 | 最近最多 5 条审核记录，字段见 8.6 |

主要错误：`17500` 稿件不存在、`10103` 非管理员。

### 8.4 审核单个稿件

```http
POST /admin/submissions/{submissionId}/review
token: <管理员 accessToken>
Content-Type: application/json
```

请求体：

```json
{
  "decision": "RETURN",
  "reason": "请补充准确的拍摄地点",
  "expectedVersion": 3
}
```

| 字段 | 类型 | 必填 | 约束/说明 |
|---|---|---:|---|
| `decision` | string | 是 | `APPROVE`、`RETURN`、`REJECT` |
| `reason` | string | 条件必填 | 最长 1000 字符；`RETURN` 和 `REJECT` 时去除首尾空白后必须非空 |
| `expectedVersion` | number | 是 | 非负整数；来自稿件列表或详情的 `version` |

决定与状态对应关系：

| `decision` | 审核后状态 | 行为 |
|---|---|---|
| `APPROVE` | `APPROVED` | 每张投稿图片生成一条媒体记录并回填 `mediaId` |
| `RETURN` | `RETURNED` | 作者可以修改并重新提交 |
| `REJECT` | `REJECTED` | 永久拒绝，作者不能重新提交 |

审核只允许从 `PENDING` 状态执行。状态更新、媒体发布和审核日志写入位于同一数据库事务中。更新 SQL 同时校验 `status = PENDING` 和 `version = expectedVersion`，成功后版本加一。

同一管理员使用相同 `expectedVersion` 重复提交相同 `decision` 时按幂等成功返回；审核人或决定不同则返回版本冲突。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.submissionId` | number | 否 | 稿件 ID |
| `data.status` | string | 否 | 审核后的状态 |
| `data.version` | number | 否 | 审核后的版本，即原版本加一 |
| `data.reviewReason` | string | 是 | 规范化后的审核原因 |
| `data.reviewedBy` | number | 否 | 审核管理员 ID |
| `data.reviewedAt` | string | 否 | 审核时间 |

JSON 响应示例：

```json
{
  "code": 0,
  "msg": "稿件审核成功",
  "data": {
    "submissionId": 801,
    "status": "RETURNED",
    "version": 4,
    "reviewReason": "请补充准确的拍摄地点",
    "reviewedBy": 20001,
    "reviewedAt": "2026-08-25T14:00:00"
  },
  "timestamp": 1787637600000
}
```

主要错误：`17500` 稿件不存在、`17501` 稿件已不处于待审核状态、`17502` 版本冲突、`17503` 缺少退回/拒绝原因，以及通用字段校验错误 `400`。

### 8.5 批量审核稿件

```http
POST /admin/submissions/reviews/batch
token: <管理员 accessToken>
Content-Type: application/json
```

请求体：

```json
{
  "items": [
    {
      "submissionId": 801,
      "decision": "APPROVE",
      "reason": null,
      "expectedVersion": 2
    },
    {
      "submissionId": 802,
      "decision": "RETURN",
      "reason": "地点信息不准确",
      "expectedVersion": 4
    }
  ]
}
```

`items` 必须包含 1～50 条记录，稿件 ID 不能重复。每项字段约束与单条审核一致。

每条稿件使用独立事务处理；一条失败不会回滚其他已成功稿件。接口整体成功时 HTTP 和外层 `code` 仍为成功，前端必须检查每个 `data.items[].success`。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.requestedCount` | number | 否 | 请求项数量 |
| `data.successCount` | number | 否 | 成功数量 |
| `data.failureCount` | number | 否 | 失败数量 |
| `data.items` | array&lt;object&gt; | 否 | 逐条处理结果，顺序与请求一致 |
| `data.items[].submissionId` | number | 否 | 稿件 ID |
| `data.items[].success` | boolean | 否 | 是否成功 |
| `data.items[].code` | number | 否 | 单项业务码；成功为 `0` |
| `data.items[].message` | string | 否 | 单项结果说明 |
| `data.items[].status` | string | 是 | 成功时为审核后状态，失败时为空 |
| `data.items[].version` | number | 是 | 成功时为审核后版本，失败时为空 |

请求体整体不合法，例如空数组、超过 50 条或稿件 ID 重复时，返回通用 `400`，不会开始逐条审核。

### 8.6 稿件审核记录

```http
GET /admin/submissions/{submissionId}/review-logs?page=1&size=20
token: <管理员 accessToken>
```

| 参数 | 类型 | 必填 | 默认值 | 约束/说明 |
|---|---:|---:|---:|---|
| `page` | number | 否 | `1` | 正整数 |
| `size` | number | 否 | `20` | 正整数；大于 100 时按 100 返回 |

审核记录按轮次、记录 ID 降序排列。

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.total` | number | 否 | 审核记录总数 |
| `data.page` | number | 否 | 当前页码 |
| `data.size` | number | 否 | 每页数量 |
| `data.items` | array&lt;object&gt; | 否 | 审核记录数组 |
| `data.items[].id` | number | 否 | 审核记录 ID |
| `data.items[].submissionId` | number | 否 | 稿件 ID |
| `data.items[].roundNo` | number | 否 | 审核轮次，从 1 开始 |
| `data.items[].submissionVersion` | number | 否 | 本轮审核前的稿件版本 |
| `data.items[].decision` | string | 否 | `APPROVE`、`RETURN`、`REJECT` |
| `data.items[].reason` | string | 是 | 审核原因 |
| `data.items[].beforeStatus` | string | 否 | 审核前状态，当前为 `PENDING` |
| `data.items[].afterStatus` | string | 否 | 审核后状态 |
| `data.items[].reviewedBy` | number | 否 | 审核管理员 ID |
| `data.items[].reviewerName` | string | 是 | 审核管理员显示名 |
| `data.items[].reviewedAt` | string | 否 | 审核时间 |

主要错误：`17500` 稿件不存在、`10103` 非管理员。

### 8.7 管理员用户列表

```http
GET /admin/users?keyword=张三&role=0&status=1&page=1&size=20
token: <管理员 accessToken>
```

| 参数 | 类型 | 必填 | 默认值 | 约束/说明 |
|---|---|---:|---:|---|
| `keyword` | string | 否 | - | 最长 50 字符；模糊匹配手机号、CAS ID、姓名和昵称 |
| `role` | number | 否 | - | `0` 普通用户，`1` 管理员 |
| `status` | number | 否 | - | `0` 停用，`1` 正常，`2` 冻结 |
| `page` | number | 否 | `1` | 正整数 |
| `size` | number | 否 | `20` | 正整数；大于 100 时按 100 返回 |

仅查询未删除用户，按创建时间、ID 降序排列。管理员响应中的手机号不脱敏；头像为短期预签名 URL。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.total`、`page`、`size` | number | 否 | 总数、规范化页码、每页数量 |
| `data.items` | array&lt;object&gt; | 否 | 用户数组 |
| `data.items[].id` | number | 否 | 用户 ID |
| `data.items[].phone`、`casId`、`name`、`nickname`、`avatarUrl`、`bio` | string | 是 | 手机号、统一认证信息、昵称、头像和简介 |
| `data.items[].role` | number | 否 | `0` 普通用户，`1` 管理员 |
| `data.items[].status` | number | 否 | `0` 停用，`1` 正常，`2` 冻结 |
| `data.items[].frozenUntil`、`frozenReason` | string | 是 | 冻结截止时间和原因 |
| `data.items[].allowUpload`、`allowDownload` | boolean | 否 | 投稿和原图下载权限 |
| `data.items[].lastLoginAt`、`createdAt`、`updatedAt` | string | 是 | 登录、创建、更新时间 |

```json
{
  "code": 0,
  "msg": "成功",
  "data": {
    "total": 1,
    "page": 1,
    "size": 20,
    "items": [{
      "id": 1001,
      "phone": "13812345678",
      "casId": "202300000000",
      "name": "张三",
      "nickname": "山大光影",
      "avatarUrl": null,
      "bio": "记录校园四季。",
      "role": 0,
      "status": 1,
      "frozenUntil": null,
      "frozenReason": null,
      "allowUpload": true,
      "allowDownload": false,
      "lastLoginAt": "2026-08-22T09:00:00",
      "createdAt": "2026-08-01T09:00:00",
      "updatedAt": "2026-08-22T09:00:00"
    }]
  },
  "timestamp": 1787392800000
}
```

### 8.8 管理员用户详情

```http
GET /admin/users/{userId}
token: <管理员 accessToken>
```

`userId` 必须为正数；已逻辑删除的用户按不存在处理。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.id` | number | 否 | 用户 ID |
| `data.phone`、`casId`、`name`、`nickname`、`avatarUrl`、`bio` | string | 是 | 未脱敏手机号、统一认证信息、昵称、头像和简介 |
| `data.role`、`status` | number | 否 | 角色 `0/1`；状态 `0/1/2` |
| `data.frozenUntil`、`frozenReason` | string | 是 | 冻结信息 |
| `data.allowUpload`、`allowDownload` | boolean | 否 | 投稿、下载权限 |
| `data.lastLoginAt`、`createdAt`、`updatedAt` | string | 是 | 时间字段 |

JSON 响应示例：

```json
{"code":0,"msg":"成功","data":{"id":1001,"phone":"13812345678","casId":"202300000000","name":"张三","nickname":"山大光影","avatarUrl":null,"bio":"记录校园四季。","role":0,"status":1,"frozenUntil":null,"frozenReason":null,"allowUpload":true,"allowDownload":false,"lastLoginAt":"2026-08-22T09:00:00","createdAt":"2026-08-01T09:00:00","updatedAt":"2026-08-22T09:00:00"},"timestamp":1787392800000}
```

主要错误：`17000` 用户不存在。

### 8.9 修改用户角色

```http
PATCH /admin/users/{userId}/role
token: <管理员 accessToken>
Content-Type: application/json

{"role": 1}
```

`role` 必填，只允许 `0` 或 `1`。不能修改自己的管理员角色，也不能降级系统中最后一名正常管理员。角色实际变化后，目标用户所有现有 access token 和 refresh token 都会失效。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.id` | number | 否 | 目标用户 ID |
| `data.phone`、`casId`、`name`、`nickname`、`avatarUrl`、`bio` | string | 是 | 用户资料 |
| `data.role` | number | 否 | 修改后的角色 |
| `data.status` | number | 否 | 当前状态 |
| `data.frozenUntil`、`frozenReason` | string | 是 | 冻结信息 |
| `data.allowUpload`、`allowDownload` | boolean | 否 | 投稿、下载权限 |
| `data.lastLoginAt`、`createdAt`、`updatedAt` | string | 是 | 时间字段 |

JSON 响应示例：

```json
{
  "code": 0,
  "msg": "用户角色修改成功",
  "data": {
    "id": 1001,
    "phone": "13812345678",
    "casId": "202300000000",
    "name": "张三",
    "nickname": "山大光影",
    "avatarUrl": null,
    "bio": null,
    "role": 1,
    "status": 1,
    "frozenUntil": null,
    "frozenReason": null,
    "allowUpload": true,
    "allowDownload": false,
    "lastLoginAt": "2026-08-22T09:00:00",
    "createdAt": "2026-08-01T09:00:00",
    "updatedAt": "2026-08-22T10:00:00"
  },
  "timestamp": 1787392800000
}
```

主要错误：`17000` 用户不存在、`17001` 修改自身角色、`17003` 不能移除最后一名正常管理员。

### 8.10 修改用户状态

```http
PATCH /admin/users/{userId}/status
token: <管理员 accessToken>
Content-Type: application/json
```

请求体字段：

| 字段 | JSON 类型 | 必填 | 说明 |
|---|---|---:|---|
| `status` | number | 是 | `0` 停用、`1` 正常、`2` 冻结 |
| `frozenUntil` | string | 冻结时是 | `status=2` 时必须晚于服务器当前时间；其他状态忽略并清空 |
| `frozenReason` | string | 冻结时是 | 去除首尾空白后非空，最长 255；其他状态忽略并清空 |

```json
{
  "status": 2,
  "frozenUntil": "2026-09-01T00:00:00",
  "frozenReason": "违反社区规范"
}
```

不能停用或冻结自己，也不能使最后一名正常管理员不可用。修改成功会使目标用户全部登录凭据失效。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.id` | number | 否 | 目标用户 ID |
| `data.phone`、`casId`、`name`、`nickname`、`avatarUrl`、`bio` | string | 是 | 用户资料 |
| `data.role`、`status` | number | 否 | 当前角色和修改后的状态 |
| `data.frozenUntil`、`frozenReason` | string | 是 | 冻结状态下的截止时间和原因 |
| `data.allowUpload`、`allowDownload` | boolean | 否 | 投稿、下载权限 |
| `data.lastLoginAt`、`createdAt`、`updatedAt` | string | 是 | 时间字段 |

JSON 响应示例：

```json
{
  "code": 0,
  "msg": "用户状态修改成功",
  "data": {
    "id": 1001,
    "phone": "13812345678",
    "casId": "202300000000",
    "name": "张三",
    "nickname": "山大光影",
    "avatarUrl": null,
    "bio": null,
    "role": 0,
    "status": 2,
    "frozenUntil": "2026-09-01T00:00:00",
    "frozenReason": "违反社区规范",
    "allowUpload": true,
    "allowDownload": false,
    "lastLoginAt": "2026-08-22T09:00:00",
    "createdAt": "2026-08-01T09:00:00",
    "updatedAt": "2026-08-22T10:10:00"
  },
  "timestamp": 1787392800000
}
```

主要错误：`17000`、`17002`、`17003`、`17004`、`17005`。

### 8.11 修改用户功能权限

```http
PATCH /admin/users/{userId}/permissions
token: <管理员 accessToken>
Content-Type: application/json

{"allowUpload": true, "allowDownload": false}
```

`allowUpload` 和 `allowDownload` 都可选，`null` 或省略表示不修改；至少提供一项非 `null` 值。该操作不会主动使目标用户现有 token 失效。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.id` | number | 否 | 目标用户 ID |
| `data.phone`、`casId`、`name`、`nickname`、`avatarUrl`、`bio` | string | 是 | 用户资料 |
| `data.role`、`status` | number | 否 | 角色和状态 |
| `data.frozenUntil`、`frozenReason` | string | 是 | 冻结信息 |
| `data.allowUpload`、`allowDownload` | boolean | 否 | 修改后的投稿、下载权限 |
| `data.lastLoginAt`、`createdAt`、`updatedAt` | string | 是 | 时间字段 |

JSON 响应示例：

```json
{
  "code": 0,
  "msg": "用户权限修改成功",
  "data": {
    "id": 1001,
    "phone": "13812345678",
    "casId": "202300000000",
    "name": "张三",
    "nickname": "山大光影",
    "avatarUrl": null,
    "bio": null,
    "role": 0,
    "status": 1,
    "frozenUntil": null,
    "frozenReason": null,
    "allowUpload": true,
    "allowDownload": false,
    "lastLoginAt": "2026-08-22T09:00:00",
    "createdAt": "2026-08-01T09:00:00",
    "updatedAt": "2026-08-22T10:20:00"
  },
  "timestamp": 1787392800000
}
```

主要错误：`17000` 用户不存在、`17006` 没有提供可修改权限。

### 8.12 创建地点

```http
POST /admin/locations
token: <管理员 accessToken>
Content-Type: application/json
```

请求体字段：

| 字段 | JSON 类型 | 必填 | 约束/说明 |
|---|---|---:|---|
| `campusId` | number | 是 | 正数，且校区必须启用 |
| `name` | string | 是 | 去除首尾空白后非空，最长 100 |
| `categoryCode` | string | 是 | 从 `/location-categories` 选择；大写字母开头，只能包含大写字母、数字和下划线，且必须对应启用分类 |
| `address` | string | 否 | 最长 255；空白保存为 `null` |
| `longitude` | number | 是 | `-180～180` |
| `latitude` | number | 是 | `-90～90` |
| `coverKey` | string | 否 | 最长 512；数据库保存 R2 ObjectKey，不能传 URL 或绝对路径；未传或空白时暂用 `avatars/default.png` |
| `description` | string | 否 | 最长 2000 |
| `sortOrder` | number | 否 | 非负，默认 `0` |
| `status` | number | 否 | `0` 停用、`1` 启用，默认 `1` |

```json
{
  "campusId": 1,
  "name": "新建教学楼",
  "categoryCode": "BUILDING",
  "address": "中心校区内",
  "longitude": 117.0618,
  "latitude": 36.6745,
  "coverKey": "locations/admin/zhixin-building.jpg",
  "description": "教学建筑",
  "sortOrder": 10,
  "status": 1
}
```

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.id`、`campusId` | number | 否 | 地点、校区 ID |
| `data.name` | string | 否 | 地点名称 |
| `data.categoryCode` | string | 否 | 地点分类代码 |
| `data.address`、`coverKey`、`description` | string | 是 | 地址、持久化封面 ObjectKey、描述 |
| `data.longitude`、`latitude` | number | 否 | 经纬度 |
| `data.sortOrder`、`status` | number | 否 | 排序值、状态 `0/1` |
| `data.createdAt`、`updatedAt` | string | 是 | 创建、更新时间 |

```json
{
  "code": 0,
  "msg": "地点创建成功",
  "data": {
    "id": 110,
    "campusId": 1,
    "name": "新建教学楼",
    "categoryCode": "BUILDING",
    "address": "中心校区内",
    "longitude": 117.0618,
    "latitude": 36.6745,
    "coverKey": "locations/admin/zhixin-building.jpg",
    "description": "教学建筑",
    "sortOrder": 10,
    "status": 1,
    "createdAt": "2026-08-22T10:30:00",
    "updatedAt": "2026-08-22T10:30:00"
  },
  "timestamp": 1787392800000
}
```

主要错误：`17112` 目标校区不存在或停用、`17113` 地点分类不存在或停用，以及通用字段校验错误 `400`。

### 8.13 修改地点

```http
PATCH /admin/locations/{locationId}
token: <管理员 accessToken>
Content-Type: application/json
```

路径 ID 必须为正数。请求字段与创建地点相同但全部可选；至少提供一个非 `null` 字段。`categoryCode` 传入时不能为空且必须对应启用分类；`coverKey`、地址和描述传空白可清为 `null`，名称不能清空；未提供的字段保持原值。地点恢复启用时会重新校验当前校区和分类。

请求示例：

```json
{"description": "更新后的地点介绍", "sortOrder": 20, "status": 0}
```

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.id`、`campusId` | number | 否 | 地点、校区 ID |
| `data.name` | string | 否 | 地点名称 |
| `data.categoryCode` | string | 否 | 地点分类代码 |
| `data.address`、`coverKey`、`description` | string | 是 | 地址、持久化封面 ObjectKey、描述 |
| `data.longitude`、`latitude` | number | 否 | 经纬度 |
| `data.sortOrder`、`status` | number | 否 | 排序值和更新后状态 |
| `data.createdAt`、`updatedAt` | string | 是 | 创建、更新时间 |

JSON 响应示例：

```json
{"code":0,"msg":"地点修改成功","data":{"id":110,"campusId":1,"name":"新建教学楼","categoryCode":"BUILDING","address":"中心校区内","longitude":117.0618,"latitude":36.6745,"coverKey":"locations/admin/zhixin-building.jpg","description":"更新后的地点介绍","sortOrder":20,"status":0,"createdAt":"2026-08-22T10:30:00","updatedAt":"2026-08-22T10:35:00"},"timestamp":1787392800000}
```

主要错误：`17110` 地点不存在、`17111` 空更新、`17112` 新校区无效、`17113` 地点分类不存在或停用。

### 8.14 管理员媒体列表

```http
GET /admin/media?keyword=晚霞&locationId=101&status=1&page=1&size=20
token: <管理员 accessToken>
```

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---:|---:|---|
| `keyword` | string | 否 | - | 最长 50；模糊匹配标题、描述、标签编码 |
| `locationId` | number | 否 | - | 正数地点 ID；只作筛选，不校验地点是否存在 |
| `status` | number | 否 | - | `0` 隐藏、`1` 可见 |
| `page` | number | 否 | `1` | 正整数 |
| `size` | number | 否 | `20` | 正整数；大于 100 时按 100 返回 |

结果包含隐藏媒体，按更新时间、ID 降序排列；图片 URL 均为短期预签名 URL。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.total`、`page`、`size` | number | 否 | 分页信息 |
| `data.items` | array&lt;object&gt; | 否 | 媒体数组 |
| `data.items[].id` | number | 否 | 媒体 ID |
| `data.items[].submissionId`、`uploaderId`、`locationId` | number | 是 | 来源投稿、上传者、地点 ID |
| `data.items[].imageUrl`、`thumbnailUrl`、`title`、`description` | string | 是 | 图片、缩略图、标题、描述 |
| `data.items[].shotAt` | string | 是 | 拍摄时间 |
| `data.items[].tags` | array&lt;string&gt; | 否 | 标签名称数组 |
| `data.items[].status` | number | 否 | `0` 隐藏、`1` 可见 |
| `data.items[].viewCount`、`likeCount`、`favoriteCount`、`downloadCount` | number | 否 | 各类计数 |
| `data.items[].createdAt`、`updatedAt` | string | 是 | 创建、更新时间 |

```json
{
  "code": 0,
  "msg": "成功",
  "data": {
    "total": 1,
    "page": 1,
    "size": 20,
    "items": [{
      "id": 501,
      "submissionId": 701,
      "uploaderId": 1001,
      "locationId": 101,
      "imageUrl": "https://r2.example.com/media-501.jpg?X-Amz-Signature=...",
      "thumbnailUrl": "https://r2.example.com/thumb-501.jpg?X-Amz-Signature=...",
      "title": "知新楼晚霞",
      "description": "傍晚的知新楼",
      "shotAt": "2026-08-15T18:30:00",
      "tags": ["建筑", "晚霞"],
      "status": 1,
      "viewCount": 121,
      "likeCount": 12,
      "favoriteCount": 7,
      "downloadCount": 2,
      "createdAt": "2026-08-15T19:00:00",
      "updatedAt": "2026-08-22T10:00:00"
    }]
  },
  "timestamp": 1787392800000
}
```

### 8.15 修改媒体分类

```http
PATCH /admin/media/{mediaId}/classification
token: <管理员 accessToken>
Content-Type: application/json
```

| 字段 | JSON 类型 | 必填 | 说明 |
|---|---|---:|---|
| `locationId` | number | 否 | 正数；目标地点自身必须启用 |
| `tagIds` | array&lt;number&gt; | 否 | 最多 20 个正数标签 ID，不得重复；`[]` 表示清空标签 |

至少提供一个非 `null` 字段。标签关系实际以标签名称编码写入媒体。

```json
{"locationId": 101, "tagIds": [1, 2]}
```

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.id` | number | 否 | 媒体 ID |
| `data.submissionId`、`uploaderId`、`locationId` | number | 是 | 来源投稿、上传者、修改后的地点 ID |
| `data.imageUrl`、`thumbnailUrl`、`title`、`description` | string | 是 | 图片、缩略图、标题、描述 |
| `data.shotAt` | string | 是 | 拍摄时间 |
| `data.tags` | array&lt;string&gt; | 否 | 修改后的标签名称数组 |
| `data.status` | number | 否 | `0/1` |
| `data.viewCount`、`likeCount`、`favoriteCount`、`downloadCount` | number | 否 | 各类计数 |
| `data.createdAt`、`updatedAt` | string | 是 | 创建、更新时间 |

```json
{"code":0,"msg":"媒体分类更新成功","data":{"id":501,"submissionId":701,"uploaderId":1001,"locationId":101,"imageUrl":"https://r2.example.com/media-501.jpg?X-Amz-Signature=...","thumbnailUrl":"https://r2.example.com/thumb-501.jpg?X-Amz-Signature=...","title":"知新楼晚霞","description":"傍晚的知新楼","shotAt":"2026-08-15T18:30:00","tags":["建筑","晚霞"],"status":1,"viewCount":121,"likeCount":12,"favoriteCount":7,"downloadCount":2,"createdAt":"2026-08-15T19:00:00","updatedAt":"2026-08-22T10:30:00"},"timestamp":1787392800000}
```

主要错误：`17200` 媒体不存在、`17201` 空分类、`17202` 地点无效、`17100` 标签不存在，以及重复标签 ID 对应通用 `400`。

### 8.16 隐藏媒体

```http
POST /admin/media/{mediaId}/hide
token: <管理员 accessToken>
```

无请求体。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.id` | number | 否 | 媒体 ID |
| `data.submissionId`、`uploaderId`、`locationId` | number | 是 | 来源投稿、上传者、地点 ID |
| `data.imageUrl`、`thumbnailUrl`、`title`、`description` | string | 是 | 图片、缩略图、标题、描述 |
| `data.shotAt` | string | 是 | 拍摄时间 |
| `data.tags` | array&lt;string&gt; | 否 | 标签数组 |
| `data.status` | number | 否 | 隐藏成功后为 `0` |
| `data.viewCount`、`likeCount`、`favoriteCount`、`downloadCount` | number | 否 | 各类计数 |
| `data.createdAt`、`updatedAt` | string | 是 | 创建、更新时间 |

```json
{"code":0,"msg":"媒体已隐藏","data":{"id":501,"submissionId":701,"uploaderId":1001,"locationId":101,"imageUrl":"https://r2.example.com/media-501.jpg?X-Amz-Signature=...","thumbnailUrl":"https://r2.example.com/thumb-501.jpg?X-Amz-Signature=...","title":"知新楼晚霞","description":"傍晚的知新楼","shotAt":"2026-08-15T18:30:00","tags":["建筑","晚霞"],"status":0,"viewCount":121,"likeCount":12,"favoriteCount":7,"downloadCount":2,"createdAt":"2026-08-15T19:00:00","updatedAt":"2026-08-22T10:40:00"},"timestamp":1787392800000}
```

主要错误：`17200` 媒体不存在、`17203` 已经隐藏。

### 8.17 恢复媒体

```http
POST /admin/media/{mediaId}/restore
token: <管理员 accessToken>
```

无请求体。媒体必须配置一个自身启用的地点。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.id` | number | 否 | 媒体 ID |
| `data.submissionId`、`uploaderId`、`locationId` | number | 是 | 来源投稿、上传者、地点 ID |
| `data.imageUrl`、`thumbnailUrl`、`title`、`description` | string | 是 | 图片、缩略图、标题、描述 |
| `data.shotAt` | string | 是 | 拍摄时间 |
| `data.tags` | array&lt;string&gt; | 否 | 标签数组 |
| `data.status` | number | 否 | 恢复成功后为 `1` |
| `data.viewCount`、`likeCount`、`favoriteCount`、`downloadCount` | number | 否 | 各类计数 |
| `data.createdAt`、`updatedAt` | string | 是 | 创建、更新时间 |

```json
{"code":0,"msg":"媒体已恢复","data":{"id":501,"submissionId":701,"uploaderId":1001,"locationId":101,"imageUrl":"https://r2.example.com/media-501.jpg?X-Amz-Signature=...","thumbnailUrl":"https://r2.example.com/thumb-501.jpg?X-Amz-Signature=...","title":"知新楼晚霞","description":"傍晚的知新楼","shotAt":"2026-08-15T18:30:00","tags":["建筑","晚霞"],"status":1,"viewCount":121,"likeCount":12,"favoriteCount":7,"downloadCount":2,"createdAt":"2026-08-15T19:00:00","updatedAt":"2026-08-22T10:50:00"},"timestamp":1787392800000}
```

主要错误：`17200` 媒体不存在、`17202` 地点缺失或停用、`17204` 已经可见。

### 8.18 永久删除媒体

```http
DELETE /admin/media/{mediaId}
token: <管理员 accessToken>
```

该操作永久删除媒体、点赞、收藏、下载、足迹、专题、投稿资源关联及 R2 原图/缩略图；收藏夹手动封面会被清空。时光对比删除该媒体后不足两项时，整个时光对比也会删除。此操作不可恢复。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data` | null | 是 | 固定为 `null` |

```json
{"code":0,"msg":"媒体及其存储文件已删除","data":null,"timestamp":1787392800000}
```

主要错误：`17200` 媒体不存在、`17205 / HTTP 502` 存储文件删除失败。

### 8.19 管理员标签列表

```http
GET /admin/tags?keyword=建筑
token: <管理员 accessToken>
```

`keyword` 可选，最长 32 字符，按标签名模糊匹配。接口不分页，按标签名、ID 升序；`mediaCount` 统计所有引用该标签名的媒体，包括隐藏媒体。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data` | array&lt;object&gt; | 否 | 标签数组 |
| `data[].id` | number | 否 | 标签 ID |
| `data[].name` | string | 否 | 标签名称 |
| `data[].mediaCount` | number | 否 | 引用该标签的媒体数 |
| `data[].createdAt`、`updatedAt` | string | 是 | 创建、更新时间 |

```json
{"code":0,"msg":"成功","data":[{"id":1,"name":"建筑","mediaCount":36,"createdAt":"2026-08-18T09:00:00","updatedAt":"2026-08-18T09:00:00"}],"timestamp":1787392800000}
```

### 8.20 创建标签

```http
POST /admin/tags
token: <管理员 accessToken>
Content-Type: application/json

{"name":"建筑"}
```

`name` 必填、最长 32，后端会去除首尾空白和所有 `|` 字符；规范化后不能为空且不能与现有标签同名。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.id` | number | 否 | 新标签 ID |
| `data.name` | string | 否 | 规范化后的名称 |
| `data.mediaCount` | number | 否 | 固定从 `0` 开始 |
| `data.createdAt`、`updatedAt` | string | 是 | 创建、更新时间 |

```json
{"code":0,"msg":"标签创建成功","data":{"id":1,"name":"建筑","mediaCount":0,"createdAt":"2026-08-22T11:00:00","updatedAt":"2026-08-22T11:00:00"},"timestamp":1787392800000}
```

主要错误：`17101` 标签名已存在，以及通用字段错误 `400`。

### 8.21 修改标签

```http
PATCH /admin/tags/{tagId}
token: <管理员 accessToken>
Content-Type: application/json

{"name":"校园建筑"}
```

名称规则同 8.15。改名会同步替换所有媒体（包括隐藏媒体）中的该标签，并对重复标签去重。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.id`、`mediaCount` | number | 否 | 标签 ID、改名后的引用数 |
| `data.name` | string | 否 | 新名称 |
| `data.createdAt`、`updatedAt` | string | 是 | 创建、更新时间 |

```json
{"code":0,"msg":"标签修改成功","data":{"id":1,"name":"校园建筑","mediaCount":36,"createdAt":"2026-08-18T09:00:00","updatedAt":"2026-08-22T11:10:00"},"timestamp":1787392800000}
```

主要错误：`17100` 标签不存在、`17101` 新名称已存在。

### 8.22 合并或删除标签

```http
POST /admin/tags/{tagId}/merge
token: <管理员 accessToken>
Content-Type: application/json
```

| 字段 | JSON 类型 | 必填 | 说明 |
|---|---|---:|---|
| `targetTagId` | number | 否 | 正数目标标签 ID；传 `null` 或省略表示删除源标签，非空表示合并 |

合并会把所有媒体中的源标签替换为目标标签并去重，然后删除源标签；删除则从所有媒体移除源标签。不能合并到自身。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data` | null | 是 | 固定为 `null` |

```json
{"code":0,"msg":"标签合并成功","data":null,"timestamp":1787392800000}
```

`targetTagId` 为空时成功文案为“标签已删除”。主要错误：`17100` 源或目标标签不存在、`17103` 合并到自身。

### 8.23 创建专题

```http
POST /admin/topics
token: <管理员 accessToken>
Content-Type: application/json
```

| 字段 | JSON 类型 | 必填 | 约束/说明 |
|---|---|---:|---|
| `name` | string | 是 | 非空，最长 100 |
| `slug` | string | 是 | 1～64 位，小写字母或数字开头，只含小写字母、数字、连字符；全局唯一 |
| `description` | string | 否 | 最长 1000，空白保存为 `null` |
| `coverKey` | string | 否 | 最长 1000，数据库直存 URL |
| `status` | number | 否 | `0` 停用、`1` 启用，默认 `1` |
| `sortOrder` | number | 否 | 非负，默认 `0` |
| `mediaIds` | array&lt;number&gt; | 否 | 最多 200 个正数 ID，不得重复；按数组顺序保存，默认空数组 |

```json
{"name":"校园建筑","slug":"campus-buildings","description":"校园代表性建筑","coverUrl":null,"status":1,"sortOrder":0,"mediaIds":[501,502]}
```

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.id` | number | 否 | 专题 ID |
| `data.name`、`slug` | string | 否 | 名称、标识 |
| `data.description`、`coverKey` | string | 是 | 描述、封面地址 |
| `data.status`、`sortOrder` | number | 否 | 状态、排序值 |
| `data.mediaIds` | array&lt;number&gt; | 否 | 关联媒体 ID，保持配置顺序 |
| `data.createdAt`、`updatedAt` | string | 是 | 创建、更新时间 |

```json
{"code":0,"msg":"专题创建成功","data":{"id":81,"name":"校园建筑","slug":"campus-buildings","description":"校园代表性建筑","coverUrl":null,"status":1,"sortOrder":0,"mediaIds":[501,502],"createdAt":"2026-08-22T11:20:00","updatedAt":"2026-08-22T11:20:00"},"timestamp":1787392800000}
```

主要错误：`17401` slug 已存在、`17200` 关联媒体不存在，以及通用字段错误 `400`。当前只校验媒体存在，不要求媒体可见。

### 8.24 修改专题

```http
PATCH /admin/topics/{topicId}
token: <管理员 accessToken>
Content-Type: application/json
```

请求字段及约束同 8.18，但全部可选；至少提供一个非 `null` 字段。`mediaIds: []` 清空关联，省略或 `null` 表示不修改；字符串空白可清空描述/封面，但不能清空名称。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.id` | number | 否 | 专题 ID |
| `data.name`、`slug` | string | 否 | 更新后的名称、标识 |
| `data.description`、`coverKey` | string | 是 | 描述、封面 |
| `data.status`、`sortOrder` | number | 否 | 状态、排序值 |
| `data.mediaIds` | array&lt;number&gt; | 否 | 更新后的媒体 ID 顺序 |
| `data.createdAt`、`updatedAt` | string | 是 | 创建、更新时间 |

JSON 响应示例：

```json
{"code":0,"msg":"专题修改成功","data":{"id":81,"name":"校园建筑精选","slug":"campus-buildings","description":"校园代表性建筑","coverUrl":null,"status":1,"sortOrder":1,"mediaIds":[502,501],"createdAt":"2026-08-22T11:20:00","updatedAt":"2026-08-22T11:25:00"},"timestamp":1787392800000}
```

主要错误：`17400` 专题不存在、`17401` slug 已存在、`17402` 空更新、`17200` 媒体不存在。

### 8.25 创建时光对比

```http
POST /admin/time-comparisons
token: <管理员 accessToken>
Content-Type: application/json
```

| 字段路径 | JSON 类型 | 必填 | 约束/说明 |
|---|---|---:|---|
| `locationId` | number | 是 | 正数，地点自身必须启用 |
| `title` | string | 是 | 非空，最长 200 |
| `description` | string | 否 | 最长 2000，空白保存为 `null` |
| `status` | number | 否 | `0` 隐藏、`1` 可见，默认 `1` |
| `items` | array&lt;object&gt; | 是 | 2～20 项，媒体 ID 不能重复 |
| `items[].mediaId` | number | 是 | 必须是可见且属于 `locationId` 的媒体 |
| `items[].label` | string | 否 | 最长 100，空白保存为 `null` |
| `items[].sortOrder` | number | 否 | 非负；省略时使用数组下标，从 `0` 开始 |

```json
{"locationId":101,"title":"知新楼今昔","description":"不同年代的知新楼","status":1,"items":[{"mediaId":501,"label":"2010年","sortOrder":0},{"mediaId":502,"label":"2026年","sortOrder":1}]}
```

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.id`、`locationId` | number | 否 | 对比、地点 ID |
| `data.title` | string | 否 | 标题 |
| `data.description` | string | 是 | 描述 |
| `data.status` | number | 否 | `0/1` |
| `data.items` | array&lt;object&gt; | 否 | 已创建的对比项 |
| `data.items[].id`、`mediaId`、`sortOrder` | number | 否 | 对比项 ID、媒体 ID、排序值 |
| `data.items[].label` | string | 是 | 展示标签 |
| `data.createdAt`、`updatedAt` | string | 是 | 创建、更新时间 |

```json
{"code":0,"msg":"时光对比创建成功","data":{"id":61,"locationId":101,"title":"知新楼今昔","description":"不同年代的知新楼","status":1,"items":[{"id":601,"mediaId":501,"label":"2010年","sortOrder":0},{"id":602,"mediaId":502,"label":"2026年","sortOrder":1}],"createdAt":"2026-08-22T11:30:00","updatedAt":"2026-08-22T11:30:00"},"timestamp":1787392800000}
```

主要错误：`17301` 地点不存在或停用、`17300` 媒体不存在/隐藏/不属于该地点，以及重复媒体对应通用 `400`。

## 9. 健康检查接口及其他代码现状提醒

`PingController` 提供三种权限级别的健康检查。

### 9.1 公开健康检查

```http
GET /ping/public
token: <可选 accessToken>
```

权限：公开。若携带有效 token，`msg` 末尾会追加当前用户 ID。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 说明 |
|---|---|---|
| `data` | null | 该接口不返回业务数据 |

JSON 响应示例（未携带 token）：

```json
{
  "code": 0,
  "msg": "public ping successfully",
  "data": null,
  "timestamp": 1786773600000
}
```

### 9.2 登录健康检查

```http
GET /ping/auth
token: <accessToken>
```

权限：登录。`msg` 包含当前用户角色和用户 ID。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 说明 |
|---|---|---|
| `data` | null | 该接口不返回业务数据 |

JSON 响应示例：

```json
{
  "code": 0,
  "msg": "auth ping successfully. Welcome, USER 10001 !",
  "data": null,
  "timestamp": 1786773600000
}
```

### 9.3 管理员健康检查

```http
GET /ping/admin
token: <管理员 accessToken>
```

权限：管理员。`msg` 包含管理员用户 ID。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 说明 |
|---|---|---|
| `data` | null | 该接口不返回业务数据 |

JSON 响应示例：

```json
{
  "code": 0,
  "msg": "admin ping successfully. Welcome, ADMIN 20001 !",
  "data": null,
  "timestamp": 1786773600000
}
```

`/ping/public` 可用于不需要身份的基础存活检查；`/ping/auth` 和 `/ping/admin` 可分别验证登录鉴权与管理员鉴权链路。

### 9.4 其他代码现状提醒

- `UserController` 只有 `/users` 类级路径，没有任何实际方法映射。
- `/auth/register` 方法存在，但 `@PostMapping` 已注释，不是可调用接口。
- 仓库没有 CORS 配置；浏览器跨域是否可用取决于同源部署或外层网关配置。
- 仓库没有 OpenAPI/Swagger 配置，本文字段来自当前 Java 类型和实际组装逻辑。


## 10. 搜索、发现与话题

### 10.1 搜索建议

```http
GET /search/suggestions?keyword=中心校区&limit=10
```

权限：公开。

Query 参数：

| 参数 | 类型 | 必填 | 默认值 | 约束/说明 |
|---|---|---:|---|---|
| `keyword` | string | 否 | - | 推荐参数；去除首尾空白后最长 50 字符 |
| `q` | string | 否 | - | 兼容旧客户端；仅在 `keyword` 为空时使用 |
| `limit` | number | 否 | `10` | 小于 1 按 1，大于 20 按 20 |

`keyword` 和 `q` 都未传或均为空时，接口仍会返回通用建议。建议按地点、校区、标签、媒体的顺序去重收集，达到 `limit` 后停止。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data` | array&lt;object&gt; | 否 | 搜索建议数组；没有结果时为 `[]` |
| `data[].type` | string | 否 | `LOCATION`、`CAMPUS`、`TAG` 或 `MEDIA` |
| `data[].id` | number | 是 | 地点、校区或媒体 ID；标签建议为 `null` |
| `data[].text` | string | 否 | 建议主文本 |
| `data[].subtitle` | string | 是 | 校区、地址、地点名称或“标签”等辅助信息 |

JSON 响应示例：

```json
{
  "code": 0,
  "msg": "成功",
  "data": [
    {
      "type": "LOCATION",
      "id": 101,
      "text": "知新楼",
      "subtitle": "中心校区"
    },
    {
      "type": "CAMPUS",
      "id": 1,
      "text": "中心校区",
      "subtitle": "山东省济南市山大南路27号"
    },
    {
      "type": "TAG",
      "id": null,
      "text": "建筑",
      "subtitle": "标签"
    },
    {
      "type": "MEDIA",
      "id": 501,
      "text": "知新楼晚霞",
      "subtitle": "知新楼"
    }
  ],
  "timestamp": 1786860000000
}
```

主要错误：`15000` 搜索关键词超过 50 字符，以及参数类型错误对应的通用 `400`。

### 10.2 媒体综合搜索

```http
GET /search/media?q=晚霞&cityId=1&sort=relevance&page=1&size=20
```

权限：公开。所有筛选条件同时生效；只返回媒体、地点、校区、城市均处于启用状态的数据。

Query 参数：

| 参数 | 类型 | 必填 | 默认值 | 约束/说明 |
|---|---|---:|---|---|
| `q` | string | 否 | - | 最长 50 字符；匹配媒体标题、描述、标签、地点名称/地址、校区名称和城市名称 |
| `cityId` | number | 否 | - | 正整数；按城市过滤 |
| `campusId` | number | 否 | - | 正整数；按校区过滤 |
| `locationId` | number | 否 | - | 正整数；按地点过滤 |
| `topicId` | number | 否 | - | 正整数；按启用话题过滤 |
| `tag` | string | 否 | - | 最长 30 字符；按完整标签过滤，不是模糊匹配 |
| `shotYear` | number | 否 | - | `1900` 至 `2100` |
| `sort` | string | 否 | `relevance` | `relevance`、`newest`、`oldest`、`hot`；忽略大小写和首尾空白 |
| `page` | number | 否 | `1` | `1` 至 `10000` |
| `size` | number | 否 | `20` | `1` 至 `50` |

排序含义：

- `relevance`：优先精确命中和前缀命中，再按收藏、点赞、浏览热度排序。
- `newest`：按拍摄时间排序；无拍摄时间时使用创建时间，新的在前。
- `oldest`：按拍摄时间排序；无拍摄时间时使用创建时间，旧的在前。
- `hot`：按 `收藏数 × 5 + 点赞数 × 3 + 浏览数` 降序。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.total` | number | 否 | 符合条件的媒体总数 |
| `data.page` | number | 否 | 当前页码 |
| `data.size` | number | 否 | 每页数量 |
| `data.items` | array&lt;object&gt; | 否 | 当前页媒体摘要数组 |
| `data.items[].id` | number | 否 | 媒体 ID |
| `data.items[].title` | string | 是 | 媒体标题 |
| `data.items[].locationId` | number | 否 | 地点 ID |
| `data.items[].locationName` | string | 是 | 地点名称 |
| `data.items[].thumbnailUrl` | string | 否 | 缩略图预签名 URL |
| `data.items[].shotAt` | string | 是 | 拍摄时间，ISO LocalDateTime |
| `data.items[].viewCount` | number | 否 | 浏览次数 |
| `data.items[].likeCount` | number | 否 | 点赞数 |
| `data.items[].favoriteCount` | number | 否 | 收藏数 |

JSON 响应示例：

```json
{
  "code": 0,
  "msg": "成功",
  "data": {
    "total": 3,
    "page": 1,
    "size": 20,
    "items": [
      {
        "id": 501,
        "title": "知新楼晚霞",
        "locationId": 101,
        "locationName": "知新楼",
        "thumbnailUrl": "https://r2.example.com/thumb-501.jpg?X-Amz-Signature=...",
        "shotAt": "2026-08-15T18:30:00",
        "viewCount": 121,
        "likeCount": 12,
        "favoriteCount": 7
      }
    ]
  },
  "timestamp": 1786860000000
}
```

主要错误：通用字段校验错误 `400`、`15001` 搜索排序方式不正确。

### 10.3 发现首页

```http
GET /discovery/home?cityId=1
```

权限：公开。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---:|---|---|
| `cityId` | number | 否 | - | 传入时必须是启用城市；不传时聚合全部城市 |

当前固定返回最多 12 条精选媒体、12 条最新媒体、12 个热门标签、6 个校区分区且每区最多 6 条媒体。热门标签根据最多 200 条热门媒体统计；话题列表和话题中的 `mediaCount` 当前不随 `cityId` 过滤。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.cityId` | number | 是 | 当前筛选城市 ID；未筛选时为 `null` |
| `data.featured` | array&lt;object&gt; | 否 | 热门精选媒体摘要数组 |
| `data.latest` | array&lt;object&gt; | 否 | 最新媒体摘要数组 |
| `data.featured[].id`、`data.latest[].id`、`data.campuses[].media[].id` | number | 否 | 媒体 ID |
| `data.featured[].title`、`data.latest[].title`、`data.campuses[].media[].title` | string | 是 | 媒体标题 |
| `data.featured[].locationId`、`data.latest[].locationId`、`data.campuses[].media[].locationId` | number | 否 | 地点 ID |
| `data.featured[].locationName`、`data.latest[].locationName`、`data.campuses[].media[].locationName` | string | 是 | 地点名称 |
| `data.featured[].thumbnailUrl`、`data.latest[].thumbnailUrl`、`data.campuses[].media[].thumbnailUrl` | string | 否 | 缩略图预签名 URL |
| `data.featured[].shotAt`、`data.latest[].shotAt`、`data.campuses[].media[].shotAt` | string | 是 | 拍摄时间 |
| `data.featured[].viewCount`、`data.latest[].viewCount`、`data.campuses[].media[].viewCount` | number | 否 | 浏览次数 |
| `data.featured[].likeCount`、`data.latest[].likeCount`、`data.campuses[].media[].likeCount` | number | 否 | 点赞数 |
| `data.featured[].favoriteCount`、`data.latest[].favoriteCount`、`data.campuses[].media[].favoriteCount` | number | 否 | 收藏数 |
| `data.popularTags` | array&lt;object&gt; | 否 | 热门标签数组 |
| `data.popularTags[].name` | string | 否 | 标签名称 |
| `data.popularTags[].mediaCount` | number | 否 | 统计样本中包含该标签的媒体数 |
| `data.topics` | array&lt;object&gt; | 否 | 全局启用话题摘要数组 |
| `data.topics[].id` | number | 否 | 话题 ID |
| `data.topics[].name` | string | 否 | 话题名称 |
| `data.topics[].slug` | string | 是 | 话题短标识 |
| `data.topics[].description` | string | 是 | 话题描述 |
| `data.topics[].coverUrl` | string | 是 | 话题封面地址 |
| `data.topics[].mediaCount` | number | 否 | 话题下可见媒体数 |
| `data.campuses` | array&lt;object&gt; | 否 | 校区分区数组 |
| `data.campuses[].campusId` | number | 否 | 校区 ID |
| `data.campuses[].campusName` | string | 否 | 校区名称 |
| `data.campuses[].coverUrl` | string | 是 | 校区封面地址 |
| `data.campuses[].media` | array&lt;object&gt; | 否 | 校区热门媒体摘要数组 |

JSON 响应示例：

```json
{
  "code": 0,
  "msg": "成功",
  "data": {
    "cityId": 1,
    "featured": [
      {
        "id": 501,
        "title": "知新楼晚霞",
        "locationId": 101,
        "locationName": "知新楼",
        "thumbnailUrl": "https://r2.example.com/thumb-501.jpg?X-Amz-Signature=...",
        "shotAt": "2026-08-15T18:30:00",
        "viewCount": 121,
        "likeCount": 12,
        "favoriteCount": 7
      }
    ],
    "latest": [
      {
        "id": 502,
        "title": "校园清晨",
        "locationId": 102,
        "locationName": "中心校区南门",
        "thumbnailUrl": "https://r2.example.com/thumb-502.jpg?X-Amz-Signature=...",
        "shotAt": "2026-08-16T06:30:00",
        "viewCount": 30,
        "likeCount": 5,
        "favoriteCount": 2
      }
    ],
    "popularTags": [
      {
        "name": "建筑",
        "mediaCount": 28
      }
    ],
    "topics": [
      {
        "id": 201,
        "name": "山大四季",
        "slug": "sdu-seasons",
        "description": "记录校园四季",
        "coverUrl": "https://example.com/topics/seasons.jpg",
        "mediaCount": 36
      }
    ],
    "campuses": [
      {
        "campusId": 1,
        "campusName": "中心校区",
        "coverUrl": "https://example.com/campuses/central.jpg",
        "media": [
          {
            "id": 501,
            "title": "知新楼晚霞",
            "locationId": 101,
            "locationName": "知新楼",
            "thumbnailUrl": "https://r2.example.com/thumb-501.jpg?X-Amz-Signature=...",
            "shotAt": "2026-08-15T18:30:00",
            "viewCount": 121,
            "likeCount": 12,
            "favoriteCount": 7
          }
        ]
      }
    ]
  },
  "timestamp": 1786860000000
}
```

主要错误：`12000` 城市不存在或已停用。

### 10.4 话题列表

```http
GET /topics
```

权限：公开。只返回启用话题，按后台 `sortOrder`、`id` 升序排列。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data` | array&lt;object&gt; | 否 | 话题摘要数组；没有数据时为 `[]` |
| `data[].id` | number | 否 | 话题 ID |
| `data[].name` | string | 否 | 话题名称 |
| `data[].slug` | string | 是 | 话题短标识 |
| `data[].description` | string | 是 | 话题描述 |
| `data[].coverUrl` | string | 是 | 话题封面地址 |
| `data[].mediaCount` | number | 否 | 话题下可见媒体数 |
| `data[].favoriteCount` | number | 否 | 话题收藏数 |
| `data[].favorited` | boolean | 否 | 当前正式登录用户是否已收藏；未登录或非正式账号为 `false` |

JSON 响应示例：

```json
{
  "code": 0,
  "msg": "成功",
  "data": [
    {
      "id": 201,
      "name": "山大四季",
      "slug": "sdu-seasons",
      "description": "记录校园四季",
      "coverUrl": "https://example.com/topics/seasons.jpg",
      "mediaCount": 36,
      "favoriteCount": 23,
      "favorited": true
    }
  ],
  "timestamp": 1786860000000
}
```

### 10.5 话题详情

```http
GET /topics/{topicId}
```

权限：公开。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.id` | number | 否 | 话题 ID |
| `data.name` | string | 否 | 话题名称 |
| `data.slug` | string | 是 | 话题短标识 |
| `data.description` | string | 是 | 话题描述 |
| `data.coverUrl` | string | 是 | 话题封面地址 |
| `data.mediaCount` | number | 否 | 话题下可见媒体数 |
| `data.favoriteCount` | number | 否 | 话题收藏数 |
| `data.favorited` | boolean | 否 | 当前正式登录用户是否已收藏；未登录或非正式账号为 `false` |
| `data.createdAt` | string | 是 | 创建时间，ISO LocalDateTime |
| `data.updatedAt` | string | 是 | 更新时间，ISO LocalDateTime |

JSON 响应示例：

```json
{
  "code": 0,
  "msg": "成功",
  "data": {
    "id": 201,
    "name": "山大四季",
    "slug": "sdu-seasons",
    "description": "记录校园四季",
    "coverUrl": "https://example.com/topics/seasons.jpg",
    "mediaCount": 36,
    "favoriteCount": 23,
    "favorited": true,
    "createdAt": "2026-08-01T09:00:00",
    "updatedAt": "2026-08-15T10:00:00"
  },
  "timestamp": 1786860000000
}
```

主要错误：`15100` 话题不存在或已停用。

### 10.6 话题媒体

```http
GET /topics/{topicId}/media?page=1&size=20
```

权限：公开。

| 参数 | 类型 | 必填 | 默认值 | 实际处理 |
|---|---:|---:|---:|---|
| `topicId` | number | 是 | - | 正数话题 ID |
| `page` | number | 否 | `1` | 小于 1 按 1 |
| `size` | number | 否 | `20` | 小于 1 按 1，大于 50 按 50 |

媒体按话题关联的 `sortOrder` 升序，再按媒体创建时间、ID 降序排列。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.total` | number | 否 | 话题下可见媒体总数 |
| `data.page` | number | 否 | 后端规范化后的当前页码 |
| `data.size` | number | 否 | 后端规范化后的每页数量 |
| `data.items` | array&lt;object&gt; | 否 | 当前页媒体摘要数组 |
| `data.items[].id` | number | 否 | 媒体 ID |
| `data.items[].title` | string | 是 | 媒体标题 |
| `data.items[].locationId` | number | 否 | 地点 ID |
| `data.items[].locationName` | string | 是 | 地点名称 |
| `data.items[].thumbnailUrl` | string | 否 | 缩略图预签名 URL |
| `data.items[].shotAt` | string | 是 | 拍摄时间 |
| `data.items[].viewCount` | number | 否 | 浏览次数 |
| `data.items[].likeCount` | number | 否 | 点赞数 |
| `data.items[].favoriteCount` | number | 否 | 收藏数 |

JSON 响应示例：

```json
{
  "code": 0,
  "msg": "成功",
  "data": {
    "total": 36,
    "page": 1,
    "size": 20,
    "items": [
      {
        "id": 501,
        "title": "知新楼晚霞",
        "locationId": 101,
        "locationName": "知新楼",
        "thumbnailUrl": "https://r2.example.com/thumb-501.jpg?X-Amz-Signature=...",
        "shotAt": "2026-08-15T18:30:00",
        "viewCount": 121,
        "likeCount": 12,
        "favoriteCount": 7
      }
    ]
  },
  "timestamp": 1786860000000
}
```

主要错误：`15100` 话题不存在或已停用。

## 11. 个人收藏夹与批量收藏

本节接口均要求有效 access token。收藏夹与批量媒体收藏当前只检查登录状态；地点和话题收藏要求当前账号为统一认证正式用户。

收藏夹封面 URL 和收藏夹内容中的缩略图 URL 均为短期预签名地址。未手动设置封面时，后端会使用收藏夹中最近收藏的一条可见媒体作为封面。

### 11.1 收藏夹列表

```http
GET /favorite-folders
token: <accessToken>
```

权限：登录。首次查询时，如果当前用户没有默认收藏夹，后端会自动创建。结果按默认收藏夹优先、`sortOrder`、ID 升序排列。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data` | array&lt;object&gt; | 否 | 当前用户的收藏夹数组 |
| `data[].id` | number | 否 | 收藏夹 ID |
| `data[].name` | string | 否 | 收藏夹名称 |
| `data[].description` | string | 是 | 收藏夹描述 |
| `data[].coverMediaId` | number | 是 | 实际展示的封面媒体 ID；可能来自手动设置或自动回退 |
| `data[].coverUrl` | string | 是 | 封面媒体缩略图预签名 URL |
| `data[].itemCount` | number | 否 | 收藏夹中当前可见媒体数量 |
| `data[].isDefault` | boolean | 否 | 是否为系统默认收藏夹 |
| `data[].sortOrder` | number | 否 | 排序值 |
| `data[].createdAt` | string | 是 | 创建时间，ISO LocalDateTime |
| `data[].updatedAt` | string | 是 | 更新时间，ISO LocalDateTime |

JSON 响应示例：

```json
{
  "code": 0,
  "msg": "成功",
  "data": [
    {
      "id": 301,
      "name": "默认收藏夹",
      "description": "系统默认收藏夹",
      "coverMediaId": 501,
      "coverUrl": "https://r2.example.com/thumb-501.jpg?X-Amz-Signature=...",
      "itemCount": 8,
      "isDefault": true,
      "sortOrder": 0,
      "createdAt": "2026-08-16T09:00:00",
      "updatedAt": "2026-08-16T09:00:00"
    },
    {
      "id": 302,
      "name": "校园建筑",
      "description": "喜欢的校园建筑照片",
      "coverMediaId": null,
      "coverUrl": null,
      "itemCount": 0,
      "isDefault": false,
      "sortOrder": 10,
      "createdAt": "2026-08-16T10:00:00",
      "updatedAt": "2026-08-16T10:00:00"
    }
  ],
  "timestamp": 1786860000000
}
```

### 11.2 创建收藏夹

```http
POST /favorite-folders
token: <accessToken>
Content-Type: application/json
```

请求体字段：

| 字段 | JSON 类型 | 必填 | 默认值 | 约束/说明 |
|---|---|---:|---|---|
| `name` | string | 是 | - | 非空，最长 50 字符；保存前去除首尾空白 |
| `description` | string | 否 | `null` | 最长 255 字符；空白字符串保存为 `null` |
| `sortOrder` | number | 否 | `0` | 不小于 0 |

请求体示例：

```json
{
  "name": "校园建筑",
  "description": "喜欢的校园建筑照片",
  "sortOrder": 10
}
```

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.id` | number | 否 | 新收藏夹 ID |
| `data.name` | string | 否 | 收藏夹名称 |
| `data.description` | string | 是 | 收藏夹描述 |
| `data.coverMediaId` | number | 是 | 新收藏夹暂无封面，为 `null` |
| `data.coverUrl` | string | 是 | 新收藏夹暂无封面，为 `null` |
| `data.itemCount` | number | 否 | 新收藏夹为 `0` |
| `data.isDefault` | boolean | 否 | 用户创建的收藏夹为 `false` |
| `data.sortOrder` | number | 否 | 排序值 |
| `data.createdAt` | string | 是 | 创建时间 |
| `data.updatedAt` | string | 是 | 更新时间 |

JSON 响应示例：

```json
{
  "code": 0,
  "msg": "收藏夹创建成功",
  "data": {
    "id": 302,
    "name": "校园建筑",
    "description": "喜欢的校园建筑照片",
    "coverMediaId": null,
    "coverUrl": null,
    "itemCount": 0,
    "isDefault": false,
    "sortOrder": 10,
    "createdAt": "2026-08-16T10:00:00",
    "updatedAt": "2026-08-16T10:00:00"
  },
  "timestamp": 1786860000000
}
```

主要错误：通用字段校验错误 `400`、`13103` 已存在同名收藏夹。

### 11.3 修改收藏夹

```http
PATCH /favorite-folders/{folderId}
token: <accessToken>
Content-Type: application/json
```

路径参数 `folderId` 必须为正数。请求体至少需要提供一个非 `null` 字段：

| 字段 | JSON 类型 | 必填 | 省略/`null` 语义 | 约束/说明 |
|---|---|---:|---|---|
| `name` | string | 否 | 不修改 | 最长 50 字符；空白字符串返回 `400` |
| `description` | string | 否 | 不修改 | 最长 255 字符；传空字符串可清空描述 |
| `coverMediaId` | number | 否 | 不修改 | 正数；媒体必须可见且已经位于该收藏夹中 |
| `clearCover` | boolean | 否 | 不修改 | `true` 清除手动封面；不能与 `coverMediaId` 同时使用 |
| `sortOrder` | number | 否 | 不修改 | 不小于 0 |

默认收藏夹也允许修改名称、描述、封面和排序，只是不允许删除。

请求体示例：

```json
{
  "description": "校园建筑与地标",
  "coverMediaId": 501,
  "sortOrder": 5
}
```

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.id` | number | 否 | 收藏夹 ID |
| `data.name` | string | 否 | 修改后的名称 |
| `data.description` | string | 是 | 修改后的描述 |
| `data.coverMediaId` | number | 是 | 实际展示的封面媒体 ID |
| `data.coverUrl` | string | 是 | 封面缩略图预签名 URL |
| `data.itemCount` | number | 否 | 当前可见媒体数量 |
| `data.isDefault` | boolean | 否 | 是否默认收藏夹 |
| `data.sortOrder` | number | 否 | 修改后的排序值 |
| `data.createdAt` | string | 是 | 创建时间 |
| `data.updatedAt` | string | 是 | 本次更新时间 |

JSON 响应示例：

```json
{
  "code": 0,
  "msg": "收藏夹修改成功",
  "data": {
    "id": 302,
    "name": "校园建筑",
    "description": "校园建筑与地标",
    "coverMediaId": 501,
    "coverUrl": "https://r2.example.com/thumb-501.jpg?X-Amz-Signature=...",
    "itemCount": 6,
    "isDefault": false,
    "sortOrder": 5,
    "createdAt": "2026-08-16T10:00:00",
    "updatedAt": "2026-08-16T11:00:00"
  },
  "timestamp": 1786860000000
}
```

主要错误：通用 `400`、`13000` 媒体不存在或不可见、`13100` 收藏夹不存在或无权访问、`13103` 同名收藏夹、`13106` 封面媒体不在收藏夹中。

### 11.4 删除收藏夹

```http
DELETE /favorite-folders/{folderId}
token: <accessToken>
```

权限：登录。路径参数 `folderId` 必须为正数。默认收藏夹不能删除；删除普通收藏夹会同时取消其中的全部收藏关系，并同步减少相关媒体收藏数。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 说明 |
|---|---|---|
| `data` | null | 该接口不返回业务数据 |

JSON 响应示例：

```json
{
  "code": 0,
  "msg": "成功",
  "data": null,
  "timestamp": 1786860000000
}
```

主要错误：`13100` 收藏夹不存在或无权访问、`13104` 默认收藏夹不能删除，以及路径参数校验错误。

### 11.5 收藏夹内容

```http
GET /favorite-folders/{folderId}/items?page=1&size=20
token: <accessToken>
```

权限：登录。只返回当前仍可见的媒体，按收藏时间、收藏关系 ID 降序排列。

| 参数 | 类型 | 必填 | 默认值 | 实际处理 |
|---|---:|---:|---:|---|
| `folderId` | number | 是 | - | 正数，且收藏夹必须属于当前用户 |
| `page` | number | 否 | `1` | 必须为正数 |
| `size` | number | 否 | `20` | 必须为正数；大于 50 按 50 |

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.total` | number | 否 | 收藏夹中可见媒体总数 |
| `data.page` | number | 否 | 当前页码 |
| `data.size` | number | 否 | 后端规范化后的每页数量 |
| `data.items` | array&lt;object&gt; | 否 | 当前页媒体摘要数组 |
| `data.items[].id` | number | 否 | 媒体 ID |
| `data.items[].title` | string | 是 | 媒体标题 |
| `data.items[].locationId` | number | 否 | 地点 ID |
| `data.items[].locationName` | string | 是 | 地点名称 |
| `data.items[].thumbnailUrl` | string | 否 | 缩略图预签名 URL |
| `data.items[].shotAt` | string | 是 | 拍摄时间 |
| `data.items[].viewCount` | number | 否 | 浏览次数 |
| `data.items[].likeCount` | number | 否 | 点赞数 |
| `data.items[].favoriteCount` | number | 否 | 收藏数 |

JSON 响应示例：

```json
{
  "code": 0,
  "msg": "成功",
  "data": {
    "total": 6,
    "page": 1,
    "size": 20,
    "items": [
      {
        "id": 501,
        "title": "知新楼晚霞",
        "locationId": 101,
        "locationName": "知新楼",
        "thumbnailUrl": "https://r2.example.com/thumb-501.jpg?X-Amz-Signature=...",
        "shotAt": "2026-08-15T18:30:00",
        "viewCount": 121,
        "likeCount": 12,
        "favoriteCount": 7
      }
    ]
  },
  "timestamp": 1786860000000
}
```

主要错误：`13100` 收藏夹不存在或无权访问，以及路径/分页参数校验错误。

### 11.6 收藏地点

```http
POST /locations/{locationId}/favorites
token: <accessToken>
```

权限：登录，且服务层要求统一认证正式用户。路径参数 `locationId` 必须为正数，地点及其所属校区、城市必须启用。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.locationId` | number | 否 | 地点 ID |
| `data.favoriteCount` | number | 否 | 收藏后的地点收藏数 |
| `data.favorited` | boolean | 否 | 收藏成功后为 `true` |

主要错误：`12200` 地点不存在或已停用、`14000` 仅统一认证正式用户可用、`13107` 已收藏过该内容。

### 11.7 取消收藏地点

```http
DELETE /locations/{locationId}/favorites
token: <accessToken>
```

权限：登录，且服务层要求统一认证正式用户。取消当前用户对指定地点的收藏。

响应字段同收藏地点，取消成功后 `favorited` 为 `false`。

主要错误：`12200` 地点不存在或已停用、`14000` 仅统一认证正式用户可用、`13108` 尚未收藏该内容。

### 11.8 收藏话题

```http
POST /topics/{topicId}/favorites
token: <accessToken>
```

权限：登录，且服务层要求统一认证正式用户。路径参数 `topicId` 必须为正数，话题必须启用。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.topicId` | number | 否 | 话题 ID |
| `data.favoriteCount` | number | 否 | 收藏后的话题收藏数 |
| `data.favorited` | boolean | 否 | 收藏成功后为 `true` |

主要错误：`15100` 话题不存在或已停用、`14000` 仅统一认证正式用户可用、`13107` 已收藏过该内容。

### 11.9 取消收藏话题

```http
DELETE /topics/{topicId}/favorites
token: <accessToken>
```

权限：登录，且服务层要求统一认证正式用户。取消当前用户对指定话题的收藏。

响应字段同收藏话题，取消成功后 `favorited` 为 `false`。

主要错误：`15100` 话题不存在或已停用、`14000` 仅统一认证正式用户可用、`13108` 尚未收藏该内容。

### 11.10 收藏地点列表

```http
GET /favorites/locations?page=1&size=20
token: <accessToken>
```

权限：登录，且服务层要求统一认证正式用户。只返回当前仍启用且所属校区、城市也启用的地点，按收藏时间倒序排列。

响应为 `PageResult<LocationListVO>`，地点条目包含 `favoriteCount` 和 `favorited`，其中 `favorited` 恒为 `true`。

### 11.11 收藏话题列表

```http
GET /favorites/topics?page=1&size=20
token: <accessToken>
```

权限：登录，且服务层要求统一认证正式用户。只返回当前仍启用的话题，按收藏时间倒序排列。

响应为 `PageResult<TopicSummaryVO>`，话题条目包含 `favoriteCount` 和 `favorited`，其中 `favorited` 恒为 `true`。

### 11.12 批量管理收藏

```http
POST /favorites/batch
token: <accessToken>
Content-Type: application/json
```

权限：登录。

请求体字段：

| 字段 | JSON 类型 | 必填 | 约束/说明 |
|---|---|---:|---|
| `action` | string | 是 | `ADD`、`REMOVE` 或 `MOVE`，区分大小写 |
| `folderId` | number | 视操作而定 | `ADD` 时为目标收藏夹，省略则使用默认收藏夹；`MOVE` 时为源收藏夹；`REMOVE` 时当前实现忽略该字段 |
| `targetFolderId` | number | `MOVE` 时是 | `MOVE` 的目标收藏夹；其他操作忽略 |
| `mediaIds` | array&lt;number&gt; | 是 | 非空，单次最多 100 个；每项必须为正数；后端会去重并保留首次出现顺序 |

操作语义：

- `ADD`：所有媒体必须存在且可见；已收藏的媒体不会重复插入，`affectedCount` 不增加。
- `REMOVE`：按媒体 ID 取消当前用户的收藏，不校验或限定收藏夹；未收藏的 ID 静默跳过。
- `MOVE`：源、目标收藏夹必须属于当前用户且不能相同；不在源收藏夹中的媒体静默跳过。
- 任一操作发生业务异常时，整个批次事务回滚。

请求体示例：

```json
{
  "action": "MOVE",
  "folderId": 301,
  "targetFolderId": 302,
  "mediaIds": [501, 502, 502]
}
```

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.action` | string | 否 | 实际执行的 `ADD`、`REMOVE` 或 `MOVE` |
| `data.requestedCount` | number | 否 | 去重后的媒体 ID 数量 |
| `data.affectedCount` | number | 否 | 实际新增、删除或移动的收藏关系数 |
| `data.folderId` | number | 是 | `ADD` 的实际目标或 `MOVE` 的目标收藏夹 ID；`REMOVE` 为 `null` |

JSON 响应示例：

```json
{
  "code": 0,
  "msg": "批量收藏操作完成",
  "data": {
    "action": "MOVE",
    "requestedCount": 2,
    "affectedCount": 2,
    "folderId": 302
  },
  "timestamp": 1786860000000
}
```

主要错误：通用字段校验错误 `400`、`13000` 媒体不存在或不可见、`13100` 收藏夹不存在或无权访问、`13105` 批量移动缺少源或目标收藏夹。

## 12. 个人中心与浏览足迹

本节接口均要求有效 access token。个人资料、头像和密码接口要求当前用户存在、未删除且状态为 `NORMAL`；浏览足迹接口还要求用户是具有 `casId` 的统一认证正式用户。

个人资料响应中的 `avatarUrl` 是有效期为 10 分钟的预签名 URL。客户端不应长期缓存，需要时可重新请求个人资料。

### 12.1 查询个人资料

```http
GET /users/me
token: <accessToken>
```

权限：登录。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.id` | number | 否 | 当前用户 ID |
| `data.phoneMasked` | string | 是 | 脱敏手机号；常规号码保留前 3 位和后 4 位，中间显示 `****` |
| `data.casId` | string | 是 | 统一认证 CAS ID；历史非正式账号可能为空 |
| `data.name` | string | 是 | 统一认证姓名 |
| `data.nickname` | string | 是 | 用户昵称 |
| `data.avatarUrl` | string | 是 | 头像预签名 URL；未设置头像时为 `null` |
| `data.bio` | string | 是 | 个人简介 |
| `data.role` | string | 否 | 用户角色：`USER` 或 `ADMIN` |
| `data.status` | string | 否 | 用户状态；当前接口成功时为 `NORMAL` |
| `data.allowUpload` | boolean | 否 | 是否允许投稿 |
| `data.allowDownload` | boolean | 否 | 是否允许下载原图 |
| `data.passwordConfigured` | boolean | 否 | 当前账号是否已经配置密码 |
| `data.lastLoginAt` | string | 是 | 最近登录时间，ISO LocalDateTime |
| `data.createdAt` | string | 是 | 账号创建时间，ISO LocalDateTime |
| `data.updatedAt` | string | 是 | 账号更新时间，ISO LocalDateTime |

JSON 响应示例：

```json
{
  "code": 0,
  "msg": "成功",
  "data": {
    "id": 1001,
    "phoneMasked": "138****5678",
    "casId": "202300000000",
    "name": "张三",
    "nickname": "山大摄影师",
    "avatarUrl": "https://r2.example.com/avatars/1001/avatar.jpg?X-Amz-Signature=...",
    "bio": "记录校园里的光影。",
    "role": "USER",
    "status": "NORMAL",
    "allowUpload": true,
    "allowDownload": true,
    "passwordConfigured": true,
    "lastLoginAt": "2026-08-17T20:30:00",
    "createdAt": "2026-08-01T09:00:00",
    "updatedAt": "2026-08-17T20:35:00"
  },
  "timestamp": 1786968000000
}
```

主要错误：`10102` 用户不存在、`10104` 账户已被冻结或停用。

### 12.2 修改个人资料

```http
PATCH /users/me
token: <accessToken>
Content-Type: application/json
```

权限：登录。

请求体字段：

| 字段 | JSON 类型 | 必填 | 约束/说明 |
|---|---|---:|---|
| `nickname` | string | 否 | 去除首尾空白后长度为 2～30；传 `null` 或省略表示不修改，不能清空昵称 |
| `bio` | string | 否 | 最长 500 字符；去除首尾空白后为空字符串时保存为 `null`；传 `null` 或省略表示不修改 |

至少需要提供一个非 `null` 字段，否则返回 `16000`。

请求体示例：

```json
{
  "nickname": "山大光影",
  "bio": "记录校园建筑与四季。"
}
```

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.id` | number | 否 | 当前用户 ID |
| `data.phoneMasked` | string | 是 | 脱敏手机号 |
| `data.casId` | string | 是 | 统一认证 CAS ID |
| `data.name` | string | 是 | 统一认证姓名 |
| `data.nickname` | string | 是 | 修改后的昵称 |
| `data.avatarUrl` | string | 是 | 当前头像预签名 URL |
| `data.bio` | string | 是 | 修改后的个人简介 |
| `data.role` | string | 否 | `USER` 或 `ADMIN` |
| `data.status` | string | 否 | 当前接口成功时为 `NORMAL` |
| `data.allowUpload` | boolean | 否 | 是否允许投稿 |
| `data.allowDownload` | boolean | 否 | 是否允许下载原图 |
| `data.passwordConfigured` | boolean | 否 | 是否已经配置密码 |
| `data.lastLoginAt` | string | 是 | 最近登录时间 |
| `data.createdAt` | string | 是 | 账号创建时间 |
| `data.updatedAt` | string | 是 | 本次修改后的更新时间 |

JSON 响应示例：

```json
{
  "code": 0,
  "msg": "个人资料修改成功",
  "data": {
    "id": 1001,
    "phoneMasked": "138****5678",
    "casId": "202300000000",
    "name": "张三",
    "nickname": "山大光影",
    "avatarUrl": "https://r2.example.com/avatars/1001/avatar.jpg?X-Amz-Signature=...",
    "bio": "记录校园建筑与四季。",
    "role": "USER",
    "status": "NORMAL",
    "allowUpload": true,
    "allowDownload": true,
    "passwordConfigured": true,
    "lastLoginAt": "2026-08-17T20:30:00",
    "createdAt": "2026-08-01T09:00:00",
    "updatedAt": "2026-08-17T21:00:00"
  },
  "timestamp": 1786968000000
}
```

主要错误：通用字段校验错误 `400`、`16000` 没有可修改字段、`16001` 昵称去除首尾空白后长度不合法。

### 12.3 上传或替换头像

```http
PUT /users/me/avatar
token: <accessToken>
Content-Type: multipart/form-data
```

权限：登录。

Multipart 字段：

| 字段 | 类型 | 必填 | 约束/说明 |
|---|---|---:|---|
| `file` | file | 是 | 非空；最大 5 MiB；仅支持 PNG、JPEG、WebP；声明的 MIME 必须与文件魔数一致 |

上传成功后替换原头像；数据库事务提交后，后端会删除旧头像文件。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.id` | number | 否 | 当前用户 ID |
| `data.phoneMasked` | string | 是 | 脱敏手机号 |
| `data.casId` | string | 是 | 统一认证 CAS ID |
| `data.name` | string | 是 | 统一认证姓名 |
| `data.nickname` | string | 是 | 用户昵称 |
| `data.avatarUrl` | string | 否 | 新头像的预签名 URL |
| `data.bio` | string | 是 | 个人简介 |
| `data.role` | string | 否 | `USER` 或 `ADMIN` |
| `data.status` | string | 否 | 当前接口成功时为 `NORMAL` |
| `data.allowUpload` | boolean | 否 | 是否允许投稿 |
| `data.allowDownload` | boolean | 否 | 是否允许下载原图 |
| `data.passwordConfigured` | boolean | 否 | 是否已经配置密码 |
| `data.lastLoginAt` | string | 是 | 最近登录时间 |
| `data.createdAt` | string | 是 | 账号创建时间 |
| `data.updatedAt` | string | 是 | 本次修改后的更新时间 |

JSON 响应示例：

```json
{
  "code": 0,
  "msg": "头像修改成功",
  "data": {
    "id": 1001,
    "phoneMasked": "138****5678",
    "casId": "202300000000",
    "name": "张三",
    "nickname": "山大光影",
    "avatarUrl": "https://r2.example.com/avatars/1001/new-avatar.webp?X-Amz-Signature=...",
    "bio": "记录校园建筑与四季。",
    "role": "USER",
    "status": "NORMAL",
    "allowUpload": true,
    "allowDownload": true,
    "passwordConfigured": true,
    "lastLoginAt": "2026-08-17T20:30:00",
    "createdAt": "2026-08-01T09:00:00",
    "updatedAt": "2026-08-17T21:10:00"
  },
  "timestamp": 1786968000000
}
```

主要错误：`16200` 头像文件为空、`16201` 头像文件超过 5 MiB、`16202` 文件类型或 MIME/文件内容不匹配，以及 multipart 请求体超过全局限制时的 `19000`。

### 12.4 删除头像

```http
DELETE /users/me/avatar
token: <accessToken>
```

权限：登录。未设置头像时重复调用也返回成功。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.id` | number | 否 | 当前用户 ID |
| `data.phoneMasked` | string | 是 | 脱敏手机号 |
| `data.casId` | string | 是 | 统一认证 CAS ID |
| `data.name` | string | 是 | 统一认证姓名 |
| `data.nickname` | string | 是 | 用户昵称 |
| `data.avatarUrl` | string | 是 | 删除成功后为 `null` |
| `data.bio` | string | 是 | 个人简介 |
| `data.role` | string | 否 | `USER` 或 `ADMIN` |
| `data.status` | string | 否 | 当前接口成功时为 `NORMAL` |
| `data.allowUpload` | boolean | 否 | 是否允许投稿 |
| `data.allowDownload` | boolean | 否 | 是否允许下载原图 |
| `data.passwordConfigured` | boolean | 否 | 是否已经配置密码 |
| `data.lastLoginAt` | string | 是 | 最近登录时间 |
| `data.createdAt` | string | 是 | 账号创建时间 |
| `data.updatedAt` | string | 是 | 有头像被删除时更新；原本无头像时保持原值 |

JSON 响应示例：

```json
{
  "code": 0,
  "msg": "头像删除成功",
  "data": {
    "id": 1001,
    "phoneMasked": "138****5678",
    "casId": "202300000000",
    "name": "张三",
    "nickname": "山大光影",
    "avatarUrl": null,
    "bio": "记录校园建筑与四季。",
    "role": "USER",
    "status": "NORMAL",
    "allowUpload": true,
    "allowDownload": true,
    "passwordConfigured": true,
    "lastLoginAt": "2026-08-17T20:30:00",
    "createdAt": "2026-08-01T09:00:00",
    "updatedAt": "2026-08-17T21:15:00"
  },
  "timestamp": 1786968000000
}
```

主要错误：`10102` 用户不存在、`10104` 账户已被冻结或停用。

### 12.5 修改密码

```http
PUT /users/me/password
token: <accessToken>
Content-Type: application/json
```

权限：登录。该接口只能修改已经配置过的密码，不能为无密码账号首次设置密码。

请求体字段：

| 字段 | JSON 类型 | 必填 | 约束/说明 |
|---|---|---:|---|
| `currentPassword` | string | 是 | 非空，最多 64 个字符；UTF-8 编码超过 72 字节时按当前密码错误处理 |
| `newPassword` | string | 是 | 非空，8～64 个字符且 UTF-8 编码不超过 72 字节；不能与当前密码相同 |
| `confirmPassword` | string | 是 | 非空，最多 64 个字符；必须与 `newPassword` 完全相同 |

请求体示例：

```json
{
  "currentPassword": "old-password",
  "newPassword": "new-password-2026",
  "confirmPassword": "new-password-2026"
}
```

修改成功后，后端会提升 token 版本并删除当前用户全部 refresh token。当前设备及其他设备已有的 access token 和 refresh token 均不可继续使用，客户端收到成功响应后应立即清除本地凭证并重新登录。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data` | null | 是 | 固定为 `null` |

JSON 响应示例：

```json
{
  "code": 0,
  "msg": "密码修改成功，现有登录凭据已失效",
  "data": null,
  "timestamp": 1786968000000
}
```

主要错误：通用字段校验错误 `400`、`16100` 未配置密码、`16101` 当前密码错误、`16102` 两次新密码不一致、`16103` 新旧密码相同、`16104` 新密码编码后超过 72 字节。

### 12.6 分页查询浏览足迹

```http
GET /users/me/history?page=1&size=20
token: <accessToken>
```

权限：登录且必须是统一认证正式用户。

Query 参数：

| 参数 | 类型 | 必填 | 默认值 | 约束/说明 |
|---|---|---:|---:|---|
| `page` | number | 否 | `1` | 必须为正整数 |
| `size` | number | 否 | `20` | 必须为正整数；大于 50 时按 50 返回 |

只统计媒体自身 `status = 1` 的足迹，按 `lastViewedAt`、媒体 ID 降序排列。请求超出最后一页时返回原 `total` 和空 `items`。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.total` | number | 否 | 当前可见浏览足迹总数 |
| `data.page` | number | 否 | 后端规范化后的当前页码 |
| `data.size` | number | 否 | 后端规范化后的每页数量，最大为 50 |
| `data.items` | array&lt;object&gt; | 否 | 当前页浏览足迹数组；没有数据时为 `[]` |
| `data.items[].media` | object | 否 | 媒体摘要对象 |
| `data.items[].media.id` | number | 否 | 媒体 ID |
| `data.items[].media.title` | string | 是 | 媒体标题 |
| `data.items[].media.locationId` | number | 否 | 地点 ID |
| `data.items[].media.locationName` | string | 是 | 地点名称 |
| `data.items[].media.thumbnailUrl` | string | 否 | 缩略图预签名 URL |
| `data.items[].media.shotAt` | string | 是 | 拍摄时间，ISO LocalDateTime |
| `data.items[].media.viewCount` | number | 否 | 媒体总浏览次数 |
| `data.items[].media.likeCount` | number | 否 | 媒体点赞数 |
| `data.items[].media.favoriteCount` | number | 否 | 媒体收藏数 |
| `data.items[].viewCount` | number | 否 | 当前用户浏览该媒体的累计次数 |
| `data.items[].lastViewedAt` | string | 否 | 当前用户最近一次浏览时间，ISO LocalDateTime |

JSON 响应示例：

```json
{
  "code": 0,
  "msg": "成功",
  "data": {
    "total": 12,
    "page": 1,
    "size": 20,
    "items": [
      {
        "media": {
          "id": 501,
          "title": "知新楼晚霞",
          "locationId": 101,
          "locationName": "知新楼",
          "thumbnailUrl": "https://r2.example.com/thumb-501.jpg?X-Amz-Signature=...",
          "shotAt": "2026-08-15T18:30:00",
          "viewCount": 121,
          "likeCount": 12,
          "favoriteCount": 7
        },
        "viewCount": 4,
        "lastViewedAt": "2026-08-17T20:40:00"
      }
    ]
  },
  "timestamp": 1786968000000
}
```

主要错误：通用参数校验错误 `400`、`14000` 仅统一认证正式用户可用。

### 12.7 清空浏览足迹

```http
DELETE /users/me/history
token: <accessToken>
```

权限：登录且必须是统一认证正式用户。没有足迹时重复调用也返回成功。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data` | null | 是 | 固定为 `null` |

JSON 响应示例：

```json
{
  "code": 0,
  "msg": "浏览足迹已清空",
  "data": null,
  "timestamp": 1786968000000
}
```

主要错误：`14000` 仅统一认证正式用户可用。

### 12.8 删除单条浏览足迹

```http
DELETE /users/me/history/{mediaId}
token: <accessToken>
```

权限：登录且必须是统一认证正式用户。

| 参数 | 位置 | 类型 | 必填 | 说明 |
|---|---|---:|---:|---|
| `mediaId` | Path | number | 是 | 正数媒体 ID |

如果指定媒体没有对应浏览足迹，或媒体记录已经不存在，接口仍返回成功。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data` | null | 是 | 固定为 `null` |

JSON 响应示例：

```json
{
  "code": 0,
  "msg": "浏览足迹已删除",
  "data": null,
  "timestamp": 1786968000000
}
```

主要错误：通用路径参数校验错误 `400`、`14000` 仅统一认证正式用户可用。

## 13. 公告与公告管理

### 13.1 公告列表

```http
GET /announcements?page=1&size=20
```

权限：公开。只返回状态为 `PUBLISHED` 且发布时间不晚于当前时间的公告；按置顶优先、`sortOrder` 升序、发布时间和 ID 降序排列。

| 参数 | 类型 | 必填 | 默认值 | 约束 |
|---|---|---:|---:|---|
| `page` | number | 否 | `1` | `1～10000` |
| `size` | number | 否 | `20` | `1～50` |

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.total`、`page`、`size` | number | 否 | 分页信息 |
| `data.items` | array&lt;object&gt; | 否 | 公告摘要数组 |
| `data.items[].id` | number | 否 | 公告 ID |
| `data.items[].title` | string | 否 | 标题 |
| `data.items[].summary` | string | 是 | 摘要 |
| `data.items[].isPinned` | boolean | 否 | 是否置顶 |
| `data.items[].publishedAt` | string | 否 | 发布时间 |

```json
{"code":0,"msg":"成功","data":{"total":2,"page":1,"size":20,"items":[{"id":901,"title":"校园影像征集活动","summary":"欢迎提交校园四季影像。","isPinned":true,"publishedAt":"2026-08-22T08:00:00"}]},"timestamp":1787392800000}
```

主要错误：通用分页参数错误 `400`。

### 13.2 公告详情

```http
GET /announcements/{announcementId}
```

权限：公开。只有已经到达发布时间的 `PUBLISHED` 公告可见。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.id` | number | 否 | 公告 ID |
| `data.title` | string | 否 | 标题 |
| `data.summary` | string | 是 | 摘要 |
| `data.content` | string | 否 | 公告正文；后端不做 Markdown/HTML 转换 |
| `data.isPinned` | boolean | 否 | 是否置顶 |
| `data.publishedAt` | string | 否 | 发布时间 |
| `data.updatedAt` | string | 是 | 更新时间 |

```json
{"code":0,"msg":"成功","data":{"id":901,"title":"校园影像征集活动","summary":"欢迎提交校园四季影像。","content":"活动详情正文……","isPinned":true,"publishedAt":"2026-08-22T08:00:00","updatedAt":"2026-08-22T08:00:00"},"timestamp":1787392800000}
```

主要错误：`17000` 公告不存在、未发布、已下线或发布时间未到。

### 13.3 管理员公告列表

```http
GET /admin/announcements?status=DRAFT&keyword=征集&page=1&size=20
token: <管理员 accessToken>
```

| 参数 | 类型 | 必填 | 默认值 | 约束/说明 |
|---|---|---:|---:|---|
| `status` | string | 否 | - | `DRAFT`、`PUBLISHED`、`OFFLINE`，区分大小写 |
| `keyword` | string | 否 | - | 最长 100；模糊匹配标题和摘要 |
| `page` | number | 否 | `1` | `1～10000` |
| `size` | number | 否 | `20` | `1～50` |

返回所有状态，按更新时间、ID 降序排列。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.total`、`page`、`size` | number | 否 | 分页信息 |
| `data.items` | array&lt;object&gt; | 否 | 管理公告数组 |
| `data.items[].id` | number | 否 | 公告 ID |
| `data.items[].title`、`content` | string | 否 | 标题、正文 |
| `data.items[].summary` | string | 是 | 摘要 |
| `data.items[].status` | string | 否 | `DRAFT`、`PUBLISHED`、`OFFLINE` |
| `data.items[].isPinned` | boolean | 否 | 是否置顶 |
| `data.items[].sortOrder` | number | 否 | 排序值 |
| `data.items[].publishedAt` | string | 是 | 首次或最近一次发布时间 |
| `data.items[].createdBy`、`updatedBy` | number | 是 | 创建、更新管理员 ID |
| `data.items[].createdAt`、`updatedAt` | string | 是 | 创建、更新时间 |

```json
{"code":0,"msg":"成功","data":{"total":1,"page":1,"size":20,"items":[{"id":901,"title":"校园影像征集活动","summary":"欢迎提交校园四季影像。","content":"活动详情正文……","status":"DRAFT","isPinned":true,"sortOrder":0,"publishedAt":null,"createdBy":20001,"updatedBy":20001,"createdAt":"2026-08-22T08:00:00","updatedAt":"2026-08-22T08:00:00"}]},"timestamp":1787392800000}
```

### 13.4 创建公告

```http
POST /admin/announcements
token: <管理员 accessToken>
Content-Type: application/json
```

| 字段 | JSON 类型 | 必填 | 约束/说明 |
|---|---|---:|---|
| `title` | string | 是 | 非空，最长 200，保存时去除首尾空白 |
| `summary` | string | 否 | 最长 500；空白保存为 `null` |
| `content` | string | 是 | 非空，最长 50000；正文按原值保存 |
| `isPinned` | boolean | 否 | 默认 `false` |
| `sortOrder` | number | 否 | 非负，默认 `0` |

新公告固定创建为 `DRAFT`，不能在创建请求中直接发布。

```json
{"title":"校园影像征集活动","summary":"欢迎提交校园四季影像。","content":"活动详情正文……","isPinned":true,"sortOrder":0}
```

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.id` | number | 否 | 新公告 ID |
| `data.title`、`content` | string | 否 | 标题、正文 |
| `data.summary` | string | 是 | 摘要 |
| `data.status` | string | 否 | 固定为 `DRAFT` |
| `data.isPinned` | boolean | 否 | 是否置顶 |
| `data.sortOrder` | number | 否 | 排序值 |
| `data.publishedAt` | string | 是 | 创建时为 `null` |
| `data.createdBy`、`updatedBy` | number | 否 | 当前管理员 ID |
| `data.createdAt`、`updatedAt` | string | 否 | 创建、更新时间 |

```json
{"code":0,"msg":"公告创建成功","data":{"id":901,"title":"校园影像征集活动","summary":"欢迎提交校园四季影像。","content":"活动详情正文……","status":"DRAFT","isPinned":true,"sortOrder":0,"publishedAt":null,"createdBy":20001,"updatedBy":20001,"createdAt":"2026-08-22T08:00:00","updatedAt":"2026-08-22T08:00:00"},"timestamp":1787392800000}
```

### 13.5 修改公告

```http
PATCH /admin/announcements/{announcementId}
token: <管理员 accessToken>
Content-Type: application/json
```

请求字段与 13.4 相同但全部可选；至少提供一个非 `null` 字段。`summary` 传空白可清为 `null`，标题和正文不能清空。该接口不修改公告状态。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.id`、`sortOrder` | number | 否 | 公告 ID、排序值 |
| `data.title`、`content` | string | 否 | 更新后的标题、正文 |
| `data.summary` | string | 是 | 摘要 |
| `data.status` | string | 否 | 当前状态 |
| `data.isPinned` | boolean | 否 | 是否置顶 |
| `data.publishedAt` | string | 是 | 发布时间 |
| `data.createdBy`、`updatedBy` | number | 是 | 操作者 ID |
| `data.createdAt`、`updatedAt` | string | 是 | 时间字段 |

JSON 响应示例：

```json
{"code":0,"msg":"公告修改成功","data":{"id":901,"title":"校园影像征集活动（更新）","summary":"欢迎提交校园四季影像。","content":"更新后的活动详情正文……","status":"DRAFT","isPinned":true,"sortOrder":0,"publishedAt":null,"createdBy":20001,"updatedBy":20001,"createdAt":"2026-08-22T08:00:00","updatedAt":"2026-08-22T11:50:00"},"timestamp":1787392800000}
```

主要错误：`17000` 公告不存在、`17002` 空更新，以及通用字段错误 `400`。

### 13.6 发布或下线公告

```http
POST /admin/announcements/{announcementId}/status
token: <管理员 accessToken>
Content-Type: application/json

{"status":"PUBLISHED"}
```

`status` 必填，接口只接受 `PUBLISHED` 或 `OFFLINE`：草稿和已下线公告可以发布，发布会把 `publishedAt` 更新为当前时间；只有已发布公告可以下线。重复请求当前状态按成功返回。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.id`、`sortOrder` | number | 否 | 公告 ID、排序值 |
| `data.title`、`content` | string | 否 | 标题、正文 |
| `data.summary` | string | 是 | 摘要 |
| `data.status` | string | 否 | 变更后的 `PUBLISHED` 或 `OFFLINE` |
| `data.isPinned` | boolean | 否 | 是否置顶 |
| `data.publishedAt` | string | 是 | 发布时间；发布成功后非空 |
| `data.createdBy`、`updatedBy` | number | 是 | 操作者 ID |
| `data.createdAt`、`updatedAt` | string | 是 | 时间字段 |

```json
{"code":0,"msg":"公告发布成功","data":{"id":901,"title":"校园影像征集活动","summary":"欢迎提交校园四季影像。","content":"活动详情正文……","status":"PUBLISHED","isPinned":true,"sortOrder":0,"publishedAt":"2026-08-22T12:00:00","createdBy":20001,"updatedBy":20001,"createdAt":"2026-08-22T08:00:00","updatedAt":"2026-08-22T12:00:00"},"timestamp":1787392800000}
```

下线成功文案为“公告下线成功”。主要错误：`17000` 公告不存在、`17001` 状态转换不允许。

## 14. 举报与举报管理

举报功能支持举报媒体和用户。用户先查询可用理由，再通过对应目标类型的专用接口提交举报；管理员通过列表和详情取得最新 `version`，最后提交处理决定及可选资源处置动作。

### 14.1 举报状态、处理决定与处置动作

举报状态：

| JSON 值 | 含义 | 是否为进行中状态 |
|---|---|---:|
| `PENDING` | 待处理；新举报的初始状态 | 是 |
| `PROCESSING` | 处理中 | 是 |
| `CONFIRMED` | 举报成立，处理完成 | 否 |
| `DISMISSED` | 举报不成立，处理完成 | 否 |
| `CLOSED` | 举报关闭，处理完成 | 否 |

处理决定与终态：

| `decision` | 处理后状态 | 约束 |
|---|---|---|
| `CONFIRM` | `CONFIRMED` | 必须填写处理理由，并至少提供一个处置动作 |
| `DISMISS` | `DISMISSED` | 不能执行 `NO_ACTION` 以外的资源处置 |
| `CLOSE` | `CLOSED` | 不能执行 `NO_ACTION` 以外的资源处置 |

主要状态流转：

```text
PENDING / PROCESSING ──CONFIRM──> CONFIRMED
        │
        ├─────────────DISMISS───> DISMISSED
        │
        └─────────────CLOSE─────> CLOSED
```

当前没有把举报从 `PENDING` 修改为 `PROCESSING` 的接口；该状态已在数据库、查询条件和处理逻辑中预留。处理决定只能作用于 `PENDING` 或 `PROCESSING`，终态举报不能再次处理。

处置动作：

| `type` | 目标 | 行为和约束 |
|---|---|---|
| `HIDE_MEDIA` | 被举报媒体 | 隐藏媒体；已经隐藏时按成功返回 |
| `RESTORE_MEDIA` | 被举报媒体 | 恢复媒体；已经可见时按成功返回 |
| `FREEZE_USER` | 用户举报的目标用户，或媒体举报的上传者 | 必须提供未来的 `frozenUntil` 和非空 `reason` |
| `NO_ACTION` | 举报目标 | 只记录处理结论，不修改媒体或用户状态 |

同一次请求中的动作类型不能重复；`NO_ACTION` 不能和其他动作并存；`HIDE_MEDIA` 与 `RESTORE_MEDIA` 不能同时出现。第一版不支持举报证据附件。

### 14.2 查询举报理由

```http
GET /reports/reasons
```

权限：公开。只返回已启用的举报理由，按后台 `sortOrder`、理由编码升序排列。响应不包含 `sortOrder`；提交举报时将 `code` 作为请求字段 `reasonType`。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data` | array&lt;object&gt; | 否 | 可用举报理由数组；无可用理由时为 `[]` |
| `data[].code` | string | 否 | 稳定理由编码，例如 `COPYRIGHT` |
| `data[].name` | string | 否 | 前端展示名称 |
| `data[].description` | string | 是 | 理由说明 |

JSON 响应示例：

```json
{
  "code": 0,
  "msg": "成功",
  "data": [
    {
      "code": "COPYRIGHT",
      "name": "侵犯版权",
      "description": "未经授权使用他人享有著作权的内容"
    },
    {
      "code": "ILLEGAL_CONTENT",
      "name": "违法违规",
      "description": "涉嫌违反法律法规或平台规范"
    }
  ],
  "timestamp": 1788084000000
}
```

### 14.3 提交举报

媒体举报：

```http
POST /reports/media
token: <accessToken>
Content-Type: application/json
```

请求字段：

| 字段 | JSON 类型 | 必填 | 约束/说明 |
|---|---|---:|---|
| `mediaId` | number | 是 | 正整数；目标媒体必须存在且当前可见 |
| `reasonType` | string | 是 | 非空，最长 32；必须来自 14.2 返回的启用理由；后端去除首尾空白并转为大写 |
| `description` | string | 否 | 最长 1000；空白按 `null` 保存 |

```json
{
  "mediaId": 501,
  "reasonType": "COPYRIGHT",
  "description": "该图片疑似未经原作者授权"
}
```

用户举报：

```http
POST /reports/user
token: <accessToken>
Content-Type: application/json
```

请求字段：

| 字段 | JSON 类型 | 必填 | 约束/说明 |
|---|---|---:|---|
| `userId` | number | 是 | 正整数；目标用户必须存在，且不能是当前登录用户自己 |
| `reasonType` | string | 是 | 非空，最长 32；必须来自 14.2 返回的启用理由；后端去除首尾空白并转为大写 |
| `description` | string | 否 | 最长 1000；空白按 `null` 保存 |

```json
{
  "userId": 10002,
  "reasonType": "OTHER",
  "description": "该用户多次发布骚扰内容"
}
```

两个接口权限均为登录用户，路径决定 `targetType`，请求体不接受 `targetType` 或通用 `targetId`。

提交规则：

- 媒体目标必须存在且当前可见；用户目标必须存在且未删除。目标不可举报时统一返回 `18000`。
- 用户不能举报自己。
- 同一用户在最近一小时内最多提交 10 条举报，统计包含该用户提交的所有状态。
- 同一用户不能对同一类型、同一目标同时存在多条进行中举报；`PENDING` 和 `PROCESSING` 都视为进行中。
- 已有举报进入 `CONFIRMED`、`DISMISSED` 或 `CLOSED` 后，可以再次举报同一目标。
- 新举报固定创建为 `PENDING`，乐观锁版本固定为 `0`。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.id` | number | 否 | 新举报 ID |
| `data.targetType` | string | 否 | `MEDIA` 或 `USER`，由调用路径决定 |
| `data.targetId` | number | 否 | 被举报媒体或用户 ID |
| `data.reasonType` | string | 否 | 规范化后的理由编码 |
| `data.reasonName` | string | 否 | 理由展示名称 |
| `data.description` | string | 是 | 规范化后的补充说明 |
| `data.status` | string | 否 | 固定为 `PENDING` |
| `data.createdAt` | string | 否 | 创建时间，ISO LocalDateTime |
| `data.version` | number | 否 | 固定为 `0` |

JSON 响应示例：

```json
{
  "code": 0,
  "msg": "举报提交成功",
  "data": {
    "id": 1201,
    "targetType": "MEDIA",
    "targetId": 501,
    "reasonType": "COPYRIGHT",
    "reasonName": "侵犯版权",
    "description": "该图片疑似未经原作者授权",
    "status": "PENDING",
    "createdAt": "2026-08-30T10:00:00",
    "version": 0
  },
  "timestamp": 1788055200000
}
```

主要错误：通用字段校验错误 `400`、`18000` 目标不存在或不可举报、`18001` 理由不存在或已停用、`18002` 重复进行中举报、`18003` 超过每小时提交限制、`18011` 举报自己。

### 14.4 管理端举报列表

```http
GET /admin/reports?status=PENDING&targetType=MEDIA&reasonType=COPYRIGHT&page=1&size=20
token: <管理员 accessToken>
```

权限：管理员。结果按举报创建时间、举报 ID 降序排列。

Query 参数：

| 参数 | 类型 | 必填 | 默认值 | 约束/说明 |
|---|---:|---:|---:|---|
| `status` | `ReportStatus` | 否 | `PENDING` | 精确过滤状态，取值见 14.1；当前不能通过空值查询全部状态 |
| `targetType` | `ReportTargetType` | 否 | - | `MEDIA` 或 `USER` |
| `reasonType` | string | 否 | - | 最长 32；去除首尾空白并转为大写后精确匹配；空白表示不筛选 |
| `reporterId` | number | 否 | - | 正整数；精确过滤举报人 |
| `createdFrom` | ISO LocalDateTime | 否 | - | 创建时间下界，包含边界 |
| `createdTo` | ISO LocalDateTime | 否 | - | 创建时间上界，包含边界；不能早于 `createdFrom` |
| `page` | number | 否 | `1` | 正整数 |
| `size` | number | 否 | `20` | 正整数；大于 100 时按 100 返回 |

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.total` | number | 否 | 符合条件的举报总数 |
| `data.page` | number | 否 | 当前页码 |
| `data.size` | number | 否 | 后端规范化后的每页数量 |
| `data.items` | array&lt;object&gt; | 否 | 当前页举报摘要；无数据时为 `[]` |
| `data.items[].id` | number | 否 | 举报 ID |
| `data.items[].reporterId` | number | 否 | 举报人 ID |
| `data.items[].reporterName` | string | 是 | 优先使用昵称，其次姓名、CAS ID |
| `data.items[].targetType` | string | 否 | `MEDIA` 或 `USER` |
| `data.items[].targetId` | number | 否 | 被举报目标 ID |
| `data.items[].targetTitle` | string | 是 | 媒体标题或用户显示名；目标已删除时为空 |
| `data.items[].targetThumbnailUrl` | string | 是 | 媒体缩略图/原图或用户头像 URL；目标已删除或无文件 Key 时为空 |
| `data.items[].reasonType` | string | 否 | 举报理由编码 |
| `data.items[].reasonName` | string | 是 | 举报理由名称 |
| `data.items[].description` | string | 是 | 举报补充说明 |
| `data.items[].status` | string | 否 | 举报状态，取值见 14.1 |
| `data.items[].processedBy` | number | 是 | 最终处理管理员 ID；进行中时为空 |
| `data.items[].processorName` | string | 是 | 最终处理管理员显示名 |
| `data.items[].processedAt` | string | 是 | 最终处理时间 |
| `data.items[].createdAt` | string | 否 | 举报创建时间 |
| `data.items[].version` | number | 否 | 当前乐观锁版本；处理时作为 `expectedVersion` 提交 |

JSON 响应示例：

```json
{
  "code": 0,
  "msg": "成功",
  "data": {
    "total": 1,
    "page": 1,
    "size": 20,
    "items": [
      {
        "id": 1201,
        "reporterId": 10001,
        "reporterName": "校园摄影爱好者",
        "targetType": "MEDIA",
        "targetId": 501,
        "targetTitle": "中心校区秋景",
        "targetThumbnailUrl": "https://r2.example.com/media/501-thumb.jpg?X-Amz-Signature=...",
        "reasonType": "COPYRIGHT",
        "reasonName": "侵犯版权",
        "description": "该图片疑似未经原作者授权",
        "status": "PENDING",
        "processedBy": null,
        "processorName": null,
        "processedAt": null,
        "createdAt": "2026-08-30T10:00:00",
        "version": 0
      }
    ]
  },
  "timestamp": 1788055260000
}
```

主要错误：通用枚举、时间格式或字段校验错误 `400`；`createdFrom` 晚于 `createdTo` 时返回通用 `400`；非管理员返回 `10103`。

### 14.5 管理端举报详情

```http
GET /admin/reports/{reportId}
token: <管理员 accessToken>
```

权限：管理员。`reportId` 必须为正整数。

第一版保存举报目标的类型和 ID，但不保存举报提交时的对象快照。因此详情中的标题、描述、状态、关联用户和缩略图/头像都是查询时的当前值；目标已被删除时 `target.exists` 为 `false`，其他目标详情字段可能为空。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.id` | number | 否 | 举报 ID |
| `data.reporter` | object | 否 | 举报人信息及历史统计 |
| `data.reporter.id` | number | 否 | 举报人 ID |
| `data.reporter.casId` | string | 是 | 举报人 CAS ID |
| `data.reporter.name` | string | 是 | 举报人姓名 |
| `data.reporter.nickname` | string | 是 | 举报人昵称 |
| `data.reporter.avatarUrl` | string | 是 | 由头像 Key 生成的预签名 URL |
| `data.reporter.reportCount` | number | 否 | 该用户累计提交的举报数，包含所有状态 |
| `data.reporter.confirmedCount` | number | 否 | 该用户被确认成立的举报数 |
| `data.target` | object | 否 | 被举报目标的当前信息 |
| `data.target.type` | string | 否 | 当前为 `MEDIA` |
| `data.target.id` | number | 否 | 目标媒体 ID |
| `data.target.exists` | boolean | 否 | 目标媒体当前是否仍存在 |
| `data.target.title` | string | 是 | 媒体当前标题 |
| `data.target.description` | string | 是 | 媒体当前描述 |
| `data.target.uploaderId` | number | 是 | 媒体上传者 ID |
| `data.target.status` | number | 是 | 媒体当前状态：`0` 隐藏、`1` 可见 |
| `data.target.thumbnailUrl` | string | 是 | 缩略图优先、原图兜底的预签名 URL |
| `data.reasonType` | string | 否 | 举报理由编码 |
| `data.reasonName` | string | 是 | 举报理由名称 |
| `data.reasonDescription` | string | 是 | 举报理由字典说明 |
| `data.description` | string | 是 | 举报人填写的补充说明 |
| `data.status` | string | 否 | 举报状态，取值见 14.1 |
| `data.decisionReason` | string | 是 | 管理员最终处理理由；进行中时为空 |
| `data.processedBy` | number | 是 | 最终处理管理员 ID |
| `data.processorName` | string | 是 | 最终处理管理员显示名 |
| `data.processedAt` | string | 是 | 最终处理时间 |
| `data.relatedActiveReportCount` | number | 否 | 同一目标其他 `PENDING`、`PROCESSING` 举报数量，不包含当前举报 |
| `data.createdAt` | string | 否 | 举报创建时间 |
| `data.updatedAt` | string | 否 | 举报更新时间 |
| `data.version` | number | 否 | 当前乐观锁版本 |
| `data.operationLogs` | array&lt;object&gt; | 否 | 最近最多 100 条操作日志，按时间、日志 ID 降序排列 |
| `data.operationLogs[].id` | number | 否 | 日志 ID |
| `data.operationLogs[].operationType` | string | 否 | 当前处理日志为 `DECISION` |
| `data.operationLogs[].decision` | string | 是 | `CONFIRM`、`DISMISS` 或 `CLOSE` |
| `data.operationLogs[].beforeStatus` | string | 否 | 操作前举报状态 |
| `data.operationLogs[].afterStatus` | string | 否 | 操作后举报状态 |
| `data.operationLogs[].reason` | string | 是 | 本次处理理由 |
| `data.operationLogs[].actions` | array&lt;object&gt; | 是 | 请求中的处置动作快照 |
| `data.operationLogs[].results` | array&lt;object&gt; | 是 | 处置动作执行结果快照 |
| `data.operationLogs[].operatorId` | number | 否 | 操作管理员 ID |
| `data.operationLogs[].operatorName` | string | 是 | 操作管理员显示名 |
| `data.operationLogs[].reportVersion` | number | 否 | 操作前使用的举报版本 |
| `data.operationLogs[].createdAt` | string | 否 | 操作时间 |

JSON 响应示例：

```json
{
  "code": 0,
  "msg": "成功",
  "data": {
    "id": 1201,
    "reporter": {
      "id": 10001,
      "casId": "202600000001",
      "name": "示例用户",
      "nickname": "校园摄影爱好者",
      "avatarUrl": "https://r2.example.com/avatars/10001.jpg?X-Amz-Signature=...",
      "reportCount": 3,
      "confirmedCount": 1
    },
    "target": {
      "type": "MEDIA",
      "id": 501,
      "exists": true,
      "title": "中心校区秋景",
      "description": "中心校区秋季景色",
      "uploaderId": 10002,
      "status": 0,
      "thumbnailUrl": "https://r2.example.com/media/501-thumb.jpg?X-Amz-Signature=..."
    },
    "reasonType": "COPYRIGHT",
    "reasonName": "侵犯版权",
    "reasonDescription": "未经授权使用他人享有著作权的内容",
    "description": "该图片疑似未经原作者授权",
    "status": "CONFIRMED",
    "decisionReason": "确认存在未经授权的版权内容",
    "processedBy": 20001,
    "processorName": "内容管理员",
    "processedAt": "2026-08-30T10:10:00",
    "relatedActiveReportCount": 0,
    "createdAt": "2026-08-30T10:00:00",
    "updatedAt": "2026-08-30T10:10:00",
    "version": 1,
    "operationLogs": [
      {
        "id": 3001,
        "operationType": "DECISION",
        "decision": "CONFIRM",
        "beforeStatus": "PENDING",
        "afterStatus": "CONFIRMED",
        "reason": "确认存在未经授权的版权内容",
        "actions": [
          {
            "type": "HIDE_MEDIA",
            "frozenUntil": null,
            "reason": null
          }
        ],
        "results": [
          {
            "type": "HIDE_MEDIA",
            "targetId": 501,
            "message": "媒体已隐藏"
          }
        ],
        "operatorId": 20001,
        "operatorName": "内容管理员",
        "reportVersion": 0,
        "createdAt": "2026-08-30T10:10:00"
      }
    ]
  },
  "timestamp": 1788055800000
}
```

主要错误：路径参数校验错误 `400`、`18004` 举报不存在、非管理员 `10103`。

### 14.6 提交举报处理决定

```http
POST /admin/reports/{reportId}/decision
token: <管理员 accessToken>
Content-Type: application/json
```

权限：管理员。`reportId` 必须为正整数。

请求字段：

| 字段路径 | JSON 类型 | 必填 | 约束/说明 |
|---|---|---:|---|
| `decision` | string | 是 | `CONFIRM`、`DISMISS`、`CLOSE`，取值及终态见 14.1 |
| `reason` | string | 条件必填 | 最长 1000；空白按 `null` 处理；`CONFIRM` 时必须非空 |
| `actions` | array&lt;object&gt; | 条件必填 | 最多 10 个；未传或 `null` 按 `[]`；`CONFIRM` 时至少一个 |
| `actions[].type` | string | 是 | `HIDE_MEDIA`、`RESTORE_MEDIA`、`FREEZE_USER`、`NO_ACTION` |
| `actions[].frozenUntil` | string | 条件必填 | ISO LocalDateTime；仅 `FREEZE_USER` 使用，且必须晚于服务器当前时间 |
| `actions[].reason` | string | 条件必填 | 最长 255；`FREEZE_USER` 时去除首尾空白后必须非空 |
| `expectedVersion` | number | 是 | 非负整数；必须等于列表或详情中的最新 `version` |

确认举报成立并隐藏媒体：

```json
{
  "decision": "CONFIRM",
  "reason": "确认存在未经授权的版权内容",
  "actions": [
    {
      "type": "HIDE_MEDIA",
      "frozenUntil": null,
      "reason": null
    }
  ],
  "expectedVersion": 0
}
```

确认举报成立但不修改资源时，使用单独的 `NO_ACTION`。举报不成立或直接关闭时可以不传 `actions`，也可以只传 `NO_ACTION`：

```json
{
  "decision": "DISMISS",
  "reason": "现有信息无法证明侵权",
  "actions": [],
  "expectedVersion": 0
}
```

处理规则：

- 只允许处理 `PENDING` 或 `PROCESSING` 举报。
- 后端先比较 `expectedVersion`，更新时再次同时校验进行中状态和版本；成功后版本加一。
- 举报终态更新、媒体或用户处置、操作日志写入位于同一数据库事务，任一步失败都会整体回滚。
- `HIDE_MEDIA`、`RESTORE_MEDIA` 对已经处于目标状态的媒体按成功处理，并在动作结果中说明当前状态。
- `USER` 举报只允许 `FREEZE_USER` 或 `NO_ACTION`，不允许隐藏或恢复媒体。
- `FREEZE_USER` 对用户举报操作目标用户，对媒体举报操作媒体上传者；媒体没有上传者时拒绝处理。
- 每次成功处理都会追加一条 `DECISION` 操作日志，不覆盖历史日志。

响应 `data` 字段：

| 字段路径 | JSON 类型 | 可能为 `null` | 说明 |
|---|---|---:|---|
| `data.reportId` | number | 否 | 举报 ID |
| `data.status` | string | 否 | 处理后的终态 |
| `data.reason` | string | 是 | 规范化后的处理理由 |
| `data.processedBy` | number | 否 | 当前管理员 ID |
| `data.processedAt` | string | 否 | 处理时间 |
| `data.version` | number | 否 | 处理后的版本，即 `expectedVersion + 1` |
| `data.actions` | array&lt;object&gt; | 否 | 动作执行结果；无动作时为 `[]` |
| `data.actions[].type` | string | 否 | 已执行的动作类型 |
| `data.actions[].targetId` | number | 否 | 实际作用对象 ID；冻结用户时为上传者 ID，其他动作通常为媒体 ID |
| `data.actions[].message` | string | 否 | 动作结果说明 |

JSON 响应示例：

```json
{
  "code": 0,
  "msg": "举报处理成功",
  "data": {
    "reportId": 1201,
    "status": "CONFIRMED",
    "reason": "确认存在未经授权的版权内容",
    "processedBy": 20001,
    "processedAt": "2026-08-30T10:10:00",
    "version": 1,
    "actions": [
      {
        "type": "HIDE_MEDIA",
        "targetId": 501,
        "message": "媒体已隐藏"
      }
    ]
  },
  "timestamp": 1788055800000
}
```

主要错误：通用字段或枚举校验错误 `400`、`18004` 举报不存在、`18005` 版本冲突、`18006` 举报已是终态、`18007` 确认成立但缺少处理理由、`18008` 确认成立但缺少动作、`18009` 动作组合或参数不合法；执行资源处置时目标已不存在返回 `18000`。

媒体存在 `PENDING` 或 `PROCESSING` 举报时，管理员永久删除媒体接口返回 `18010 / HTTP 409`，不会删除媒体及关联数据。前端收到 `18005` 后应重新获取举报详情和最新版本，不要直接重复提交原请求。

## 15. 错误码对照

### 15.1 通用与认证

| HTTP | `code` | 默认 `msg` | 说明 |
|---:|---:|---|---|
| 400 | `400` | 请求错误 | JSON/枚举/字段校验/请求方法等通用错误；字段校验时 `data` 为字段错误表 |
| 500 | `500` | 服务器内部错误 | 未识别异常或内部依赖失败 |
| 400 | `10000` | 确认密码必须与原密码相同 | 已废弃，当前接口不使用 |
| 400 | `10001` | 注册所使用的sdupass JWT无效或已过期 | 当前无公开注册路由 |
| 401 | `10100` | 未登录或认证失败 | 登录接口未携带有效 access token |
| 400 | `10101` | sdupass 验证失败 | SDU Pass 换取凭证失败 |
| 404 | `10102` | 用户不存在 | token 对应用户或业务用户不存在 |
| 403 | `10103` | 该接口仅管理员可访问 | 管理员接口权限不足 |
| 403 | `10104` | 账户已被冻结或停用 | 登录、刷新或个人中心服务检查到账号不可用 |
| 401 | `10105` | access token 已过期 | 前端可尝试 refresh 后重试一次 |
| 401 | `10106` | access token 无效 | 签名、格式、token 版本或角色内容无效 |
| 401 | `10200` | refresh token 无效或已过期 | 需要重新登录 |
| 500 | `10201` | 凭证轮换失败，请稍后再试 | 并发刷新或 Redis 原子轮换失败；不要继续使用旧 token |
| 400 | `10300` | 非法的一次性登录票据 | loginTicket 无效、已过期或已被使用，需要重新发起统一认证 |

### 15.2 地图

| HTTP | `code` | 默认 `msg` | 当前使用情况 |
|---:|---:|---|---|
| 404 | `12000` | 城市不存在或已停用 | 使用中 |
| 404 | `12100` | 校区不存在或已停用 | 使用中 |
| 400 | `12101` | 校区不属于指定城市 | 已定义，当前接口未抛出 |
| 404 | `12200` | 地点不存在或已停用 | 使用中 |
| 400 | `12201` | 地点不属于指定校区 | 已定义，当前接口未抛出 |
| 400 | `12300` | 地图点位查询必须且只能指定 cityId 或 campusId | 使用中 |

### 15.3 媒体与收藏夹

| HTTP | `code` | 默认 `msg` |
|---:|---:|---|
| 404 | `13000` | 媒体不存在或不可见 |
| 409 | `13001` | 已经点赞过该媒体 |
| 409 | `13002` | 尚未点赞该媒体 |
| 404 | `13100` | 收藏夹不存在或无权访问 |
| 409 | `13101` | 该媒体已在收藏夹中 |
| 409 | `13102` | 尚未收藏该媒体 |
| 409 | `13103` | 已存在同名收藏夹 |
| 409 | `13104` | 默认收藏夹不能删除 |
| 400 | `13105` | 批量添加或移动收藏时必须指定目标收藏夹 |
| 400 | `13106` | 封面媒体不在当前收藏夹中 |
| 403 | `13200` | 当前账号无原图下载权限 |
| 400 | `13291` | 下载票据无效或已过期 |
| 404 | `13300` | 时光对比不存在或不可见 |

### 15.4 投稿与上传

| HTTP | `code` | 默认 `msg` | 说明 |
|---:|---:|---|---|
| 403 | `14000` | 该功能仅对统一认证正式用户开放 | 点赞、收藏、下载、投稿、浏览足迹等服务层检查 |
| 403 | `14001` | 当前账号无投稿权限 | `allowUpload` 未开启 |
| 404 | `14002` | 稿件不存在或无权访问 | 同时用于隐藏无权访问的稿件 |
| 409 | `14003` | 当前稿件状态不允许执行该操作 | 具体 `msg` 可能说明允许的状态 |
| 400 | `14004` | 请至少上传一张图片 | 创建无文件，或替换图片但未传新文件 |
| 400 | `14005` | 单次投稿最多上传9张图片 | 服务层合并计数超限；单请求字段超限也可能先返回通用 `400` |
| 400 | `14006` | 请先确认原创与版权声明 | 已定义；当前 Bean Validation 实际返回通用 `400` |
| 400 | `14007` | 上传照片为空 | 文件上传校验 |
| 400 | `14008` | 提交文件过大 | 应用层单文件超过 20 MiB |
| 400 | `14009` | 上传文件类型不支持 | MIME 或文件魔数不符合要求 |
| 409 | `14010` | 稿件已发生变化，请刷新后重试 | 作者修改、重新提交或撤回时发生乐观锁冲突 |
| 400 | `19000` | 请求体过大 | 请求在 multipart 解析阶段超过限制 |

### 15.5 搜索、发现与话题

| HTTP | `code` | 默认 `msg` | 说明 |
|---:|---:|---|---|
| 400 | `15000` | 搜索关键词长度不能超过50个字符 | 搜索建议接口使用；媒体搜索的字段注解也会限制长度 |
| 400 | `15001` | 搜索排序方式不正确 | `sort` 不属于允许值 |
| 404 | `15100` | 专题不存在或已停用 | 话题详情和话题媒体接口使用；代码枚举文案使用“专题” |

### 15.6 个人中心

| HTTP | `code` | 默认 `msg` | 说明 |
|---:|---:|---|---|
| 400 | `16000` | 请至少提供一项需要修改的个人资料 | `nickname` 和 `bio` 均未提供或均为 `null` |
| 400 | `16001` | 昵称长度必须在2到30个字符之间 | 昵称去除首尾空白后长度不合法 |
| 409 | `16100` | 当前账号未配置密码，不能通过该接口修改 | 当前没有首次设置密码接口 |
| 400 | `16101` | 当前密码不正确 | 当前密码不匹配、编码后超过 72 字节或并发修改失败 |
| 400 | `16102` | 两次输入的新密码不一致 | `newPassword` 与 `confirmPassword` 不同 |
| 409 | `16103` | 新密码不能与当前密码相同 | 新密码匹配当前密码 |
| 400 | `16104` | 密码编码后不能超过72字节 | 新密码 UTF-8 编码超过 BCrypt 支持范围 |
| 400 | `16200` | 头像文件为空 | 未传 `file` 或文件内容为空 |
| 400 | `16201` | 头像文件过大 | 文件超过 5 MiB |
| 400 | `16202` | 头像文件类型不支持 | 仅支持 PNG、JPEG、WebP，且 MIME 必须与文件内容一致 |

### 15.7 公告与管理员用户管理

> 当前代码为公告和管理员用户管理重复分配了 `17000～17002`。前端必须结合请求接口解释，不能只按业务码做全局文案映射。

| HTTP | `code` | 默认 `msg` | 使用接口/说明 |
|---:|---:|---|---|
| 404 | `17000` | 公告不存在或不可见 | 公告公开详情、后台公告更新/状态接口 |
| 409 | `17001` | 公告状态不允许执行该操作 | 公告发布或下线状态不合法 |
| 400 | `17002` | 请至少提供一项需要修改的公告内容 | 公告空更新 |
| 404 | `17000` | 用户不存在 | 管理员用户详情和修改接口 |
| 409 | `17001` | 不能修改自己的管理员角色 | 修改用户角色 |
| 409 | `17002` | 不能停用或冻结自己的账号 | 修改用户状态 |
| 409 | `17003` | 系统必须至少保留一名可用管理员 | 降级、停用或冻结最后一名正常管理员 |
| 400 | `17004` | 冻结用户时必须填写冻结原因 | `status = 2` 但原因为空 |
| 400 | `17005` | 冻结截止时间必须晚于当前时间 | 冻结截止时间缺失或无效 |
| 400 | `17006` | 请至少提供一项需要修改的用户权限 | 权限空更新 |

### 15.8 标签与管理员分类、资源运维

| HTTP | `code` | 默认 `msg` |
|---:|---:|---|
| 404 | `17100` | 标签不存在 |
| 409 | `17101` | 标签名称已存在 |
| 400 | `17102` | 请提供需要修改的标签名称 |
| 409 | `17103` | 标签不能合并到自身 |
| 404 | `17110` | 地点不存在 |
| 400 | `17111` | 请至少提供一项需要修改的地点信息 |
| 400 | `17112` | 目标校区不存在或已停用 |
| 400 | `17113` | 地点分类不存在或已停用 |
| 404 | `17200` | 媒体不存在 |
| 400 | `17201` | 请至少提供地点或标签分类 |
| 400 | `17202` | 媒体所属地点不存在或已停用 |
| 409 | `17203` | 媒体已经处于隐藏状态 |
| 409 | `17204` | 媒体已经处于可见状态 |
| 502 | `17205` | 媒体存储文件删除失败 |
| 400 | `17300` | 时光对比包含不存在或不可用的媒体 |
| 400 | `17301` | 时光对比地点不存在或已停用 |
| 404 | `17400` | 专题不存在 |
| 409 | `17401` | 专题标识已存在 |
| 400 | `17402` | 请至少提供一项需要修改的专题信息 |

### 15.9 管理员投稿审核

| HTTP | `code` | 默认 `msg` | 说明 |
|---:|---:|---|---|
| 404 | `17500` | 稿件不存在 | 管理端详情、审核或审核记录查询目标不存在 |
| 409 | `17501` | 稿件已不处于待审核状态 | 仅 `PENDING` 稿件允许审核 |
| 409 | `17502` | 稿件已被其他管理员处理，请刷新后重试 | `expectedVersion` 过期，或相同版本已被其他审核人/决定使用 |
| 400 | `17503` | 退回或拒绝稿件时必须填写原因 | `RETURN`、`REJECT` 的原因去除首尾空白后为空 |

### 15.10 举报与举报管理

| HTTP | `code` | 默认 `msg` |
|---:|---:|---|
| 404 | `18000` | 举报对象不存在或不可举报 |
| 400 | `18001` | 举报理由不存在或已停用 |
| 409 | `18002` | 请勿重复举报同一内容 |
| 429 | `18003` | 举报提交过于频繁，请稍后再试 |
| 404 | `18004` | 举报记录不存在 |
| 409 | `18005` | 举报已被其他管理员处理，请刷新后重试 |
| 409 | `18006` | 当前举报状态不允许执行该操作 |
| 400 | `18007` | 确认举报成立时必须填写处理理由 |
| 400 | `18008` | 确认举报成立时必须指定处置动作 |
| 400 | `18009` | 举报处置动作不合法 |
| 409 | `18010` | 媒体存在尚未处理的举报，暂时无法删除 |
| 400 | `18011` | 不能举报自己 |

## 16. 前端接入检查清单

- 使用环境变量维护 `BASE_URL`，不要根据 Controller 注释硬编码 `/api/v1`。
- 请求头名称使用小写或原样 `token`，值只放 JWT，不加 `Bearer `。
- access token 过期仅触发一次 refresh；对并发请求合并刷新，刷新后同时替换两个 token。
- refresh 返回 `10200` 时清理登录态并跳转登录；`10201` 不要重放旧 refresh token。
- 公开媒体详情需要展示用户互动状态时携带有效 token；未登录时按 `false` 处理。
- 所有接口先检查 HTTP 状态，再检查 `code === 0`，不要依赖 `msg` 文案。
- 枚举传英文大写名称；投稿状态使用 `PENDING/APPROVED/RETURNED/WITHDRAWN/REJECTED`，审核决定使用 `APPROVE/RETURN/REJECT`。
- 投稿使用多值 FormData 字段；创建投稿传 `tagIds`，修改投稿当前仍传 `tags`，文件均传 `files`；不手动写 multipart boundary。
- 头像使用字段名为 `file` 的 FormData 上传；限制为 PNG、JPEG、WebP 和 5 MiB。
- 预签名 URL（包括媒体、投稿预览、头像和旧版下载地址）只作短期展示/下载使用，过期后重新请求对应详情。
- 浏览器按钮下载原图推荐先调用 `/media/{mediaId}/downloads/ticket` 取票，再打开 `/media/downloads/stream?ticket=...`；流式下载响应是文件二进制，不是统一 JSON。
- 修改密码成功后立即清除本地 access token 和 refresh token，并引导用户重新登录。
- 浏览足迹的 `size` 最大按 50 返回；清空和单条删除均按幂等成功处理。
- 搜索排序值使用 `relevance`、`newest`、`oldest`、`hot`；搜索建议的 `TAG` 类型没有 ID。
- 批量收藏操作的 `action` 使用大写 `ADD`、`REMOVE`、`MOVE`；`requestedCount` 是媒体 ID 去重后的数量。
- 管理员用户接口的 `role/status` 使用数字编码；公告状态使用大写字符串 `DRAFT/PUBLISHED/OFFLINE`。
- 管理员审核提交前必须使用列表或详情返回的最新 `version` 作为 `expectedVersion`；收到 `17502` 时刷新稿件，不要直接使用旧版本重试。
- 批量审核必须逐项检查 `data.items[].success`；外层 `code === 0` 不表示全部成功。
- 管理员永久删除媒体会同时删除业务关联和存储文件，前端应增加不可恢复确认。
- 举报理由必须从 `/reports/reasons` 获取；管理员处理举报前使用详情中的最新 `version`，收到 `18005` 后刷新而不是直接重试。
- `17000～17002` 当前在公告和管理员用户模块间冲突，错误展示必须结合请求接口。
- 可用 `/ping/public` 做免登录存活检查，用 `/ping/auth`、`/ping/admin` 分别检查登录和管理员鉴权链路。

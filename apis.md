# visualSDU 前端接口对照文档

> 总结日期：2026-08-16
> 本文描述的是**当前代码实际行为**，不是规划中的接口。

## 1. 全局约定

### 1.1 基础地址与路径前缀

下文路径均为不包含 `BASE_URL` 的后端原始路径，例如：

```text
GET {BASE_URL}/cities
```

### 1.2 统一响应体

所有 Controller 正常响应和已处理异常都使用相同响应体：

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
- 所有成功接口当前均返回 HTTP `200`；创建接口不是 `201`，无数据接口也不是 `204`。
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
- 投稿创建和修改使用 `multipart/form-data`；使用浏览器 `FormData` 时不要手动设置带 boundary 的 `Content-Type`。
- `LocalDateTime` 使用不带时区的 ISO 8601 字符串，例如 `2026-08-15T14:30:00`。
- ID、计数和经纬度在 JSON 中均为数字。
- 投稿标签和文件使用同名多值字段，例如多次 `formData.append("tags", tag)`、多次 `formData.append("files", file)`。
- 媒体图片、缩略图、投稿预览和下载地址由 R2 预签名生成，有效期为 10 分钟。不要长期缓存 URL；需要时重新请求对应详情。
- 城市、校区、地点的 `coverUrl` 是数据库直接返回值，不走上述预签名逻辑。

## 2. 接口总览

至今开发完成的共有 41 个实际映射的接口。

| 模块 | 方法 | 路径 | 权限 | 请求格式 | `data` 类型 |
|---|---|---|---|---|---|
| 认证 | GET | `/auth/sdupass-login` | 公开 | Query | 凭证对象 |
| 认证 | POST | `/auth/refresh` | 公开 | JSON | 凭证对象 |
| 认证 | DELETE | `/auth/logout` | 登录 | JSON | `null` |
| 城市 | GET | `/cities` | 公开 | - | 城市对象数组 |
| 城市 | GET | `/cities/{cityId}/campuses` | 公开 | Path | 校区摘要对象数组 |
| 校区 | GET | `/campuses/{campusId}` | 公开 | Path | 校区详情对象 |
| 校区 | GET | `/campuses/{campusId}/locations` | 公开 | Query | 地点摘要对象数组 |
| 地点 | GET | `/locations/{locationId}` | 公开 | Path | 地点详情对象 |
| 地图 | GET | `/map/markers` | 公开 | Query | 点位对象数组 |
| 媒体 | GET | `/media/{mediaId}` | 公开 | Path | 媒体详情对象 |
| 媒体 | POST | `/media/{mediaId}/views` | 公开 | Path | 媒体互动状态对象 |
| 媒体 | POST | `/media/{mediaId}/likes` | 登录 | Path | 媒体互动状态对象 |
| 媒体 | DELETE | `/media/{mediaId}/likes` | 登录 | Path | 媒体互动状态对象 |
| 媒体 | POST | `/media/{mediaId}/favorites` | 登录 | 可选 JSON | 媒体互动状态对象 |
| 媒体 | DELETE | `/media/{mediaId}/favorites` | 登录 | Path | 媒体互动状态对象 |
| 媒体 | POST | `/media/{mediaId}/downloads` | 登录 | Path | 下载信息对象 |
| 媒体 | GET | `/media/{mediaId}/related` | 公开 | Query | 媒体摘要对象数组 |
| 搜索 | GET | `/search/suggestions` | 公开 | Query | 搜索建议对象数组 |
| 搜索 | GET | `/search/media` | 公开 | Query | 媒体摘要分页对象 |
| 发现 | GET | `/discovery/home` | 公开 | Query | 发现首页聚合对象 |
| 话题 | GET | `/topics` | 公开 | - | 话题摘要对象数组 |
| 话题 | GET | `/topics/{topicId}` | 公开 | Path | 话题详情对象 |
| 话题 | GET | `/topics/{topicId}/media` | 公开 | Query | 媒体摘要分页对象 |
| 收藏夹 | GET | `/favorite-folders` | 登录 | - | 收藏夹对象数组 |
| 收藏夹 | POST | `/favorite-folders` | 登录 | JSON | 收藏夹对象 |
| 收藏夹 | PATCH | `/favorite-folders/{folderId}` | 登录 | JSON | 收藏夹对象 |
| 收藏夹 | DELETE | `/favorite-folders/{folderId}` | 登录 | Path | `null` |
| 收藏夹 | GET | `/favorite-folders/{folderId}/items` | 登录 | Query | 媒体摘要分页对象 |
| 收藏 | POST | `/favorites/batch` | 登录 | JSON | 批量收藏结果对象 |
| 时光对比 | GET | `/time-comparisons` | 登录 | Query | 时光对比摘要对象数组 |
| 时光对比 | GET | `/time-comparisons/{comparisonId}` | 登录 | Path | 时光对比详情对象 |
| 投稿 | POST | `/submissions` | 登录 | Multipart | 投稿详情对象 |
| 投稿 | GET | `/submissions/mine` | 登录 | Query | 投稿摘要分页对象 |
| 投稿 | GET | `/submissions/{submissionId}` | 登录 | Path | 投稿详情对象 |
| 投稿 | PUT | `/submissions/{submissionId}` | 登录 | Multipart | 投稿详情对象 |
| 投稿 | POST | `/submissions/{submissionId}/resubmit` | 登录 | Path | 投稿详情对象 |
| 投稿 | POST | `/submissions/{submissionId}/withdraw` | 登录 | Path | `null` |
| 管理 | PUT | `/admin/settings/submission-review` | 管理员 | JSON | 审核设置对象 |
| 健康检查 | GET | `/ping/public` | 公开 | - | `null` |
| 健康检查 | GET | `/ping/auth` | 登录 | - | `null` |
| 健康检查 | GET | `/ping/admin` | 管理员 | - | `null` |

## 3. 认证接口

### 3.1 SDU Pass 登录回调

```http
GET /auth/sdupass-login?code=<callbackCode>
```

权限：公开。

Query 参数：

| 参数 | 类型 | 必填 | 说明 |
|---|---:|---:|---|
| `code` | string | 是 | SDU Pass 登录完成后回调携带的临时凭证 |

已有用户直接登录，首次访问的统一认证用户会自动创建账号。

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

- `10101`：SDU Pass 换取 token 失败。
- `10104`：账号被冻结或停用。
- `500`：登录凭证存储等内部过程失败。

### 3.2 刷新令牌

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

### 3.3 退出登录

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

### 4.1 城市列表

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
| `data[].coverUrl` | string | 是 | 城市封面地址 |
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
      "coverUrl": "https://example.com/cities/jinan.jpg",
      "description": "山东大学主要校区所在城市",
      "campusCount": 4
    }
  ],
  "timestamp": 1786773600000
}
```

### 4.2 城市下的校区

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

### 4.3 校区详情

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

### 4.4 校区下的地点

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
| `data[].categoryCode` | string | 是 | 地点分类代码 |
| `data[].address` | string | 是 | 地点地址 |
| `data[].longitude` | number | 是 | 经度 |
| `data[].latitude` | number | 是 | 纬度 |
| `data[].coverUrl` | string | 是 | 地点封面地址 |

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
      "coverUrl": "https://example.com/locations/zhixin.jpg"
    }
  ],
  "timestamp": 1786773600000
}
```

主要错误：`12100` 校区不存在或已停用。

### 4.5 地点详情

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
| `data.categoryCode` | string | 是 | 地点分类代码 |
| `data.address` | string | 是 | 地点地址 |
| `data.longitude` | number | 是 | 经度 |
| `data.latitude` | number | 是 | 纬度 |
| `data.coverUrl` | string | 是 | 地点封面地址 |
| `data.description` | string | 是 | 地点描述 |

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
    "coverUrl": "https://example.com/locations/zhixin.jpg",
    "description": "教学与科研建筑"
  },
  "timestamp": 1786773600000
}
```

主要错误：

- `12200`：地点不存在或已停用。
- `12100`：所属校区不存在或已停用。
- `12000`：所属城市不存在或已停用。

### 4.6 地图点位

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
| `data[].coverUrl` | string | 是 | 点位封面地址 |

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

### 5.8 相关媒体

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
| `REJECTED` | 已退回 |
| `WITHDRAWN` | 已撤回 |

主要状态流转：

```text
创建 ──审核开启──> PENDING ──撤回──> WITHDRAWN
  │                   │
  │                   └──后台审核退回（当前仓库无对应 HTTP 接口）──> REJECTED
  │                                                                  │
  └──审核关闭──> APPROVED <────────────重新提交且审核关闭──────────────┘
                                      │
                                      └──审核开启──> PENDING
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
| `tags` | string[] | 否 | 最多 20 个；单个最多 32 字符 |
| `description` | string | 否 | 最多 2000 字符 |
| `copyrightConfirmed` | boolean | 是 | 必须为 `true` |

文件限制：

- 支持 `image/png`、`image/jpeg`、`image/webp`。
- 后端同时检查 MIME 类型和文件魔数，两者必须一致，仅改扩展名无法通过。
- 应用层单文件上限为 20 MiB；请求还可能先受到 Spring multipart 或部署网关上限限制。
- 空文件会被过滤；过滤后必须仍至少有一张有效图片。

标签保存前会去首尾空白、移除空标签、移除标签内的 `|`、去重，并保留前 20 个。

浏览器示例：

```javascript
const form = new FormData();
for (const file of files) form.append("files", file);
form.append("locationId", String(locationId));
if (shotAt) form.append("shotAt", shotAt);
for (const tag of tags) form.append("tags", tag);
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

主要错误：`12200`、`14000`、`14001`、`14004`、`14005`、`14007`、`14008`、`14009`、`19000` 及通用字段校验错误 `400`。

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
    "status": "REJECTED",
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

仅投稿所有者可修改，且状态必须为 `PENDING` 或 `REJECTED`。Multipart 字段均为可选：

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
- 修改 `REJECTED` 稿件不会自动重新提交，修改后仍为 `REJECTED`，需要再调用 `resubmit`。

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
    "status": "REJECTED",
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

仅所有者、正式正常且允许投稿的用户可调用；只有 `REJECTED` 稿件可重新提交。

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

> 当前没有读取审核开关的 GET 接口，也没有审核通过/退回投稿的 HTTP 接口。

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
      "mediaCount": 36
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

本节接口均要求有效 access token。当前服务只检查登录状态，没有像单媒体点赞/收藏接口那样额外检查是否为统一认证正式用户。

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

### 11.6 批量管理收藏

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

## 12. 错误码对照

### 12.1 通用与认证

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
| 403 | `10104` | 账户已被冻结或停用 | 登录/刷新时账号不可用 |
| 401 | `10105` | access token 已过期 | 前端可尝试 refresh 后重试一次 |
| 401 | `10106` | access token 无效 | 签名、格式、token 版本或角色内容无效 |
| 401 | `10200` | refresh token 无效或已过期 | 需要重新登录 |
| 500 | `10201` | 凭证轮换失败，请稍后再试 | 并发刷新或 Redis 原子轮换失败；不要继续使用旧 token |

### 12.2 地图

| HTTP | `code` | 默认 `msg` | 当前使用情况 |
|---:|---:|---|---|
| 404 | `12000` | 城市不存在或已停用 | 使用中 |
| 404 | `12100` | 校区不存在或已停用 | 使用中 |
| 400 | `12101` | 校区不属于指定城市 | 已定义，当前接口未抛出 |
| 404 | `12200` | 地点不存在或已停用 | 使用中 |
| 400 | `12201` | 地点不属于指定校区 | 已定义，当前接口未抛出 |
| 400 | `12300` | 地图点位查询必须且只能指定 cityId 或 campusId | 使用中 |

### 12.3 媒体与收藏夹

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
| 404 | `13300` | 时光对比不存在或不可见 |

### 12.4 投稿与上传

| HTTP | `code` | 默认 `msg` | 说明 |
|---:|---:|---|---|
| 403 | `14000` | 该功能仅对统一认证正式用户开放 | 点赞、收藏、下载、投稿等服务层检查 |
| 403 | `14001` | 当前账号无投稿权限 | `allowUpload` 未开启 |
| 404 | `14002` | 稿件不存在或无权访问 | 同时用于隐藏无权访问的稿件 |
| 409 | `14003` | 当前稿件状态不允许执行该操作 | 具体 `msg` 可能说明允许的状态 |
| 400 | `14004` | 请至少上传一张图片 | 创建无文件，或替换图片但未传新文件 |
| 400 | `14005` | 单次投稿最多上传9张图片 | 服务层合并计数超限；单请求字段超限也可能先返回通用 `400` |
| 400 | `14006` | 请先确认原创与版权声明 | 已定义；当前 Bean Validation 实际返回通用 `400` |
| 400 | `14007` | 上传照片为空 | 文件上传校验 |
| 400 | `14008` | 提交文件过大 | 应用层单文件超过 20 MiB |
| 400 | `14009` | 上传文件类型不支持 | MIME 或文件魔数不符合要求 |
| 400 | `19000` | 请求体过大 | 请求在 multipart 解析阶段超过限制 |

### 12.5 搜索、发现与话题

| HTTP | `code` | 默认 `msg` | 说明 |
|---:|---:|---|---|
| 400 | `15000` | 搜索关键词长度不能超过50个字符 | 搜索建议接口使用；媒体搜索的字段注解也会限制长度 |
| 400 | `15001` | 搜索排序方式不正确 | `sort` 不属于允许值 |
| 404 | `15100` | 专题不存在或已停用 | 话题详情和话题媒体接口使用；代码枚举文案使用“专题” |

## 13. 前端接入检查清单

- 使用环境变量维护 `BASE_URL`，不要根据 Controller 注释硬编码 `/api/v1`。
- 请求头名称使用小写或原样 `token`，值只放 JWT，不加 `Bearer `。
- access token 过期仅触发一次 refresh；对并发请求合并刷新，刷新后同时替换两个 token。
- refresh 返回 `10200` 时清理登录态并跳转登录；`10201` 不要重放旧 refresh token。
- 公开媒体详情需要展示用户互动状态时携带有效 token；未登录时按 `false` 处理。
- 所有接口先检查 HTTP 状态，再检查 `code === 0`，不要依赖 `msg` 文案。
- 枚举传英文大写名称，尤其是 `SubmissionStatus`。
- 投稿使用多值 FormData 字段；不手动写 multipart boundary。
- 预签名媒体 URL 只作短期展示/下载使用，过期后重新请求详情。
- 搜索排序值使用 `relevance`、`newest`、`oldest`、`hot`；搜索建议的 `TAG` 类型没有 ID。
- 批量收藏操作的 `action` 使用大写 `ADD`、`REMOVE`、`MOVE`；`requestedCount` 是媒体 ID 去重后的数量。
- 可用 `/ping/public` 做免登录存活检查，用 `/ping/auth`、`/ping/admin` 分别检查登录和管理员鉴权链路。

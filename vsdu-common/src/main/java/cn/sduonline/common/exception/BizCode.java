package cn.sduonline.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BizCode {

    OK(0, "成功", 200),

    BAD_REQUEST(400, "请求错误", 400),
    INTERNAL_SERVER_ERROR(500, "服务器内部错误", 500),

    // 认证授权业务码 10xxx
    // 注册相关：100xx
    @Deprecated
    REGISTRATION_PASSWORD_CONFIRM_FAILED(10000, "确认密码必须与原密码相同", 400),
    REGISTRATION_SDUPASS_JWT_INVALID(10001, "注册所使用的sdupass JWT无效或已过期", 400),
    // 鉴权相关：101xx
    UNAUTHORIZED(10100, "未登录或认证失败", 401),
    SDUPASS_LOGIN_FAILED(10101, "sdupass 验证失败", 400),
    AUTH_USER_NOT_FOUND(10102, "用户不存在", 404),
    ADMIN_REQUIRED(10103, "该接口仅管理员可访问", 403),
    FROZEN_USER(10104, "账户已被冻结或停用", 403),
    AUTH_ACCESS_TOKEN_EXPIRED(10105, "access token 已过期", 401),
    AUTH_ACCESS_TOKEN_INVALID(10106, "access token 无效", 401),
    // 凭证轮换相关：102xx
    AUTH_REFRESH_TOKEN_INVALID(10200, "refresh token 无效或已过期", 401),
    AUTH_TOKEN_ROTATE_FAIL(10201, "凭证轮换失败，请稍后再试", 500),


    //地图：12xxx
    CITY_NOT_FOUND(12000, "城市不存在或已停用", 404),

    CAMPUS_NOT_FOUND(12100, "校区不存在或已停用", 404),
    CAMPUS_NOT_BELONG_TO_CITY(12101, "校区不属于指定城市", 400),

    LOCATION_NOT_FOUND(12200, "地点不存在或已停用", 404),
    LOCATION_NOT_BELONG_TO_CAMPUS(12201, "地点不属于指定校区", 400),

    MAP_QUERY_SCOPE_INVALID(12300, "地图点位查询必须且只能指定 cityId 或 campusId", 400),

    // 媒体浏览与互动：13xxx
    MEDIA_NOT_FOUND(13000, "媒体不存在或不可见", 404),
    MEDIA_ALREADY_LIKED(13001, "已经点赞过该媒体", 409),
    MEDIA_NOT_LIKED(13002, "尚未点赞该媒体", 409),
    FAVORITE_FOLDER_NOT_FOUND(13100, "收藏夹不存在或无权访问", 404),
    MEDIA_ALREADY_FAVORITED(13101, "该媒体已在收藏夹中", 409),
    MEDIA_NOT_FAVORITED(13102, "尚未收藏该媒体", 409),
    FAVORITE_FOLDER_NAME_EXISTS(13103, "已存在同名收藏夹", 409),
    FAVORITE_DEFAULT_FOLDER_CANNOT_DELETE(13104, "默认收藏夹不能删除", 409),
    FAVORITE_BATCH_FOLDER_REQUIRED(13105, "批量添加或移动收藏时必须指定目标收藏夹", 400),
    FAVORITE_COVER_NOT_IN_FOLDER(13106, "封面媒体不在当前收藏夹中", 400),
    MEDIA_DOWNLOAD_FORBIDDEN(13200, "当前账号无原图下载权限", 403),
    TIME_COMPARISON_NOT_FOUND(13300, "时光对比不存在或不可见", 404),

    // 用户投稿：14xxx
    FORMAL_USER_REQUIRED(14000, "该功能仅对统一认证正式用户开放", 403),
    SUBMISSION_UPLOAD_FORBIDDEN(14001, "当前账号无投稿权限", 403),
    SUBMISSION_NOT_FOUND(14002, "稿件不存在或无权访问", 404),
    SUBMISSION_STATUS_INVALID(14003, "当前稿件状态不允许执行该操作", 409),
    SUBMISSION_FILE_REQUIRED(14004, "请至少上传一张图片", 400),
    SUBMISSION_FILE_COUNT_EXCEEDED(14005, "单次投稿最多上传9张图片", 400),
    SUBMISSION_COPYRIGHT_UNCONFIRMED(14006, "请先确认原创与版权声明", 400),
    SUBMISSION_FILE_EMPTY(14007, "上传照片为空", 400),
    SUBMISSION_FILE_TOO_LARGE(14008, "提交文件过大", 400),
    SUBMISSION_FILE_TYPE_NOT_SUPPORT(14009, "上传文件类型不支持", 400),

    // 搜索与发现：15xxx
    SEARCH_KEYWORD_TOO_LONG(15000, "搜索关键词长度不能超过50个字符", 400),
    SEARCH_SORT_INVALID(15001, "搜索排序方式不正确", 400),
    TOPIC_NOT_FOUND(15100, "专题不存在或已停用", 404),

    // 个人中心：16xxx
    USER_PROFILE_UPDATE_EMPTY(16000, "请至少提供一项需要修改的个人资料", 400),
    USER_NICKNAME_INVALID(16001, "昵称长度必须在2到30个字符之间", 400),
    USER_PASSWORD_NOT_CONFIGURED(16100, "当前账号未配置密码，不能通过该接口修改", 409),
    USER_CURRENT_PASSWORD_INCORRECT(16101, "当前密码不正确", 400),
    USER_PASSWORD_CONFIRM_MISMATCH(16102, "两次输入的新密码不一致", 400),
    USER_PASSWORD_UNCHANGED(16103, "新密码不能与当前密码相同", 409),
    USER_PASSWORD_TOO_LONG(16104, "密码编码后不能超过72字节", 400),
    USER_AVATAR_FILE_EMPTY(16200, "头像文件为空", 400),
    USER_AVATAR_FILE_TOO_LARGE(16201, "头像文件过大", 400),
    USER_AVATAR_FILE_TYPE_NOT_SUPPORT(16202, "头像文件类型不支持", 400),

    // 公告：17xxx
    ANNOUNCEMENT_NOT_FOUND(17000, "公告不存在或不可见", 404),
    ANNOUNCEMENT_STATUS_INVALID(17001, "公告状态不允许执行该操作", 409),
    ANNOUNCEMENT_UPDATE_EMPTY(17002, "请至少提供一项需要修改的公告内容", 400),

    // 其它：19xxx
    UPLOAD_SIZE_TOO_LARGE(19000, "请求体过大", 400),



    ;



    private final Integer code;
    private final String msg;
    private final Integer httpStatusCode;
}

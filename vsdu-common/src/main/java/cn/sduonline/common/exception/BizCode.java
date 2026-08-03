package cn.sduonline.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BizCode {

    OK(0, "成功", 200),

    BAD_REQUEST(400, "请求错误", 400),
    INTERNAL_SERVER_ERROR(500, "服务器内部错误", 500),

    //地图：12xxx
    CITY_NOT_FOUND(12000, "城市不存在或已停用", 404),

    CAMPUS_NOT_FOUND(12100, "校区不存在或已停用", 404),
    CAMPUS_NOT_BELONG_TO_CITY(12101, "校区不属于指定城市", 400),

    LOCATION_NOT_FOUND(12200, "地点不存在或已停用", 404),
    LOCATION_NOT_BELONG_TO_CAMPUS(12201, "地点不属于指定校区", 400),

    MAP_QUERY_SCOPE_INVALID(
            12300,
            "地图点位查询必须且只能指定 cityId 或 campusId",
            400
    )



    ;



    private final Integer code;
    private final String msg;
    private final Integer httpStatusCode;
}

package cn.sduonline.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BizCode {

    OK(0, "成功", 200),

    BAD_REQUEST(400, "请求错误", 400),
    INTERNAL_SERVER_ERROR(500, "服务器内部错误", 500),


    ;

    private final Integer code;
    private final String msg;
    private final Integer httpStatusCode;
}

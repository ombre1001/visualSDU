package cn.sduonline.common.result;

import cn.sduonline.common.exception.BizCode;

public record Result<T>(
        Integer code,
        String msg,
        T data,
        Long timestamp
) {

    public static Result<Void> ok() {
        return new Result<>(
                BizCode.OK.getCode(),
                BizCode.OK.getMsg(),
                null,
                System.currentTimeMillis()
        );
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(
                BizCode.OK.getCode(),
                BizCode.OK.getMsg(),
                data,
                System.currentTimeMillis()
        );
    }

    public static <T> Result<T> success(T data, String msg) {
        return new Result<>(
                BizCode.OK.getCode(),
                msg,
                data,
                System.currentTimeMillis()
        );
    }

    public static <T> Result<T> error(BizCode bizCode) {
        return new Result<>(
                bizCode.getCode(),
                bizCode.getMsg(),
                null,
                System.currentTimeMillis()
        );
    }

    public static <T> Result<T> error(BizCode bizCode, String msg) {
        return new Result<>(
                bizCode.getCode(),
                msg,
                null,
                System.currentTimeMillis()
        );
    }

    public static <T> Result<T> error(BizCode bizCode, T data, String msg) {
        return new Result<>(
                bizCode.getCode(),
                msg,
                data,
                System.currentTimeMillis()
        );
    }
}

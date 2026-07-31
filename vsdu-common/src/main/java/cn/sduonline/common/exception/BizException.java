package cn.sduonline.common.exception;

import lombok.Getter;

@Getter
public class BizException extends RuntimeException {

    private final BizCode bizCode;

    public BizException(BizCode bizCode) {
        super(bizCode.getMsg());
        this.bizCode = bizCode;
    }

    public BizException(BizCode bizCode, String message) {
        super(message);
        this.bizCode = bizCode;
    }
}

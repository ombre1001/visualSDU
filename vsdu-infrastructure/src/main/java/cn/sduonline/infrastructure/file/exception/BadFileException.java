package cn.sduonline.infrastructure.file.exception;

import lombok.Getter;

public class BadFileException extends RuntimeException {

    private static final String BAD_FILE_ERROR_MSG = "由于文件自身问题而导致的上传失败";

    @Getter
    private final BadFileErrorCode errorCode;

    public BadFileException(BadFileErrorCode errorCode) {
        super(BAD_FILE_ERROR_MSG);
        this.errorCode = errorCode;
    }

    public enum BadFileErrorCode {

        /**
         * 上传文件为空
         */
        FILE_EMPTY,

        /**
         * 上传文件过大
         */
        FILE_TOO_LARGE,

        /**
         * 文件类型不支持
         */
        FILE_TYPE_NOT_SUPPORT,
    }
}

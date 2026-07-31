package cn.sduonline.infrastructure.file.exception;

public class FileStorageException extends RuntimeException {

    private static final String FILE_STORAGE_ERROR_MSG = "文件传输失败";

    public FileStorageException() {
        super(FILE_STORAGE_ERROR_MSG);
    }

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}

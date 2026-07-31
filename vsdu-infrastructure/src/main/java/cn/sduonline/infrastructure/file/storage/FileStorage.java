package cn.sduonline.infrastructure.file.storage;


import cn.sduonline.infrastructure.file.exception.FileStorageException;
import cn.sduonline.infrastructure.file.model.UploadFile;

public interface FileStorage {

    void storage(UploadFile uploadFile) throws FileStorageException;

    void delete(String objectKey) throws FileStorageException;

    String getUrl(String objectKey);

    default void deleteQuietly(String objectKey) {
        try {
            delete(objectKey);
        } catch (RuntimeException ignored) {
            // Ignore cleanup failure to preserve the original profile update error.
        }
    }
}

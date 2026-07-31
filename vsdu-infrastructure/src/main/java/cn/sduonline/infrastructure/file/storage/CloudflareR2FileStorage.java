package cn.sduonline.infrastructure.file.storage;

import cn.sduonline.infrastructure.file.exception.FileStorageException;
import cn.sduonline.infrastructure.file.model.UploadFile;
import cn.sduonline.infrastructure.r2.CloudflareR2Client;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Slf4j
@RequiredArgsConstructor
public class CloudflareR2FileStorage implements FileStorage{

    private final CloudflareR2Client r2Client;

    @Override
    public void storage(UploadFile uploadFile) throws FileStorageException {
        try {
            r2Client.upload(
                    uploadFile.objectKey(),
                    uploadFile.inputStream(),
                    uploadFile.size(),
                    uploadFile.contentType()
            );
        } catch (S3Exception | SdkClientException e) {
            log.error(
                    "Cloudflare R2拒绝上传文件：objectKey={}",
                    uploadFile.objectKey(),
                    e
            );
            throw new FileStorageException("上传文件失败", e);
        }
    }

    @Override
    public void delete(String objectKey) throws FileStorageException {
        try {
            r2Client.delete(objectKey);
        } catch (S3Exception | SdkClientException e) {
            log.error(
                    "Cloudflare R2拒绝上传文件：objectKey={}",
                    objectKey,
                    e
            );
            throw new FileStorageException("删除文件失败", e);
        }
    }

    @Override
    public String getUrl(String objectKey) {
        return r2Client.generatePresignedUrl(objectKey);
    }
}

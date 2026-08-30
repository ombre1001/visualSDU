package cn.sduonline.infrastructure.r2;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;

@Slf4j
public class CloudflareR2Client {

    private final CloudflareR2Properties properties;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    public CloudflareR2Client(
            CloudflareR2Properties properties
    ) throws S3Exception {
        this.properties = properties;

        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                properties.accessKeyId(),
                properties.secretAccessKey()
        );

        StaticCredentialsProvider credentialsProvider =
                StaticCredentialsProvider.create(credentials);

        S3Configuration configuration = S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .chunkedEncodingEnabled(false)
                .build();

        URI endpoint = URI.create(properties.endpoint());

        this.s3Client = S3Client.builder()
                .endpointOverride(endpoint)
                .credentialsProvider(credentialsProvider)
                .region(Region.of("auto"))
                .serviceConfiguration(configuration)
                .build();

        this.s3Presigner = S3Presigner.builder()
                .endpointOverride(endpoint)
                .credentialsProvider(credentialsProvider)
                .region(Region.of("auto"))
                .serviceConfiguration(configuration)
                .build();
    }

    /**
     * 上传文件到 Cloudflare R2。
     * <p>
     * 将输入流转换为字节数组，保证 AWS SDK 在签名、发送或重试时
     * 可以重复读取上传内容。
     */
    public void upload(
            String objectKey,
            InputStream inputStream,
            long size,
            String contentType
    ) throws S3Exception {
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("R2对象键不能为空");
        }

        if (inputStream == null) {
            throw new IllegalArgumentException("上传文件输入流不能为空");
        }

        try {
            byte[] fileBytes = inputStream.readAllBytes();

            if (fileBytes.length == 0) {
                throw new IllegalArgumentException("上传文件内容不能为空");
            }

            // 使用实际读取到的字节数，避免传入的 size 与文件内容不一致
            long actualSize = fileBytes.length;

            if (size >= 0 && size != actualSize) {
                log.warn(
                        "上传文件大小与实际读取大小不一致：key={} declaredSize={} actualSize={}",
                        objectKey,
                        size,
                        actualSize
                );
            }

            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(properties.bucket())
                    .key(objectKey)
                    .contentType(
                            contentType == null || contentType.isBlank()
                                    ? "application/octet-stream"
                                    : contentType
                    )
                    .contentLength(actualSize)
                    .build();

            PutObjectResponse response = s3Client.putObject(
                    request,
                    RequestBody.fromBytes(fileBytes)
            );

            log.info(
                    "上传文件到 Cloudflare R2 成功：key={} size={} eTag={}",
                    objectKey,
                    actualSize,
                    response.eTag()
            );
        } catch (IOException e) {
            log.error(
                    "读取待上传文件失败：key={}",
                    objectKey,
                    e
            );

            throw new IllegalStateException(
                    "读取待上传文件失败：" + objectKey,
                    e
            );
        }
    }

    public void delete(String objectKey) throws S3Exception {
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("R2对象键不能为空");
        }

        s3Client.deleteObject(
                DeleteObjectRequest.builder()
                        .bucket(properties.bucket())
                        .key(objectKey)
                        .build()
        );

        log.info(
                "删除 Cloudflare R2 文件成功：key={}",
                objectKey
        );
    }

    public String generatePresignedUrl(String objectKey) throws S3Exception {
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("R2对象键不能为空");
        }

        GetObjectRequest objectRequest = GetObjectRequest.builder()
                .bucket(properties.bucket())
                .key(objectKey)
                .build();

        GetObjectPresignRequest presignRequest =
                GetObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMinutes(10))
                        .getObjectRequest(objectRequest)
                        .build();

        return s3Presigner.presignGetObject(presignRequest)
                .url()
                .toString();
    }

    public ResponseInputStream<GetObjectResponse> getObjectStream(String objectKey) {
        GetObjectRequest objectRequest = GetObjectRequest.builder()
                .bucket(properties.bucket())
                .key(objectKey)
                .build();

        return s3Client.getObject(objectRequest);
    }

    @PreDestroy
    public void destroy() {
        if (s3Client != null) {
            s3Client.close();
        }

        if (s3Presigner != null) {
            s3Presigner.close();
        }
    }
}
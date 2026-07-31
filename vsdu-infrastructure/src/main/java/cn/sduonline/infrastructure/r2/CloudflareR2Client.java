package cn.sduonline.infrastructure.r2;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;

@Slf4j
public class CloudflareR2Client {

    private final CloudflareR2Properties properties;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    public CloudflareR2Client(CloudflareR2Properties properties) throws S3Exception {
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

    public void upload(
            String objectKey,
            InputStream inputStream,
            long size,
            String contentType
    ) throws S3Exception {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(objectKey)
                .contentType(contentType)
                .build();

        PutObjectResponse response = s3Client.putObject(
                request,
                RequestBody.fromInputStream(inputStream, size)
        );

        log.info(
                "上传文件到Cloudflare R2：key={} eTag={}",
                objectKey, response.eTag()
        );
    }

    public void delete(String objectKey) {
        s3Client.deleteObject(
                DeleteObjectRequest.builder()
                        .bucket(properties.bucket())
                        .key(objectKey)
                        .build()
        );

        log.info(
                "删除文件自Cloudflare R2：key={}",
                objectKey
        );
    }

    public String generatePresignedUrl(String objectKey) throws S3Exception {
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

    @PreDestroy
    public void destroy() {
        if (s3Client != null) s3Client.close();
    }
}

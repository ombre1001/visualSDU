package cn.sduonline.infrastructure.file.config;

import cn.sduonline.infrastructure.cdn.AliyunCdnClient;
import cn.sduonline.infrastructure.file.image.ImageFileUpload;
import cn.sduonline.infrastructure.file.storage.CloudflareR2WithAliyunCdnFileStorage;
import cn.sduonline.infrastructure.file.storage.FileStorage;
import cn.sduonline.infrastructure.r2.CloudflareR2Client;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FileConfig {

    @Bean
    public FileStorage cloudflareR2FileStorage(
            CloudflareR2Client cloudflareR2Client,
            AliyunCdnClient aliyunCdnClient
    ) {
        return new CloudflareR2WithAliyunCdnFileStorage(cloudflareR2Client, aliyunCdnClient);
    }

    @Bean
    public ImageFileUpload imageFileUpload(FileStorage fileStorage) {
        return new ImageFileUpload(fileStorage);
    }
}

package cn.sduonline.infrastructure.file.config;

import cn.sduonline.infrastructure.file.image.ImageFileUpload;
import cn.sduonline.infrastructure.file.storage.CloudflareR2FileStorage;
import cn.sduonline.infrastructure.file.storage.FileStorage;
import cn.sduonline.infrastructure.r2.CloudflareR2Client;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FileConfig {

    @Bean
    public FileStorage cloudflareR2FileStorage(CloudflareR2Client cloudflareR2Client) {
        return new CloudflareR2FileStorage(cloudflareR2Client);
    }

    @Bean
    public ImageFileUpload imageFileUpload(FileStorage fileStorage) {
        return new ImageFileUpload(fileStorage);
    }
}

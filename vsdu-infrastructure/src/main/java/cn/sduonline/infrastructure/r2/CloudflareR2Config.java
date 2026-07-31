package cn.sduonline.infrastructure.r2;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CloudflareR2Properties.class)
public class CloudflareR2Config {

    @Bean
    public CloudflareR2Client cloudflareR2Client(CloudflareR2Properties properties) {
        return new CloudflareR2Client(properties);
    }
}

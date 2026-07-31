package cn.sduonline.infrastructure.r2;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("cloudflare.r2")
public record CloudflareR2Properties(
        String endpoint,
        String accessKeyId,
        String secretAccessKey,
        String bucket
) {
}

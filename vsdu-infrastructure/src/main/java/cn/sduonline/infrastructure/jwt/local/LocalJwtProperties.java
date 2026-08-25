package cn.sduonline.infrastructure.jwt.local;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("vsdu.jwt.local")
public record LocalJwtProperties(
        Long accessTokenExpireSeconds,
        Long refreshTokenExpireSeconds,
        Long loginTicketExpireSeconds,
        String localSecret
) {
}

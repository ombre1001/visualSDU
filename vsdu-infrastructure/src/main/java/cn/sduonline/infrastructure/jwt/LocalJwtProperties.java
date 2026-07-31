package cn.sduonline.infrastructure.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("vsdu.jwt.local")
public record LocalJwtProperties(

) {
}

package cn.sduonline.infrastructure.jwt.sdupass;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("vsdu.jwt.sdupass")
public record SduPassJwtProperties(
        String sduPassSecret,
        String salt,
        Integer iterationCound,
        Integer keyLength
) {
}

package cn.sduonline.infrastructure.jwt;

import cn.sduonline.infrastructure.jwt.sdupass.SduPassClient;
import cn.sduonline.infrastructure.jwt.sdupass.SduPassJwtProperties;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.ObjectMapper;

@SpringBootConfiguration
@EnableConfigurationProperties({
        LocalJwtProperties.class, SduPassJwtProperties.class
})
public class JwtConfiguration {

    @Bean
    public SduPassClient sduPassClient(ObjectMapper objectMapper) {
        return new SduPassClient(objectMapper);
    }

    @Bean
    public JwtTokenUtils jwtTokenUtils(
            SduPassJwtProperties sduPassJwtProperties,
            LocalJwtProperties localJwtProperties
    ) throws Exception {
        return new JwtTokenUtils(
                sduPassJwtProperties, localJwtProperties
        );
    }
}

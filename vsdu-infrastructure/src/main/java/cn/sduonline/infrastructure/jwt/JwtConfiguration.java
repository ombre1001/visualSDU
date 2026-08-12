package cn.sduonline.infrastructure.jwt;

import cn.sduonline.infrastructure.jwt.local.LocalJwtProperties;
import cn.sduonline.infrastructure.jwt.local.TokenRedisOperator;
import cn.sduonline.infrastructure.jwt.sdupass.SduPassClient;
import cn.sduonline.infrastructure.jwt.sdupass.SduPassJwtProperties;
import cn.sduonline.infrastructure.redis.RedisClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import tools.jackson.databind.ObjectMapper;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({
        LocalJwtProperties.class,
        SduPassJwtProperties.class
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
                sduPassJwtProperties,
                localJwtProperties
        );
    }

    @Bean
    public TokenRedisOperator tokenRedisOperator(
            RedisClient redisClient,
            LocalJwtProperties localJwtProperties
    ) {
        return new TokenRedisOperator(
                redisClient, localJwtProperties
        );
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
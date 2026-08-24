package cn.sduonline.business.security.filter;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class WebMvcConfiguration implements WebMvcConfigurer {

    private final AuthorizationInterceptor authorizationInterceptor;
    private final LogInterceptor logInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(logInterceptor)
                .addPathPatterns("/**")
                .order(0);
        registry.addInterceptor(authorizationInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/error")
                .order(1);
    }
}
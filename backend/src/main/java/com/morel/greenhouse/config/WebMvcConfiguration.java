package com.morel.greenhouse.config;

import com.morel.greenhouse.shared.security.ApiSecurityInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {
    private final ApiSecurityInterceptor apiSecurityInterceptor;

    public WebMvcConfiguration(ApiSecurityInterceptor apiSecurityInterceptor) {
        this.apiSecurityInterceptor = apiSecurityInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiSecurityInterceptor).addPathPatterns("/api/v1/**");
    }
}

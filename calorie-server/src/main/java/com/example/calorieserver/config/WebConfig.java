package com.example.calorieserver.config;

import com.example.calorieserver.security.OwnershipGuardInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置：注册归属校验拦截器。
 * 注册/登录接口无需校验；onboarding 与其余 /api/** 均校验。
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final OwnershipGuardInterceptor ownershipGuardInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(ownershipGuardInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/register", "/api/auth/login",
                        "/api/auth/forgot-password", "/api/auth/reset-password", "/error");
    }
}

package com.example.calorieserver.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger UI 配置：接口文档元信息 + JWT Bearer 安全方案。
 * 除注册/登录外所有接口默认带锁，在 Swagger UI 右上角 Authorize 填入 token 后即可直接试调。
 */
@Configuration
public class OpenApiConfig {

    private static final String SCHEME_NAME = "bearer-jwt";

    @Bean
    public OpenAPI calorieTrackerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("卡路里追踪 API")
                        .description("神月健康 · 卡路里追踪应用后端接口。除注册/登录外均需在右上角 Authorize 填入 JWT token。")
                        .version("v1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList(SCHEME_NAME))
                .components(new Components().addSecuritySchemes(SCHEME_NAME,
                        new SecurityScheme()
                                .name(SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}

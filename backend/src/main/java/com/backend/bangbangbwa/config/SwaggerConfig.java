package com.backend.bangbangbwa.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    private static final String SECURITY_SCHEME_NAME =
            "Bearer Authentication";

    @Bean
    public OpenAPI openAPI() {
        SecurityScheme securityScheme =
                new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .name("Authorization");

        SecurityRequirement securityRequirement =
                new SecurityRequirement()
                        .addList(SECURITY_SCHEME_NAME);

        return new OpenAPI()
                .info(new Info()
                        .title("BangBangBwa API")
                        .description("세입자 매물 라이브 서비스 API 명세서")
                        .version("v1"))
                .components(new Components()
                        .addSecuritySchemes(
                                SECURITY_SCHEME_NAME,
                                securityScheme
                        ))
                .addSecurityItem(securityRequirement);
    }
}
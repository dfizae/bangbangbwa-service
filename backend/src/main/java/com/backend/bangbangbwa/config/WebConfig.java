package com.backend.bangbangbwa.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                        "http://localhost:5173",
                        "http://3.35.25.253",
                        "https://i15a504.p.ssafy.io",
                        "https://i15a504.p.ssafy.io:8443"
                )
                .allowedMethods("GET", "POST", "PUT","PATCH", "DELETE")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
package com.library.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI libraryOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Library Management API")
                        .description("Backend hệ thống quản lý thư viện (SPEC). B0 — nền tảng.")
                        .version("0.0.1")
                        .contact(new Contact().name("Library Team")));
    }
}

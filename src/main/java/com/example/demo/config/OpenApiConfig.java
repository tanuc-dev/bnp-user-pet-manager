package com.example.demo.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI apiInfo() {
        return new OpenAPI().info(new Info()
            .title("User & Pet Management API")
            .description("Spring Boot 3 + Java 21 assessment sample for managing users, pets, and ownerships with flexible queries.")
            .version("1.0.0")
        );
    }

    // Optional: Group endpoints if your project grows
    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public")
                .pathsToMatch("/users/**", "/pets/**", "/addresses/**", "/ownerships/**")
                .build();
    }
}

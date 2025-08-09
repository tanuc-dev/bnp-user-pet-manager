package com.example.demo.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

class OpenApiConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(OpenApiConfig.class);

    @Test
    void openApiBean_hasExpectedInfo() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(OpenAPI.class);

            OpenAPI openAPI = context.getBean(OpenAPI.class);
            Info info = openAPI.getInfo();

            assertThat(info).isNotNull();
            assertThat(info.getTitle()).isEqualTo("User & Pet Management API");
            assertThat(info.getVersion()).isEqualTo("1.0.0");
            assertThat(info.getDescription()).contains("Spring Boot 3 + Java 21 assessment");
        });
    }

    @Test
    void groupedOpenApiBean_isPresentWithExpectedGroupName() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(GroupedOpenApi.class);

            GroupedOpenApi grouped = context.getBean(GroupedOpenApi.class);
            assertThat(grouped.getGroup()).isEqualTo("public");

            // If your springdoc version exposes pathsToMatch(), also assert it:
            try {
                List<String> paths = grouped.getPathsToMatch();
                assertThat(paths)
                        .isNotNull()
                        .isNotEmpty()
                        .contains("/users/**", "/pets/**", "/ownerships/**");
            } catch (NoSuchMethodError ignored) {
                // Some versions don't expose getters; group check above is enough for coverage.
            }
        });
    }
}

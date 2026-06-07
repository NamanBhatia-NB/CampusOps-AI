package com.campusops.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger configuration for CampusOps AI REST APIs.
 * <p>
 * Access the Swagger UI at {@code /swagger-ui.html} and the raw spec at
 * {@code /api-docs}.
 * </p>
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "CampusOps AI API",
                version = "1.0",
                description = "AI-powered CRM and Operations Intelligence Platform for Educational Institutions",
                contact = @Contact(name = "CampusOps Team", email = "support@campusops.com"),
                license = @License(name = "MIT")
        ),
        servers = @Server(url = "/", description = "Default Server"),
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Provide a valid JWT token",
        in = SecuritySchemeIn.HEADER
)
public class SwaggerConfig {

    /**
     * Groups all REST endpoints under {@code /api/**} into a single Swagger group.
     */
    @Bean
    public GroupedOpenApi allApiGroup() {
        return GroupedOpenApi.builder()
                .group("all-apis")
                .pathsToMatch("/api/**")
                .build();
    }

    /**
     * Groups admin-only endpoints for convenient browsing.
     */
    @Bean
    public GroupedOpenApi adminApiGroup() {
        return GroupedOpenApi.builder()
                .group("admin-apis")
                .pathsToMatch("/api/admin/**")
                .build();
    }
}

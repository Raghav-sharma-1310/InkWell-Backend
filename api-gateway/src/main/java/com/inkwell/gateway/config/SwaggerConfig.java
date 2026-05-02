/*
 * Codex documentation pass: this source file contains Spring Boot configuration for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.gateway.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.List;
import org.springdoc.core.properties.AbstractSwaggerUiConfigProperties.SwaggerUrl;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
/* This class groups swagger config behavior so the module keeps a clear responsibility. */
public class SwaggerConfig {

    @Bean
    @Lazy(false)
    // Defines swagger urls so related behavior stays grouped in one place.
    public List<SwaggerUrl> swaggerUrls(SwaggerUiConfigProperties swaggerUiConfigProperties) {
        List<SwaggerUrl> urls = List.of(
            createSwaggerUrl("auth-service", "/auth-service/v3/api-docs", "Auth Service"),
            createSwaggerUrl("post-service", "/post-service/v3/api-docs", "Post Service"),
            createSwaggerUrl("comment-service", "/comment-service/v3/api-docs", "Comment Service"),
            createSwaggerUrl("category-service", "/category-service/v3/api-docs", "Category Service"),
            createSwaggerUrl("media-service", "/media-service/v3/api-docs", "Media Service"),
            createSwaggerUrl("newsletter-service", "/newsletter-service/v3/api-docs", "Newsletter Service"),
            createSwaggerUrl("notification-service", "/notification-service/v3/api-docs", "Notification Service"),
            createSwaggerUrl("payment-service", "/payment-service/v3/api-docs", "Payment Service")
        );
        swaggerUiConfigProperties.setUrls(new java.util.HashSet<>(urls));
        return urls;
    }

    @Bean
    // Defines inkwell open api so related behavior stays grouped in one place.
    public OpenAPI inkwellOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("InkWell Platform API")
                .description("Centralized API documentation for the InkWell Blogging Platform microservices")
                .version("1.0.0")
                .contact(new Contact()
                    .name("InkWell Team")
                    .email("support@inkwell.dev"))
                .license(new License()
                    .name("MIT License")
                    .url("https://opensource.org/licenses/MIT")))
            .externalDocs(new ExternalDocumentation()
                .description("InkWell Platform Documentation")
                .url("https://docs.inkwell.dev"))
            .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
            .components(new Components()
                .addSecuritySchemes("Bearer Authentication",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Enter your JWT token")));
    }

    // Performs the create swagger url workflow so callers do not duplicate this logic.
    private SwaggerUrl createSwaggerUrl(String name, String url, String displayName) {
        SwaggerUrl swaggerUrl = new SwaggerUrl();
        swaggerUrl.setName(name);
        swaggerUrl.setUrl(url);
        swaggerUrl.setDisplayName(displayName);
        return swaggerUrl;
    }
}

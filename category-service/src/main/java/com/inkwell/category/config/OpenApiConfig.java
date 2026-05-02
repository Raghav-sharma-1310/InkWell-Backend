/*
 * This source file contains Spring Boot configuration for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.category.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
/* This class groups open api config behavior so the module keeps a clear responsibility. */
public class OpenApiConfig {
    @Bean
    // Defines category open api so related behavior stays grouped in one place.
    public OpenAPI categoryOpenApi() {
        return new OpenAPI().info(new Info().title("InkWell Category Service").version("1.0.0").description("Category, tag, and taxonomy mapping APIs"));
    }
}

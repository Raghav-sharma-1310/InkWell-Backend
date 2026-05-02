/*
 * This source file contains Spring Boot configuration for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.media.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
/* This class groups app config behavior so the module keeps a clear responsibility. */
public class AppConfig {

    @Bean
    // Defines media open api so related behavior stays grouped in one place.
    public OpenAPI mediaOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("InkWell Media Service")
                        .version("1.0.0")
                        .description("Media upload, linking, and library APIs"));
    }

    @Bean
    @ConditionalOnProperty(name = "app.storage.mode", havingValue = "s3")
    // Defines s3 client so related behavior stays grouped in one place.
    public S3Client s3Client(Environment env) {
        String region = env.getProperty("app.storage.s3.region", "us-east-1");
        String accessKey = env.getProperty("app.storage.s3.access-key");
        String secretKey = env.getProperty("app.storage.s3.secret-key");

        if (!StringUtils.hasText(accessKey)) {
            throw new IllegalStateException(
                    "AWS access key is missing. Set app.storage.s3.access-key or AWS_ACCESS_KEY_ID"
            );
        }

        if (!StringUtils.hasText(secretKey)) {
            throw new IllegalStateException(
                    "AWS secret key is missing. Set app.storage.s3.secret-key or AWS_SECRET_ACCESS_KEY"
            );
        }

        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(accessKey, secretKey)
                        )
                )
                .build();
    }
}

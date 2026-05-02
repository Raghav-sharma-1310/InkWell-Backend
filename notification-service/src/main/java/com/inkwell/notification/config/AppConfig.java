/*
 * This source file contains Spring Boot configuration for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.notification.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;

@Configuration
/* This class groups app config behavior so the module keeps a clear responsibility. */
public class AppConfig {

    @Bean
    // Defines jackson2 json message converter so related behavior stays grouped in one place.
    public MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    // Defines open api so related behavior stays grouped in one place.
    public OpenAPI openAPI() {
        return new OpenAPI().info(
                new Info()
                        .title("InkWell Notification Service")
                        .version("1.0.0")
                        .description("Notification center, broadcasts, and audit logs")
        );
    }

    @Bean
    // Defines inkwell exchange so related behavior stays grouped in one place.
    public DirectExchange inkwellExchange() {
        return new DirectExchange("inkwell.exchange");
    }

    @Bean
    // Defines comment notification queue so related behavior stays grouped in one place.
    public Queue commentNotificationQueue() {
        return new Queue("comment-notification-queue", true);
    }

    @Bean
    // Defines reply notification queue so related behavior stays grouped in one place.
    public Queue replyNotificationQueue() {
        return new Queue("reply-notification-queue", true);
    }

    @Bean
    // Defines post published notification queue so related behavior stays grouped in one place.
    public Queue postPublishedNotificationQueue() {
        return new Queue("post-published-notification-queue", true);
    }

    @Bean
    public Binding commentBinding(
            DirectExchange inkwellExchange,
            @Qualifier("commentNotificationQueue") Queue commentNotificationQueue) {
        return BindingBuilder.bind(commentNotificationQueue)
                .to(inkwellExchange)
                .with("comment.created");
    }

    @Bean
    public Binding replyBinding(
            DirectExchange inkwellExchange,
            @Qualifier("replyNotificationQueue") Queue replyNotificationQueue) {
        return BindingBuilder.bind(replyNotificationQueue)
                .to(inkwellExchange)
                .with("comment.reply");
    }

    @Bean
    public Binding postBinding(
            DirectExchange inkwellExchange,
            @Qualifier("postPublishedNotificationQueue") Queue postPublishedNotificationQueue) {
        return BindingBuilder.bind(postPublishedNotificationQueue)
                .to(inkwellExchange)
                .with("post.published");
    }
}
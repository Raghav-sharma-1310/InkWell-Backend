/*
 * This source file contains Spring Boot configuration for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.comment.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
/* This class groups app config behavior so the module keeps a clear responsibility. */
public class AppConfig {
    @Bean
    // Defines jackson2 json message converter so related behavior stays grouped in one place.
    public MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    // Defines comment open api so related behavior stays grouped in one place.
    public OpenAPI commentOpenApi() { return new OpenAPI().info(new Info().title("InkWell Comment Service").version("1.0.0").description("Comment, reply, like, and moderation APIs")); }
    @Bean
    // Defines inkwell exchange so related behavior stays grouped in one place.
    public DirectExchange inkwellExchange() { return new DirectExchange("inkwell.exchange"); }
    @Bean
    // Performs the post deleted queue workflow so callers do not duplicate this logic.
    public Queue postDeletedQueue() { return new Queue("post-deleted-queue", true); }
    @Bean
    // Performs the post deleted binding workflow so callers do not duplicate this logic.
    public Binding postDeletedBinding(DirectExchange inkwellExchange, Queue postDeletedQueue) { return BindingBuilder.bind(postDeletedQueue).to(inkwellExchange).with("post.deleted"); }
}


/*
 * This source file contains Spring Boot configuration for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.newsletter.config;

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

    @Bean public OpenAPI openAPI() { return new OpenAPI().info(new Info().title("InkWell Newsletter Service").version("1.0.0").description("Newsletter subscriptions, confirmations, and campaigns")); }
    @Bean public DirectExchange inkwellExchange() { return new DirectExchange("inkwell.exchange"); }
    @Bean public Queue postPublishedNewsletterQueue() { return new Queue("post-published-newsletter-queue", true); }
    @Bean public Binding postPublishedNewsletterBinding(DirectExchange inkwellExchange, Queue postPublishedNewsletterQueue) { return BindingBuilder.bind(postPublishedNewsletterQueue).to(inkwellExchange).with("post.published"); }
}


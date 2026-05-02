/*
 * This source file contains Spring Boot configuration for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.post.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
/* This class groups rabbit config behavior so the module keeps a clear responsibility. */
public class RabbitConfig {

    @Bean
    // Defines jackson2 json message converter so related behavior stays grouped in one place.
    public MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    // Defines inkwell exchange so related behavior stays grouped in one place.
    public DirectExchange inkwellExchange() {
        return new DirectExchange("inkwell.exchange");
    }

    @Bean
    // Defines post published queue so related behavior stays grouped in one place.
    public Queue postPublishedQueue() {
        return new Queue("post-published-queue", true);
    }

    @Bean
    // Defines post published binding so related behavior stays grouped in one place.
    public Binding postPublishedBinding(DirectExchange inkwellExchange, Queue postPublishedQueue) {
        return BindingBuilder.bind(postPublishedQueue).to(inkwellExchange).with("post.published");
    }
}

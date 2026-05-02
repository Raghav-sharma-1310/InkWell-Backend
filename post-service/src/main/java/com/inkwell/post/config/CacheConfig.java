/*
 * This source file contains Spring Boot configuration for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.post.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

@Slf4j
@Configuration
/* This class groups cache config behavior so the module keeps a clear responsibility. */
public class CacheConfig {

    @Bean
    @Primary
    // Defines cache manager so related behavior stays grouped in one place.
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        try {
            connectionFactory.getConnection().ping();
            log.info("Redis is available — using RedisCacheManager");

            // ObjectMapper with Java 8 date/time support for Redis serialization
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            mapper.activateDefaultTyping(
                mapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL
            );

            RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeValuesWith(
                    RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer(mapper))
                );
            return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
        } catch (Exception ex) {
            log.warn("Redis not available ({}), falling back to in-memory cache", ex.getMessage());
            return new ConcurrentMapCacheManager("published-feed");
        }
    }
}

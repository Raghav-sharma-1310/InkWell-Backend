/*
 * This source file contains business workflow and validation logic for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.service;

import com.inkwell.auth.exception.TooManyRequestsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
/* This class groups login rate limiter test behavior so the module keeps a clear responsibility. */
class LoginRateLimiterTest {

    @Mock private StringRedisTemplate redis;
    @Mock private ValueOperations<String, String> valueOps;

    @InjectMocks private LoginRateLimiter rateLimiter;

    @Test
    @DisplayName("Should allow first login attempt")
    void firstAttempt() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment("login:attempts:test@inkwell.com")).thenReturn(1L);

        assertThatCode(() -> rateLimiter.checkAndIncrement("test@inkwell.com"))
                .doesNotThrowAnyException();
        verify(redis).expire(eq("login:attempts:test@inkwell.com"), any(Duration.class));
    }

    @Test
    @DisplayName("Should allow up to MAX_ATTEMPTS")
    void underLimit() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment("login:attempts:test@inkwell.com")).thenReturn(5L);

        assertThatCode(() -> rateLimiter.checkAndIncrement("test@inkwell.com"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should block when exceeding MAX_ATTEMPTS")
    void overLimit() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment("login:attempts:test@inkwell.com")).thenReturn(6L);

        assertThatThrownBy(() -> rateLimiter.checkAndIncrement("test@inkwell.com"))
                .isInstanceOf(TooManyRequestsException.class)
                .hasMessageContaining("Too many login attempts");
    }

    @Test
    @DisplayName("Should reset attempts on successful login")
    void resetOnSuccess() {
        rateLimiter.resetOnSuccess("test@inkwell.com");
        verify(redis).delete("login:attempts:test@inkwell.com");
    }

    @Test
    @DisplayName("Should normalize email to lowercase")
    void caseInsensitive() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment("login:attempts:test@inkwell.com")).thenReturn(1L);

        rateLimiter.checkAndIncrement("TEST@INKWELL.COM");

        verify(valueOps).increment("login:attempts:test@inkwell.com");
    }
}

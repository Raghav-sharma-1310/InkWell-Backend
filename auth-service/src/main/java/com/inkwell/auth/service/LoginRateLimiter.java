/*
 * Codex documentation pass: this source file contains business workflow and validation logic for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.service;

import com.inkwell.auth.exception.TooManyRequestsException;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
/* This class groups login rate limiter behavior so the module keeps a clear responsibility. */
public class LoginRateLimiter {

    private final StringRedisTemplate redis;
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    // Defines check and increment so related behavior stays grouped in one place.
    public void checkAndIncrement(String email) {
        String key = "login:attempts:" + email.toLowerCase();
        Long attempts = redis.opsForValue().increment(key);
        if (attempts != null && attempts == 1) {
            redis.expire(key, WINDOW);
        }
        if (attempts != null && attempts > MAX_ATTEMPTS) {
            throw new TooManyRequestsException("Too many login attempts. Try again in 15 minutes.");
        }
    }

    // Defines reset on success so related behavior stays grouped in one place.
    public void resetOnSuccess(String email) {
        redis.delete("login:attempts:" + email.toLowerCase());
    }
}

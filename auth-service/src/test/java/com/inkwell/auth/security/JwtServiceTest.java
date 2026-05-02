/*
 * Codex documentation pass: this source file contains authentication and authorization support for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.security;

import com.inkwell.auth.entity.User;
import com.inkwell.auth.enumtype.Role;
import com.inkwell.auth.enumtype.SubscriptionTier;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/* This class groups jwt service test behavior so the module keeps a clear responsibility. */
class JwtServiceTest {

    private JwtService jwtService;
    private final String secret = Base64.getEncoder().encodeToString("a-very-long-secret-key-that-is-at-least-32-bytes-long".getBytes());
    private User testUser;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(secret, 60);
        testUser = User.builder()
                .userId(UUID.randomUUID())
                .username("testuser")
                .email("test@inkwell.com")
                .role(Role.READER)
                .subscriptionTier(SubscriptionTier.FREE)
                .build();
    }

    @Test
    @DisplayName("Should generate and parse valid token")
    void generateAndParseToken() {
        String token = jwtService.generateAccessToken(testUser);
        assertThat(token).isNotEmpty();

        Claims claims = jwtService.parseToken(token);
        assertThat(claims.getSubject()).isEqualTo("test@inkwell.com");
        assertThat(claims).containsEntry("userId", testUser.getUserId().toString())
                          .containsEntry("role", "READER");
    }
}

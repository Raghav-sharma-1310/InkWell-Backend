/*
 * This source file contains business workflow and validation logic for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.service;

import com.inkwell.auth.entity.RefreshToken;
import com.inkwell.auth.entity.User;
import com.inkwell.auth.exception.BadRequestException;
import com.inkwell.auth.repository.RefreshTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
/* This class groups refresh token service test behavior so the module keeps a clear responsibility. */
class RefreshTokenServiceTest {

    @Mock private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks private RefreshTokenService refreshTokenService;

    // Verifies test user so regressions are caught during automated tests.
    private User testUser() {
        return User.builder().username("testuser").email("test@inkwell.com").build();
    }

    @Test
    @DisplayName("Should create refresh token for user")
    void createForUser() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshDays", 14L);
        User user = testUser();
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        RefreshToken token = refreshTokenService.createForUser(user);

        assertThat(token.getToken()).isNotBlank();
        assertThat(token.getUser()).isEqualTo(user);
        assertThat(token.getExpiresAt()).isAfter(LocalDateTime.now());
        verify(refreshTokenRepository).deleteByUser(user);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Should verify valid refresh token")
    void verifyValid() {
        RefreshToken token = RefreshToken.builder()
                .token("valid-token")
                .user(testUser())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        when(refreshTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));

        RefreshToken result = refreshTokenService.verify("valid-token");

        assertThat(result.getToken()).isEqualTo("valid-token");
    }

    @Test
    @DisplayName("Should reject expired refresh token")
    void verifyExpired() {
        RefreshToken token = RefreshToken.builder()
                .token("expired-token")
                .user(testUser())
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build();
        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> refreshTokenService.verify("expired-token"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("Should reject unknown refresh token")
    void verifyNotFound() {
        when(refreshTokenRepository.findByToken("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.verify("unknown"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("Should revoke single token")
    void revoke() {
        refreshTokenService.revoke("some-token");
        verify(refreshTokenRepository).deleteByToken("some-token");
    }

    @Test
    @DisplayName("Should revoke all tokens for user")
    void revokeAll() {
        User user = testUser();
        refreshTokenService.revokeAll(user);
        verify(refreshTokenRepository).deleteByUser(user);
    }
}

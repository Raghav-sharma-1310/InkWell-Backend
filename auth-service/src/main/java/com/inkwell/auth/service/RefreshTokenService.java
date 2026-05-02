/*
 * Codex documentation pass: this source file contains business workflow and validation logic for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.service;

import com.inkwell.auth.entity.RefreshToken;
import com.inkwell.auth.entity.User;
import com.inkwell.auth.exception.BadRequestException;
import com.inkwell.auth.repository.RefreshTokenRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
/* This class groups refresh token service behavior so the module keeps a clear responsibility. */
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${security.jwt.refresh-days:14}")
    private long refreshDays;

    @Transactional
    // Performs the create for user workflow so callers do not duplicate this logic.
    public RefreshToken createForUser(User user) {
        refreshTokenRepository.deleteByUser(user);
        RefreshToken refreshToken = RefreshToken.builder()
            .token(UUID.randomUUID().toString())
            .user(user)
            .expiresAt(LocalDateTime.now().plusDays(refreshDays))
            .build();
        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional(readOnly = true)
    // Performs the verify workflow so callers do not duplicate this logic.
    public RefreshToken verify(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
            .orElseThrow(() -> new BadRequestException("Refresh token not found"));
        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Refresh token expired");
        }
        return refreshToken;
    }

    @Transactional
    // Defines revoke so related behavior stays grouped in one place.
    public void revoke(String token) {
        refreshTokenRepository.deleteByToken(token);
    }

    @Transactional
    // Defines revoke all so related behavior stays grouped in one place.
    public void revokeAll(User user) {
        refreshTokenRepository.deleteByUser(user);
    }
}

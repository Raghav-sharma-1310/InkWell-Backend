/*
 * Codex documentation pass: this source file contains business workflow and validation logic for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.service;

import com.inkwell.auth.entity.PasswordOtp;
import com.inkwell.auth.exception.BadRequestException;
import com.inkwell.auth.exception.ResourceNotFoundException;
import com.inkwell.auth.repository.PasswordOtpRepository;
import com.inkwell.auth.repository.UserRepository;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
/* This class groups otp service behavior so the module keeps a clear responsibility. */
public class OtpService {

    private final PasswordOtpRepository otpRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Value("${app.otp.expiry-minutes:10}")
    private int expiryMinutes;

    @Transactional
    // Performs the generate and send workflow so callers do not duplicate this logic.
    public void generateAndSend(String email) {
        if (!userRepository.existsByEmailIgnoreCase(email.trim())) {
            throw new ResourceNotFoundException("No account found with this email");
        }
        otpRepository.deleteByEmail(email.trim().toLowerCase());

        String otp = String.format("%06d", RANDOM.nextInt(1_000_000));
        PasswordOtp entity = PasswordOtp.builder()
            .email(email.trim().toLowerCase())
            .otp(otp)
            .expiresAt(LocalDateTime.now().plusMinutes(expiryMinutes))
            .verified(false)
            .build();
        otpRepository.save(entity);

        emailService.sendOtpEmail(email.trim(), otp, expiryMinutes);
        log.info("OTP sent to {}", email);
    }

    @Transactional
    // Performs the verify workflow so callers do not duplicate this logic.
    public void verify(String email, String otp) {
        PasswordOtp entity = otpRepository.findTopByEmailIgnoreCaseOrderByCreatedAtDesc(email.trim())
            .orElseThrow(() -> new BadRequestException("No OTP found for this email. Please request a new one."));
        if (entity.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("OTP has expired. Please request a new one.");
        }
        if (!entity.getOtp().equals(otp.trim())) {
            throw new BadRequestException("Invalid OTP. Please check and try again.");
        }
        entity.setVerified(true);
        otpRepository.save(entity);
    }

    @Transactional(readOnly = true)
    // Defines ensure verified so related behavior stays grouped in one place.
    public void ensureVerified(String email) {
        PasswordOtp entity = otpRepository.findTopByEmailIgnoreCaseOrderByCreatedAtDesc(email.trim())
            .orElseThrow(() -> new BadRequestException("No verified OTP found. Please verify your OTP first."));
        if (!entity.isVerified()) {
            throw new BadRequestException("OTP not yet verified. Please verify your OTP first.");
        }
        if (entity.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("OTP session expired. Please start over.");
        }
    }

    @Transactional
    // Defines cleanup so related behavior stays grouped in one place.
    public void cleanup(String email) {
        otpRepository.deleteByEmail(email.trim().toLowerCase());
    }
}

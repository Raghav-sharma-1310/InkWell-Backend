/*
 * This source file contains business workflow and validation logic for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.service;

import com.inkwell.auth.entity.PasswordOtp;
import com.inkwell.auth.exception.BadRequestException;
import com.inkwell.auth.exception.ResourceNotFoundException;
import com.inkwell.auth.repository.PasswordOtpRepository;
import com.inkwell.auth.repository.UserRepository;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
/* This class groups otp service test behavior so the module keeps a clear responsibility. */
class OtpServiceTest {

    @Mock private PasswordOtpRepository otpRepository;
    @Mock private UserRepository userRepository;
    @Mock private EmailService emailService;

    @InjectMocks private OtpService otpService;

    @Test
    @DisplayName("Should generate and send OTP")
    void generateAndSend() {
        ReflectionTestUtils.setField(otpService, "expiryMinutes", 10);
        when(userRepository.existsByEmailIgnoreCase("test@inkwell.com")).thenReturn(true);

        otpService.generateAndSend("test@inkwell.com");

        verify(otpRepository).deleteByEmail("test@inkwell.com");
        verify(otpRepository).save(any(PasswordOtp.class));
        verify(emailService).sendOtpEmail(eq("test@inkwell.com"), anyString(), eq(10));
    }

    @Test
    @DisplayName("Should throw when user not found for OTP")
    void generateNotFound() {
        when(userRepository.existsByEmailIgnoreCase("unknown@inkwell.com")).thenReturn(false);

        assertThatThrownBy(() -> otpService.generateAndSend("unknown@inkwell.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No account found");
    }

    @Test
    @DisplayName("Should verify valid OTP")
    void verifyValid() {
        PasswordOtp otp = PasswordOtp.builder()
                .email("test@inkwell.com")
                .otp("123456")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .verified(false)
                .build();
        when(otpRepository.findTopByEmailIgnoreCaseOrderByCreatedAtDesc("test@inkwell.com"))
                .thenReturn(Optional.of(otp));

        otpService.verify("test@inkwell.com", "123456");

        assertThat(otp.isVerified()).isTrue();
        verify(otpRepository).save(otp);
    }

    @Test
    @DisplayName("Should reject expired OTP")
    void verifyExpired() {
        PasswordOtp otp = PasswordOtp.builder()
                .email("test@inkwell.com")
                .otp("123456")
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .verified(false)
                .build();
        when(otpRepository.findTopByEmailIgnoreCaseOrderByCreatedAtDesc("test@inkwell.com"))
                .thenReturn(Optional.of(otp));

        assertThatThrownBy(() -> otpService.verify("test@inkwell.com", "123456"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("Should reject invalid OTP")
    void verifyInvalid() {
        PasswordOtp otp = PasswordOtp.builder()
                .email("test@inkwell.com")
                .otp("123456")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .verified(false)
                .build();
        when(otpRepository.findTopByEmailIgnoreCaseOrderByCreatedAtDesc("test@inkwell.com"))
                .thenReturn(Optional.of(otp));

        assertThatThrownBy(() -> otpService.verify("test@inkwell.com", "999999"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid OTP");
    }

    @Test
    @DisplayName("Should reject when no OTP found")
    void verifyNoOtp() {
        when(otpRepository.findTopByEmailIgnoreCaseOrderByCreatedAtDesc("test@inkwell.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> otpService.verify("test@inkwell.com", "123456"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Should ensure verified OTP is valid")
    void ensureVerifiedSuccess() {
        PasswordOtp otp = PasswordOtp.builder()
                .email("test@inkwell.com")
                .otp("123456")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .verified(true)
                .build();
        when(otpRepository.findTopByEmailIgnoreCaseOrderByCreatedAtDesc("test@inkwell.com"))
                .thenReturn(Optional.of(otp));

        assertThatCode(() -> otpService.ensureVerified("test@inkwell.com")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should reject unverified OTP")
    void ensureVerifiedNotVerified() {
        PasswordOtp otp = PasswordOtp.builder()
                .email("test@inkwell.com")
                .otp("123456")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .verified(false)
                .build();
        when(otpRepository.findTopByEmailIgnoreCaseOrderByCreatedAtDesc("test@inkwell.com"))
                .thenReturn(Optional.of(otp));

        assertThatThrownBy(() -> otpService.ensureVerified("test@inkwell.com"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not yet verified");
    }

    @Test
    @DisplayName("Should reject expired verified OTP session")
    void ensureVerifiedExpired() {
        PasswordOtp otp = PasswordOtp.builder()
                .email("test@inkwell.com")
                .otp("123456")
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .verified(true)
                .build();
        when(otpRepository.findTopByEmailIgnoreCaseOrderByCreatedAtDesc("test@inkwell.com"))
                .thenReturn(Optional.of(otp));

        assertThatThrownBy(() -> otpService.ensureVerified("test@inkwell.com"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("Should cleanup OTP records")
    void cleanup() {
        otpService.cleanup("test@inkwell.com");
        verify(otpRepository).deleteByEmail("test@inkwell.com");
    }
}

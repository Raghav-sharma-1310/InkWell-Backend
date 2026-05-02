/*
 * Codex documentation pass: this source file contains business workflow and validation logic for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.service;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
/* This class groups email service test behavior so the module keeps a clear responsibility. */
class EmailServiceTest {

    @Mock private JavaMailSender mailSender;
    @Mock private MimeMessage mimeMessage;
    @InjectMocks private EmailService emailService;

    // Defines setup defaults so related behavior stays grouped in one place.
    private void setupDefaults() {
        ReflectionTestUtils.setField(emailService, "fromAddress", "noreply@inkwell.dev");
        ReflectionTestUtils.setField(emailService, "fromName", "InkWell");
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    @Test
    @DisplayName("Should send OTP email")
    void sendOtpEmail() {
        setupDefaults();
        emailService.sendOtpEmail("test@inkwell.com", "123456", 10);
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should send welcome email for reader")
    void sendWelcomeEmailReader() {
        setupDefaults();
        emailService.sendWelcomeEmail("test@inkwell.com", "Test User", "READER");
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should send welcome email for author")
    void sendWelcomeEmailAuthor() {
        setupDefaults();
        emailService.sendWelcomeEmail("author@inkwell.com", "Author User", "AUTHOR");
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should send welcome email for admin")
    void sendWelcomeEmailAdmin() {
        setupDefaults();
        emailService.sendWelcomeEmail("admin@inkwell.com", "Admin User", "ADMIN");
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should send login notification email")
    void sendLoginNotificationEmail() {
        setupDefaults();
        emailService.sendLoginNotificationEmail("test@inkwell.com", "Test User", "Email / Password");
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should send payment success email for reader plan")
    void sendPaymentSuccessEmailReader() {
        setupDefaults();
        LocalDateTime now = LocalDateTime.now();
        emailService.sendPaymentSuccessEmail(new EmailService.PaymentEmailDetails("test@inkwell.com", "Test User", "Reader Pro", "Reader", BigDecimal.valueOf(499), now, now, now.plusYears(1)));
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should send payment success email for author plan")
    void sendPaymentSuccessEmailAuthor() {
        setupDefaults();
        LocalDateTime now = LocalDateTime.now();
        emailService.sendPaymentSuccessEmail(new EmailService.PaymentEmailDetails("test@inkwell.com", "Test User", "Author Plus", "Author", BigDecimal.valueOf(999), now, now, now.plusYears(1)));
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should handle email sending failure gracefully")
    void sendEmailFailureGraceful() {
        ReflectionTestUtils.setField(emailService, "fromAddress", "noreply@inkwell.dev");
        ReflectionTestUtils.setField(emailService, "fromName", "InkWell");
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("SMTP error"));

        // Should not throw
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> 
            emailService.sendOtpEmail("test@inkwell.com", "123456", 10)
        );
    }
}

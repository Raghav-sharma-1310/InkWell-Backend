/*
 * This source file contains business workflow and validation logic for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.notification.service;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
/* This class groups mail service test behavior so the module keeps a clear responsibility. */
class MailServiceTest {

    @Mock private JavaMailSender mailSender;
    @Mock private MimeMessage mimeMessage;

    @InjectMocks private MailService mailService;

    @Test
    @DisplayName("Should send notification email successfully")
    void sendSuccess() {
        ReflectionTestUtils.setField(mailService, "fromAddress", "no-reply@inkwell.dev");
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        mailService.send("user@inkwell.com", "New Comment", "<p>You have a new comment</p>");

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should handle send failure gracefully")
    void sendFailure() {
        ReflectionTestUtils.setField(mailService, "fromAddress", "no-reply@inkwell.dev");
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("SMTP error"));

        // Should not throw
        assertDoesNotThrow(() -> mailService.send("user@inkwell.com", "Test", "body"));
    }
}

/*
 * This source file contains business workflow and validation logic for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.newsletter.service;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
/* This class groups mail service test behavior so the module keeps a clear responsibility. */
class MailServiceTest {

    @Mock private JavaMailSender mailSender;
    @Mock private MimeMessage mimeMessage;

    @InjectMocks private MailService mailService;

    @Test
    @DisplayName("Should send email successfully")
    void sendSuccess() {
        ReflectionTestUtils.setField(mailService, "fromAddress", "no-reply@inkwell.dev");
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        mailService.send("reader@inkwell.com", "Test Subject", "<h1>Hello</h1>");

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should handle email failure gracefully")
    void sendFailure() {
        ReflectionTestUtils.setField(mailService, "fromAddress", "no-reply@inkwell.dev");
        when(mailSender.createMimeMessage()).thenThrow(new MailSendException("SMTP error"));

        org.junit.jupiter.api.Assertions.assertThrows(MailDeliveryException.class, () -> mailService.send("reader@inkwell.com", "Test", "body"));
    }
}

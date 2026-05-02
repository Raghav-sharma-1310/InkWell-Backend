/*
 * This source file contains business workflow and validation logic for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.newsletter.service;

import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

@Slf4j
@Service
@RequiredArgsConstructor
/* This class groups mail service behavior so the module keeps a clear responsibility. */
public class MailService {
    private final JavaMailSender mailSender;

    @Value("${app.mail.from:no-reply@inkwell.dev}")
    private String fromAddress;

    // Performs the send workflow so callers do not duplicate this logic.
    public void send(String to, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress, "InkWell Newsletter");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);
            mailSender.send(message);
            log.info("Email successfully sent to {}", to);
        } catch (MailException | MessagingException | UnsupportedEncodingException ex) {
            log.error("Failed to send email to {}: {}", to, ex.getMessage());
            throw new MailDeliveryException("Failed to send email. Please try again later.", ex);
        }
    }
}

/*
 * Codex documentation pass: this source file contains business workflow and validation logic for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
/* This class groups email service behavior so the module keeps a clear responsibility. */
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@inkwell.dev}")
    private String fromAddress;

    @Value("${app.mail.name:InkWell}")
    private String fromName;

    @Async
    // Performs the send otp email workflow so callers do not duplicate this logic.
    public void sendOtpEmail(String to, String otp, int expiryMinutes) {
        String subject = "InkWell — Your Password Reset Code";
        String html = """
            <div style="font-family:'Segoe UI',Arial,sans-serif;max-width:520px;margin:0 auto;padding:32px;background:#ffffff;border-radius:16px;border:1px solid #e2e8f0">
              <div style="text-align:center;margin-bottom:24px">
                <span style="display:inline-block;background:#0f766e;color:#fff;padding:8px 16px;border-radius:8px;font-weight:700;font-size:18px">InkWell</span>
              </div>
              <h2 style="margin:0 0 8px;color:#0f172a;font-size:22px">Password Reset</h2>
              <p style="color:#64748b;margin:0 0 24px;font-size:14px;line-height:1.6">
                Use the code below to reset your password. This code expires in <strong>%d minutes</strong>.
              </p>
              <div style="text-align:center;background:#f0fdfa;border:2px dashed #0f766e;border-radius:12px;padding:20px;margin-bottom:24px">
                <span style="font-size:36px;font-weight:700;letter-spacing:8px;color:#0f766e">%s</span>
              </div>
              <p style="color:#94a3b8;font-size:12px;margin:0;text-align:center">If you didn't request this, please ignore this email.</p>
            </div>
            """.formatted(expiryMinutes, otp);
        send(to, subject, html);
    }

    @Async
    // Performs the send welcome email workflow so callers do not duplicate this logic.
    public void sendWelcomeEmail(String to, String fullName, String role) {
        String subject = "Account created successfully - Welcome to InkWell!";
        String roleDescription = switch (role) {
            case "AUTHOR" -> "You can now craft engaging posts, manage rich media, and build your reader audience.";
            case "ADMIN" -> "You have been granted full platform management access.";
            default -> "You can now explore premium content, bookmark your favorite posts, and join vibrant discussions.";
        };
        
        String html = """
            <!DOCTYPE html>
            <html>
            <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="margin:0;padding:0;background-color:#f1f5f9;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;">
              <table width="100%%" border="0" cellspacing="0" cellpadding="0" style="background-color:#f1f5f9;padding:40px 20px;">
                <tr>
                  <td align="center">
                    <table width="100%%" max-width="600" border="0" cellspacing="0" cellpadding="0" style="max-width:600px;background-color:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.1);">
                      
                      <!-- Header -->
                      <tr>
                        <td style="background:linear-gradient(135deg, #0f766e 0%%, #064e3b 100%%);padding:40px 0;text-align:center;">
                          <h1 style="margin:0;color:#ffffff;font-size:32px;font-weight:800;letter-spacing:-1px;">InkWell</h1>
                          <p style="margin:10px 0 0 0;color:#ccfbf1;font-size:16px;font-weight:500;">Your Publishing Journey Begins</p>
                        </td>
                      </tr>
                      
                      <!-- Body -->
                      <tr>
                        <td style="padding:48px 40px;">
                          <h2 style="margin:0 0 20px 0;color:#0f172a;font-size:24px;font-weight:700;">Hello %s,</h2>
                          <p style="margin:0 0 16px 0;color:#475569;font-size:16px;line-height:1.6;">
                            Your account has been created successfully. Welcome to the InkWell community!
                          </p>
                          <p style="margin:0 0 32px 0;color:#475569;font-size:16px;line-height:1.6;">
                            You are registered as an <strong>%s</strong>. %s
                          </p>
                          
                          <table width="100%%" border="0" cellspacing="0" cellpadding="0">
                            <tr>
                              <td align="center">
                                <a href="http://localhost:5173/login" style="display:inline-block;background-color:#0f766e;color:#ffffff;font-size:16px;font-weight:600;text-decoration:none;padding:16px 36px;border-radius:12px;transition:background-color 0.3s ease;">Access Your Dashboard</a>
                              </td>
                            </tr>
                          </table>
                          
                          <p style="margin:32px 0 0 0;color:#64748b;font-size:14px;line-height:1.6;text-align:center;">
                            If you have any questions, feel free to reply to this email. We're here to help!
                          </p>
                        </td>
                      </tr>
                      
                      <!-- Footer -->
                      <tr>
                        <td style="background-color:#f8fafc;padding:32px 40px;text-align:center;border-top:1px solid #e2e8f0;">
                          <p style="margin:0 0 8px 0;color:#94a3b8;font-size:12px;">
                            © 2026 InkWell Publishing Platform. All rights reserved.
                          </p>
                          <p style="margin:0;color:#94a3b8;font-size:12px;">
                            This is an automated message, please do not reply directly.
                          </p>
                        </td>
                      </tr>
                      
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(fullName, role, roleDescription);
        send(to, subject, html);
    }
    @Async
    // Performs the send login notification email workflow so callers do not duplicate this logic.
    public void sendLoginNotificationEmail(String to, String fullName, String provider) {
        String subject = "InkWell — New Sign-In Detected";
        String loginTime = java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a"));
        String html = """
            <div style="font-family:'Segoe UI',Arial,sans-serif;max-width:520px;margin:0 auto;padding:32px;background:#ffffff;border-radius:16px;border:1px solid #e2e8f0">
              <div style="text-align:center;margin-bottom:24px">
                <span style="display:inline-block;background:#0f766e;color:#fff;padding:8px 16px;border-radius:8px;font-weight:700;font-size:18px">InkWell</span>
              </div>
              <h2 style="margin:0 0 8px;color:#0f172a;font-size:22px">Hello, %s!</h2>
              <p style="color:#64748b;margin:0 0 16px;font-size:14px;line-height:1.6">
                We noticed a new sign-in to your InkWell account.
              </p>
              <div style="background:#f8fafc;border-radius:12px;padding:16px;margin-bottom:24px">
                <table style="width:100%%;border-collapse:collapse;font-size:14px;color:#334155">
                  <tr><td style="padding:6px 0;color:#94a3b8">Time</td><td style="padding:6px 0;text-align:right;font-weight:600">%s</td></tr>
                  <tr><td style="padding:6px 0;color:#94a3b8">Method</td><td style="padding:6px 0;text-align:right;font-weight:600">%s</td></tr>
                </table>
              </div>
              <p style="color:#94a3b8;font-size:12px;margin:0;text-align:center">If this wasn't you, please change your password immediately.</p>
            </div>
            """.formatted(fullName, loginTime, provider);
        send(to, subject, html);
    }

    /**
     * Details for payment success emails.
     */
    public record PaymentEmailDetails(String to, String fullName, String planName, String planType,
                                       java.math.BigDecimal amount, java.time.LocalDateTime paymentDate,
                                       java.time.LocalDateTime startDate, java.time.LocalDateTime expiryDate) {}

    @Async
    // Performs the send payment success email workflow so callers do not duplicate this logic.
    public void sendPaymentSuccessEmail(PaymentEmailDetails details) {
        String subject = "Welcome to InkWell Pro";
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy");
        
        String benefitsHtml = details.planType().contains("Author")
            ? """
                <tr><td style="padding:10px 0;font-size:14px;color:#334155;border-bottom:1px solid #f1f5f9;">
                  <span style="color:#0f766e;font-weight:700;margin-right:8px;">✍️</span> <strong>Publish unlimited posts</strong> with rich media
                </td></tr>
                <tr><td style="padding:10px 0;font-size:14px;color:#334155;border-bottom:1px solid #f1f5f9;">
                  <span style="color:#0f766e;font-weight:700;margin-right:8px;">📊</span> <strong>Advanced analytics</strong> & reader insights
                </td></tr>
                <tr><td style="padding:10px 0;font-size:14px;color:#334155;border-bottom:1px solid #f1f5f9;">
                  <span style="color:#0f766e;font-weight:700;margin-right:8px;">🔖</span> <strong>Bookmarks & reading history</strong>
                </td></tr>
                <tr><td style="padding:10px 0;font-size:14px;color:#334155;">
                  <span style="color:#0f766e;font-weight:700;margin-right:8px;">🚀</span> <strong>Priority support</strong> & early access to features
                </td></tr>
              """
            : """
                <tr><td style="padding:10px 0;font-size:14px;color:#334155;border-bottom:1px solid #f1f5f9;">
                  <span style="color:#0f766e;font-weight:700;margin-right:8px;">🔖</span> <strong>Unlimited bookmarks</strong> — Save posts for later
                </td></tr>
                <tr><td style="padding:10px 0;font-size:14px;color:#334155;border-bottom:1px solid #f1f5f9;">
                  <span style="color:#0f766e;font-weight:700;margin-right:8px;">📚</span> <strong>Reading history</strong> — Track what you've read
                </td></tr>
                <tr><td style="padding:10px 0;font-size:14px;color:#334155;border-bottom:1px solid #f1f5f9;">
                  <span style="color:#0f766e;font-weight:700;margin-right:8px;">⭐</span> <strong>Premium content</strong> — Access exclusive articles
                </td></tr>
                <tr><td style="padding:10px 0;font-size:14px;color:#334155;">
                  <span style="color:#0f766e;font-weight:700;margin-right:8px;">🎯</span> <strong>Ad-free experience</strong> & priority support
                </td></tr>
              """;

        String html = """
            <!DOCTYPE html>
            <html>
            <head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"></head>
            <body style="margin:0;padding:0;background-color:#f1f5f9;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;">
              <table width="100%%" border="0" cellspacing="0" cellpadding="0" style="background-color:#f1f5f9;padding:40px 20px;">
                <tr><td align="center">
                  <table width="100%%" border="0" cellspacing="0" cellpadding="0" style="max-width:600px;background-color:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 10px 25px -5px rgba(0,0,0,0.1);">
                    
                    <!-- Header -->
                    <tr><td style="background:linear-gradient(135deg,#0f766e 0%%,#064e3b 100%%);padding:48px 40px;text-align:center;">
                      <h1 style="margin:0;color:#ffffff;font-size:32px;font-weight:800;letter-spacing:-1px;">InkWell</h1>
                      <p style="margin:12px 0 0;color:#ccfbf1;font-size:16px;">Welcome to InkWell Pro</p>
                    </td></tr>
                    
                    <!-- Body -->
                    <tr><td style="padding:48px 40px;">
                      <div style="text-align:center;margin-bottom:32px;">
                        <div style="display:inline-block;background:linear-gradient(135deg,#fef3c7,#fde68a);border-radius:50%%;padding:16px;">
                          <span style="font-size:36px;">👑</span>
                        </div>
                      </div>
                      <h2 style="margin:0 0 8px;color:#0f172a;font-size:26px;font-weight:700;text-align:center;">Congratulations, %s!</h2>
                      <p style="margin:0 0 32px;color:#475569;font-size:16px;line-height:1.6;text-align:center;">
                        You are now a <strong style="color:#0f766e;">Pro member</strong>. Your payment has been successfully processed and your premium features are unlocked.
                      </p>
                      
                      <!-- Transaction Details -->
                      <div style="background:#f8fafc;border-radius:12px;padding:24px;margin-bottom:32px;border:1px solid #e2e8f0;">
                        <h3 style="margin:0 0 16px;color:#0f172a;font-size:14px;text-transform:uppercase;letter-spacing:1px;border-bottom:1px solid #e2e8f0;padding-bottom:8px;">Transaction Details</h3>
                        <table style="width:100%%;border-collapse:collapse;font-size:14px;color:#334155;">
                          <tr><td style="padding:8px 0;color:#64748b;">Billing Status</td><td style="padding:8px 0;text-align:right;font-weight:700;color:#0f766e;">COMPLETED</td></tr>
                          <tr><td style="padding:8px 0;color:#64748b;">Plan Name</td><td style="padding:8px 0;text-align:right;font-weight:600;">%s</td></tr>
                          <tr><td style="padding:8px 0;color:#64748b;">Plan Type</td><td style="padding:8px 0;text-align:right;font-weight:600;">%s</td></tr>
                          <tr><td style="padding:8px 0;color:#64748b;">Amount Paid</td><td style="padding:8px 0;text-align:right;font-weight:700;font-size:16px;">₹%s</td></tr>
                          <tr><td style="padding:8px 0;color:#64748b;">Payment Date</td><td style="padding:8px 0;text-align:right;font-weight:600;">%s</td></tr>
                          <tr><td style="padding:8px 0;color:#64748b;">Subscription Start</td><td style="padding:8px 0;text-align:right;font-weight:600;">%s</td></tr>
                          <tr><td style="padding:8px 0;color:#64748b;">Renewal / Expiry</td><td style="padding:8px 0;text-align:right;font-weight:600;">%s</td></tr>
                        </table>
                      </div>
                      
                      <!-- Premium Benefits -->
                      <h3 style="margin:0 0 16px;color:#0f172a;font-size:18px;font-weight:700;">🎉 Premium Benefits Unlocked</h3>
                      <table style="width:100%%;border-collapse:collapse;margin-bottom:32px;">
                        %s
                      </table>
                      
                      <table width="100%%" border="0" cellspacing="0" cellpadding="0">
                        <tr><td align="center">
                          <a href="http://localhost:5173/profile" style="display:inline-block;background:#0f766e;color:#fff;padding:16px 40px;border-radius:12px;text-decoration:none;font-weight:600;font-size:16px;">Go to Dashboard</a>
                        </td></tr>
                      </table>
                    </td></tr>
                    
                    <!-- Footer -->
                    <tr><td style="background-color:#f8fafc;padding:32px 40px;text-align:center;border-top:1px solid #e2e8f0;">
                      <p style="margin:0 0 8px;color:#94a3b8;font-size:12px;">Thank you for being a valued InkWell Pro member.</p>
                      <p style="margin:0;color:#94a3b8;font-size:12px;">© 2026 InkWell Publishing Platform. All rights reserved.</p>
                    </td></tr>
                    
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(
                details.fullName(), details.planName(), details.planType(), details.amount().toString(),
                details.paymentDate().format(formatter), details.startDate().format(formatter), details.expiryDate().format(formatter),
                benefitsHtml
            );
        send(details.to(), subject, html);
    }

    // Performs the send workflow so callers do not duplicate this logic.
    private void send(String to, String subject, String html) {
        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Email sent to {}: {}", to, subject);
        } catch (Exception e) {
            log.warn("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}

/*
 * This source file contains business workflow and validation logic for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.newsletter.service;

import com.inkwell.newsletter.dto.request.CampaignRequest;
import com.inkwell.newsletter.dto.request.SubscribeRequest;
import com.inkwell.newsletter.dto.response.SubscriberResponse;
import com.inkwell.newsletter.entity.Campaign;
import com.inkwell.newsletter.entity.Subscriber;
import com.inkwell.newsletter.enumtype.SubscriberStatus;
import com.inkwell.newsletter.repository.CampaignRepository;
import com.inkwell.newsletter.repository.SubscriberRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
/* This class groups newsletter service behavior so the module keeps a clear responsibility. */
public class NewsletterService {

    private final SubscriberRepository subscriberRepository;
    private final CampaignRepository campaignRepository;
    private final MailService mailService;
    private final TemplateService templateService;

    @Transactional
    // Defines subscribe so related behavior stays grouped in one place.
    public SubscriberResponse subscribe(SubscribeRequest request, String currentUserId) {
        Subscriber subscriber = subscriberRepository.findByEmailIgnoreCase(request.email()).orElse(null);
        if (subscriber != null) {
            if (subscriber.getStatus() == SubscriberStatus.ACTIVE) {
                throw new IllegalStateException("You are already subscribed to InkWell newsletter.");
            } else if (subscriber.getStatus() == SubscriberStatus.PENDING) {
                // Generate a new token and update subscriber details
                subscriber.setToken(UUID.randomUUID().toString());
            } else {
                subscriber.setToken(UUID.randomUUID().toString());
                subscriber.setStatus(SubscriberStatus.PENDING);
            }
        } else {
            subscriber = Subscriber.builder().token(UUID.randomUUID().toString()).build();
            subscriber.setStatus(SubscriberStatus.PENDING);
        }
        
        subscriber.setEmail(request.email().trim().toLowerCase());
        subscriber.setFullName(request.fullName());
        subscriber.setPreferences(request.preferences());
        if (currentUserId != null) {
            subscriber.setUserId(UUID.fromString(currentUserId));
        }
        
        Subscriber saved = subscriberRepository.save(subscriber);
        
        String confirmUrl = "http://localhost:8080/api/newsletter/public/confirm?token=" + saved.getToken();
        log.info("=================================================");
        log.info("NEWSLETTER CONFIRMATION URL FOR {}: {}", saved.getEmail(), confirmUrl);
        log.info("=================================================");
        
        String html = """
            <div style="font-family:'Segoe UI',Arial,sans-serif;max-width:520px;margin:0 auto;padding:32px;background:#ffffff;border-radius:16px;border:1px solid #e2e8f0">
              <div style="text-align:center;margin-bottom:24px">
                <span style="display:inline-block;background:#0f766e;color:#fff;padding:8px 16px;border-radius:8px;font-weight:700;font-size:18px">InkWell Newsletter</span>
              </div>
              <h2 style="margin:0 0 8px;color:#0f172a;font-size:22px">Verify your subscription</h2>
              <p style="color:#64748b;margin:0 0 16px;font-size:14px;line-height:1.6">
                You're almost there! Click the button below to confirm your subscription to the InkWell newsletter and start receiving our latest updates.
              </p>
              <div style="text-align:center;margin:24px 0;">
                <a href="%s" style="display:inline-block;background:#0f766e;color:#fff;padding:12px 32px;border-radius:10px;text-decoration:none;font-weight:600;font-size:14px">Confirm Subscription</a>
              </div>
              <p style="color:#94a3b8;font-size:12px;margin:0;text-align:center">If you didn't request this, you can safely ignore this email.</p>
            </div>
            """.formatted(confirmUrl);

        mailService.send(saved.getEmail(), "Confirm your InkWell subscription", html);
        return toResponse(saved);
    }

    @Transactional
    // Defines admin unsubscribe so related behavior stays grouped in one place.
    public void adminUnsubscribe(UUID id) {
        Subscriber subscriber = subscriberRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Subscriber not found"));
        subscriber.setStatus(SubscriberStatus.UNSUBSCRIBED);
        subscriber.setUnsubscribedAt(LocalDateTime.now());
        subscriberRepository.save(subscriber);
    }

    @Transactional(readOnly = true)
    // Performs the get my status workflow so callers do not duplicate this logic.
    public SubscriberResponse getMyStatus(UUID currentUserId) {
        return subscriberRepository.findByUserId(currentUserId).map(this::toResponse).orElse(null);
    }

    @Transactional
    // Defines confirm so related behavior stays grouped in one place.
    public SubscriberResponse confirm(String token) {
        Subscriber subscriber = subscriberRepository.findByToken(token).orElseThrow(() -> new IllegalArgumentException("Invalid or expired confirmation token."));
        if (subscriber.getStatus() == SubscriberStatus.ACTIVE) {
            throw new IllegalArgumentException("Subscription is already confirmed and active.");
        }
        subscriber.setStatus(SubscriberStatus.ACTIVE);
        subscriber.setSubscribedAt(LocalDateTime.now());
        Subscriber saved = subscriberRepository.save(subscriber);
        
        String subscriberName = saved.getFullName() != null ? saved.getFullName() : "Subscriber";
        String confirmDate = saved.getSubscribedAt().format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a"));
        String unsubscribeUrl = "http://localhost:8080/api/newsletter/public/unsubscribe/" + saved.getToken();

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
                      <p style="margin:12px 0 0;color:#ccfbf1;font-size:16px;">Newsletter Subscription Confirmed</p>
                    </td></tr>
                    
                    <!-- Body -->
                    <tr><td style="padding:48px 40px;">
                      <div style="text-align:center;margin-bottom:32px;">
                        <div style="display:inline-block;background:#f0fdf4;border-radius:50%%;padding:16px;">
                          <span style="font-size:32px;">✅</span>
                        </div>
                      </div>
                      <h2 style="margin:0 0 16px;color:#0f172a;font-size:24px;font-weight:700;text-align:center;">Welcome, %s!</h2>
                      <p style="margin:0 0 24px;color:#475569;font-size:16px;line-height:1.6;text-align:center;">
                        Your InkWell Newsletter subscription is now <strong style="color:#0f766e;">active</strong>. You're all set to receive the best content from our platform.
                      </p>
                      
                      <!-- Status Card -->
                      <div style="background:#f8fafc;border-radius:12px;padding:24px;margin-bottom:32px;border:1px solid #e2e8f0;">
                        <h3 style="margin:0 0 16px;color:#0f172a;font-size:14px;text-transform:uppercase;letter-spacing:1px;border-bottom:1px solid #e2e8f0;padding-bottom:8px;">Subscription Details</h3>
                        <table style="width:100%%;border-collapse:collapse;font-size:14px;color:#334155;">
                          <tr><td style="padding:8px 0;color:#64748b;">Email</td><td style="padding:8px 0;text-align:right;font-weight:600;">%s</td></tr>
                          <tr><td style="padding:8px 0;color:#64748b;">Status</td><td style="padding:8px 0;text-align:right;font-weight:700;color:#0f766e;">ACTIVE</td></tr>
                          <tr><td style="padding:8px 0;color:#64748b;">Confirmed On</td><td style="padding:8px 0;text-align:right;font-weight:600;">%s</td></tr>
                        </table>
                      </div>
                      
                      <!-- What You'll Receive -->
                      <h3 style="margin:0 0 16px;color:#0f172a;font-size:18px;font-weight:700;">What you'll receive:</h3>
                      <table style="width:100%%;border-collapse:collapse;margin-bottom:32px;">
                        <tr><td style="padding:10px 0;font-size:15px;color:#334155;border-bottom:1px solid #f1f5f9;">
                          <span style="color:#0f766e;font-weight:700;margin-right:8px;">📝</span> <strong>New Post Updates</strong> — Fresh articles from top authors
                        </td></tr>
                        <tr><td style="padding:10px 0;font-size:15px;color:#334155;border-bottom:1px solid #f1f5f9;">
                          <span style="color:#0f766e;font-weight:700;margin-right:8px;">📢</span> <strong>Campaign Updates</strong> — Curated content & exclusive newsletters
                        </td></tr>
                        <tr><td style="padding:10px 0;font-size:15px;color:#334155;">
                          <span style="color:#0f766e;font-weight:700;margin-right:8px;">🚀</span> <strong>Platform Announcements</strong> — New features & improvements
                        </td></tr>
                      </table>
                      
                      <table width="100%%" border="0" cellspacing="0" cellpadding="0">
                        <tr><td align="center">
                          <a href="http://localhost:5173" style="display:inline-block;background:#0f766e;color:#fff;padding:16px 40px;border-radius:12px;text-decoration:none;font-weight:600;font-size:16px;">Visit InkWell</a>
                        </td></tr>
                      </table>
                    </td></tr>
                    
                    <!-- Footer -->
                    <tr><td style="background-color:#f8fafc;padding:32px 40px;text-align:center;border-top:1px solid #e2e8f0;">
                      <p style="margin:0 0 8px;color:#94a3b8;font-size:12px;">© 2026 InkWell Publishing Platform. All rights reserved.</p>
                      <a href="%s" style="color:#0f766e;font-size:12px;text-decoration:underline;">Unsubscribe from this newsletter</a>
                    </td></tr>
                    
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(subscriberName, saved.getEmail(), confirmDate, unsubscribeUrl);
            
        mailService.send(saved.getEmail(), "Your InkWell Newsletter Subscription is Confirmed", html);
        return toResponse(saved);
    }

    @Transactional
    // Defines unsubscribe so related behavior stays grouped in one place.
    public void unsubscribe(String token) {
        Subscriber subscriber = subscriberRepository.findByToken(token).orElseThrow();
        subscriber.setStatus(SubscriberStatus.UNSUBSCRIBED);
        subscriber.setUnsubscribedAt(LocalDateTime.now());
        subscriberRepository.save(subscriber);
    }

    @Transactional(readOnly = true)
    // Defines all so related behavior stays grouped in one place.
    public List<SubscriberResponse> all() { return subscriberRepository.findAll().stream().map(this::toResponse).toList(); }

    @Transactional
    // Performs the send campaign workflow so callers do not duplicate this logic.
    public String sendCampaign(CampaignRequest request) {
        campaignRepository.save(Campaign.builder().subject(request.subject()).content(request.content()).build());
        
        List<Subscriber> activeSubscribers = subscriberRepository.findByStatus(SubscriberStatus.ACTIVE);
        int totalFound = activeSubscribers.size();
        
        log.info("Starting campaign '{}'. Found {} active subscribers.", request.subject(), totalFound);
        
        if (activeSubscribers.isEmpty()) {
            log.info("Campaign '{}' aborted: No active subscribers found.", request.subject());
            return "No active subscribers found. Campaign was not sent.";
        }
        
        String htmlTemplate = """
            <div style="font-family:'Segoe UI',Arial,sans-serif;max-width:600px;margin:0 auto;padding:32px;background:#ffffff;border-radius:16px;border:1px solid #e2e8f0">
              <div style="text-align:center;margin-bottom:32px;padding-bottom:16px;border-bottom:1px solid #f1f5f9;">
                <span style="display:inline-block;background:#0f766e;color:#fff;padding:8px 16px;border-radius:8px;font-weight:700;font-size:18px;letter-spacing:1px;">INKWELL</span>
              </div>
              
              <div style="color:#334155;font-size:16px;line-height:1.7;">
                %s
              </div>
              
              <div style="margin-top:40px;padding-top:20px;border-top:1px solid #f1f5f9;text-align:center;">
                <p style="color:#94a3b8;font-size:12px;margin:0 0 8px 0;">You are receiving this email because you are subscribed to the InkWell newsletter.</p>
                <a href="http://localhost:5173/newsletter/unsubscribe?token=%%s" style="color:#0f766e;font-size:12px;text-decoration:underline;">Unsubscribe</a>
              </div>
            </div>
            """;
            
        int successCount = 0;
        int failedCount = 0;
        
        for (Subscriber subscriber : activeSubscribers) {
            try {
                String formattedContent = request.content().replace("\n", "<br/>");
                String finalHtml = String.format(htmlTemplate, formattedContent).replace("%s", subscriber.getToken());
                mailService.send(subscriber.getEmail(), request.subject(), finalHtml);
                successCount++;
            } catch (Exception e) {
                failedCount++;
                log.error("Failed to send campaign email to {}: {}", subscriber.getEmail(), e.getMessage());
            }
        }
        
        log.info("Campaign '{}' finished. Sent: {}, Failed: {}", request.subject(), successCount, failedCount);
        return String.format("Campaign dispatched successfully to %d active subscribers.", successCount);
    }

    @RabbitListener(queues = "post-published-newsletter-queue")
    // Defines on post published so related behavior stays grouped in one place.
    public void onPostPublished(Map<String, Object> payload) {
        String title = String.valueOf(payload.get("title"));
        String slug = String.valueOf(payload.get("slug"));
        String excerpt = payload.containsKey("excerpt") ? String.valueOf(payload.get("excerpt")) : "";
        String authorName = payload.containsKey("authorName") ? String.valueOf(payload.get("authorName")) : "InkWell Author";
        
        String postUrl = "http://localhost:5173/posts/" + slug;
        
        String htmlTemplate = """
            <div style="font-family:'Segoe UI',Arial,sans-serif;max-width:600px;margin:0 auto;padding:32px;background:#ffffff;border-radius:16px;border:1px solid #e2e8f0">
              <div style="text-align:center;margin-bottom:32px;padding-bottom:16px;border-bottom:1px solid #f1f5f9;">
                <span style="display:inline-block;background:#0f766e;color:#fff;padding:8px 16px;border-radius:8px;font-weight:700;font-size:18px;letter-spacing:1px;">INKWELL</span>
              </div>
              
              <h2 style="margin:0 0 8px;color:#0f172a;font-size:24px">%s</h2>
              <p style="color:#64748b;font-size:14px;margin-bottom:24px;">By <strong>%s</strong></p>
              
              <div style="color:#334155;font-size:16px;line-height:1.7;margin-bottom:32px;padding:20px;background:#f8fafc;border-left:4px solid #0f766e;border-radius:4px;">
                %s
              </div>
              
              <div style="text-align:center;margin-bottom:40px;">
                <a href="%s" style="display:inline-block;background:#0f766e;color:#fff;padding:12px 32px;border-radius:10px;text-decoration:none;font-weight:600;font-size:15px">Read Full Post</a>
              </div>
              
              <div style="margin-top:40px;padding-top:20px;border-top:1px solid #f1f5f9;text-align:center;">
                <p style="color:#94a3b8;font-size:12px;margin:0 0 8px 0;">You are receiving this email because you are subscribed to the InkWell newsletter.</p>
                <a href="http://localhost:5173/newsletter/unsubscribe?token=%%s" style="color:#0f766e;font-size:12px;text-decoration:underline;">Unsubscribe</a>
              </div>
            </div>
            """;
            
        subscriberRepository.findByStatus(SubscriberStatus.ACTIVE).forEach(subscriber -> {
            String finalHtml = String.format(htmlTemplate, title, authorName, excerpt, postUrl).replace("%s", subscriber.getToken());
            mailService.send(subscriber.getEmail(), "New Post: " + title, finalHtml);
        });
    }

    // Defines to response so related behavior stays grouped in one place.
    private SubscriberResponse toResponse(Subscriber subscriber) {
        return new SubscriberResponse(subscriber.getSubscriberId(), subscriber.getEmail(), subscriber.getUserId(), subscriber.getFullName(), subscriber.getStatus(), subscriber.getSubscribedAt(), subscriber.getUnsubscribedAt(), subscriber.getPreferences());
    }
}

/*
 * This source file contains business workflow and validation logic for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.notification.service;

import com.inkwell.notification.client.AuthClient;
import com.inkwell.notification.dto.request.BroadcastRequest;
import com.inkwell.notification.dto.response.AuditLogResponse;
import com.inkwell.notification.dto.response.NotificationResponse;
import com.inkwell.notification.dto.response.UserResponse;
import com.inkwell.notification.entity.AuditLog;
import com.inkwell.notification.entity.Notification;
import com.inkwell.notification.enumtype.NotificationType;
import com.inkwell.notification.repository.AuditLogRepository;
import com.inkwell.notification.repository.NotificationRepository;
import com.inkwell.notification.security.GatewayUserPrincipal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
/* This class groups notification service behavior so the module keeps a clear responsibility. */
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuthClient authClient;
    private final MailService mailService;
    private final RabbitTemplate rabbitTemplate;

    /**
     * Groups the parameters for creating a notification.
     */
    public record NotificationDetails(UUID recipientId, UUID actorId, NotificationType type,
                                       String title, String message, String emailHtml,
                                       String relatedId, String relatedType) {}

    @Transactional(readOnly = true)
    // Defines mine so related behavior stays grouped in one place.
    public List<NotificationResponse> mine(GatewayUserPrincipal principal) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(principal.userUuid())
            .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    // Defines unread count so related behavior stays grouped in one place.
    public long unreadCount(GatewayUserPrincipal principal) {
        return notificationRepository.countByRecipientIdAndReadFalse(principal.userUuid());
    }

    @Transactional
    // Performs the mark read workflow so callers do not duplicate this logic.
    public void markRead(UUID notificationId, GatewayUserPrincipal principal) {
        Notification notification = notificationRepository.findById(notificationId).orElseThrow();
        if (notification.getRecipientId().equals(principal.userUuid())) {
            notification.setRead(true);
            notificationRepository.save(notification);
        }
    }

    @Transactional
    // Performs the mark all read workflow so callers do not duplicate this logic.
    public void markAllRead(GatewayUserPrincipal principal) {
        notificationRepository.findByRecipientIdOrderByCreatedAtDesc(principal.userUuid())
            .forEach(notification -> {
                notification.setRead(true);
                notificationRepository.save(notification);
            });
    }

    @Transactional
    // Performs the delete read workflow so callers do not duplicate this logic.
    public void deleteRead(GatewayUserPrincipal principal) {
        notificationRepository.deleteByRecipientIdAndReadTrue(principal.userUuid());
    }

    @Transactional(readOnly = true)
    // Defines audits so related behavior stays grouped in one place.
    public List<AuditLogResponse> audits() {
        return auditLogRepository.findAllByOrderByCreatedAtDesc()
            .stream().map(this::toAudit).toList();
    }

    @Transactional
    // Defines broadcast so related behavior stays grouped in one place.
    public void broadcast(BroadcastRequest request, GatewayUserPrincipal principal) {
        String htmlTemplate = """
            <div style="font-family:'Segoe UI',Arial,sans-serif;max-width:600px;margin:0 auto;padding:32px;background:#ffffff;border-radius:16px;border:1px solid #e2e8f0">
              <div style="text-align:center;margin-bottom:32px;padding-bottom:16px;border-bottom:1px solid #f1f5f9;">
                <span style="display:inline-block;background:#0f766e;color:#fff;padding:8px 16px;border-radius:8px;font-weight:700;font-size:18px;letter-spacing:1px;">INKWELL BROADCAST</span>
              </div>
              <h2 style="margin:0 0 20px;color:#0f172a;font-size:24px">%s</h2>
              <div style="color:#334155;font-size:16px;line-height:1.7;">
                %s
              </div>
              <div style="margin-top:40px;padding-top:20px;border-top:1px solid #f1f5f9;text-align:center;">
                <p style="color:#94a3b8;font-size:12px;margin:0;">© 2026 InkWell Publishing Platform. All rights reserved.</p>
              </div>
            </div>
            """;
        String formattedMessage = request.message().replace("\n", "<br/>");
        String htmlMessage = String.format(htmlTemplate, request.title(), formattedMessage);

        String broadcastId = UUID.randomUUID().toString();
        authClient.searchUsers("").data().forEach(user ->
            createNotification(new NotificationDetails(user.userId(), principal.userUuid(), NotificationType.ADMIN_BROADCAST, request.title(), request.message(), htmlMessage, broadcastId, "broadcast"))
        );
        auditLogRepository.save(AuditLog.builder().actorId(principal.userUuid()).action("ADMIN_BROADCAST").source("notification-service").details(request.title() + " (" + broadcastId + ")").build());
        rabbitTemplate.convertAndSend("inkwell.exchange", "admin.broadcast", Map.of("title", request.title(), "message", request.message(), "actorId", principal.userId()));
    }

    @RabbitListener(queues = "comment-notification-queue")
    // Defines on comment so related behavior stays grouped in one place.
    public void onComment(Map<String, Object> payload) {
        UUID recipient = UUID.fromString(String.valueOf(payload.get("postAuthorId")));
        UUID actor = UUID.fromString(String.valueOf(payload.get("commentAuthorId")));
        String html = """
            <div style="font-family:'Segoe UI',Arial,sans-serif;max-width:600px;margin:0 auto;padding:32px;background:#ffffff;border-radius:16px;border:1px solid #e2e8f0">
              <div style="text-align:center;margin-bottom:32px;padding-bottom:16px;border-bottom:1px solid #f1f5f9;">
                <span style="display:inline-block;background:#0f766e;color:#fff;padding:8px 16px;border-radius:8px;font-weight:700;font-size:18px;letter-spacing:1px;">INKWELL NOTIFICATION</span>
              </div>
              <h2 style="margin:0 0 16px;color:#0f172a;font-size:20px">New comment on your post</h2>
              <p style="color:#475569;font-size:16px;line-height:1.6">A reader just commented on your article.</p>
              <div style="text-align:center;margin-top:32px;">
                <a href="http://localhost:5173/dashboard" style="display:inline-block;background:#0f766e;color:#fff;padding:12px 32px;border-radius:10px;text-decoration:none;font-weight:600;font-size:14px">View Comment</a>
              </div>
            </div>
            """;
        createNotification(new NotificationDetails(recipient, actor, NotificationType.NEW_COMMENT, "New comment on your post", "A reader just commented on your article.", html, String.valueOf(payload.get("postId")), "post"));
    }

    @RabbitListener(queues = "reply-notification-queue")
    // Defines on reply so related behavior stays grouped in one place.
    public void onReply(Map<String, Object> payload) {
        UUID recipient = UUID.fromString(String.valueOf(payload.get("postAuthorId")));
        UUID actor = UUID.fromString(String.valueOf(payload.get("commentAuthorId")));
        String html = """
            <div style="font-family:'Segoe UI',Arial,sans-serif;max-width:600px;margin:0 auto;padding:32px;background:#ffffff;border-radius:16px;border:1px solid #e2e8f0">
              <div style="text-align:center;margin-bottom:32px;padding-bottom:16px;border-bottom:1px solid #f1f5f9;">
                <span style="display:inline-block;background:#0f766e;color:#fff;padding:8px 16px;border-radius:8px;font-weight:700;font-size:18px;letter-spacing:1px;">INKWELL NOTIFICATION</span>
              </div>
              <h2 style="margin:0 0 16px;color:#0f172a;font-size:20px">New reply in a discussion</h2>
              <p style="color:#475569;font-size:16px;line-height:1.6">A reply was added in a thread on your post.</p>
              <div style="text-align:center;margin-top:32px;">
                <a href="http://localhost:5173/dashboard" style="display:inline-block;background:#0f766e;color:#fff;padding:12px 32px;border-radius:10px;text-decoration:none;font-weight:600;font-size:14px">View Reply</a>
              </div>
            </div>
            """;
        createNotification(new NotificationDetails(recipient, actor, NotificationType.COMMENT_REPLY, "New reply in a discussion", "A reply was added in a thread on your post.", html, String.valueOf(payload.get("postId")), "post"));
    }

    @RabbitListener(queues = "post-published-notification-queue")
    // Defines on post published so related behavior stays grouped in one place.
    public void onPostPublished(Map<String, Object> payload) {
        auditLogRepository.save(AuditLog.builder().actorId(UUID.fromString(String.valueOf(payload.get("authorId")))).action("POST_PUBLISHED").source("post-service").details(String.valueOf(payload.get("title"))).build());
    }

    // Performs the create notification workflow so callers do not duplicate this logic.
    private void createNotification(NotificationDetails details) {
        notificationRepository.save(Notification.builder()
            .recipientId(details.recipientId())
            .actorId(details.actorId())
            .type(details.type())
            .title(details.title())
            .message(details.message())
            .relatedId(details.relatedId())
            .relatedType(details.relatedType())
            .read(false)
            .build());
        try {
            UserResponse recipient = authClient.getUser(details.recipientId()).data();
            mailService.send(recipient.email(), details.title(), details.emailHtml());
        } catch (Exception ex) {
            log.debug("Failed to send notification email to {}: {}", details.recipientId(), ex.getMessage());
        }
    }

    @Transactional
    // Performs the delete admin notification workflow so callers do not duplicate this logic.
    public void deleteAdminNotification(UUID notificationId) {
        notificationRepository.deleteById(notificationId);
    }

    @Transactional
    // Performs the delete admin broadcast workflow so callers do not duplicate this logic.
    public void deleteAdminBroadcast(String broadcastId) {
        notificationRepository.deleteByRelatedIdAndRelatedType(broadcastId, "broadcast");
    }

    @Transactional
    // Performs the delete admin newsletter workflow so callers do not duplicate this logic.
    public void deleteAdminNewsletter(String newsletterId) {
        notificationRepository.deleteByRelatedIdAndRelatedType(newsletterId, "newsletter");
    }

    // Defines to response so related behavior stays grouped in one place.
    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(notification.getNotificationId(), notification.getRecipientId(),
            notification.getActorId(), notification.getType(), notification.getTitle(),
            notification.getMessage(), notification.getRelatedId(), notification.getRelatedType(),
            notification.isRead(), notification.getCreatedAt());
    }

    // Defines to audit so related behavior stays grouped in one place.
    private AuditLogResponse toAudit(AuditLog auditLog) {
        return new AuditLogResponse(auditLog.getAuditId(), auditLog.getActorId(),
            auditLog.getAction(), auditLog.getSource(), auditLog.getDetails(), auditLog.getCreatedAt());
    }
}

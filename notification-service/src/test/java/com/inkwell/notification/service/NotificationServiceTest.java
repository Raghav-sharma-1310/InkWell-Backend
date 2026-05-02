/*
 * This source file contains business workflow and validation logic for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.notification.service;

import com.inkwell.notification.client.AuthClient;
import com.inkwell.notification.dto.response.AuditLogResponse;
import com.inkwell.notification.dto.response.NotificationResponse;
import com.inkwell.notification.entity.AuditLog;
import com.inkwell.notification.entity.Notification;
import com.inkwell.notification.enumtype.NotificationType;
import com.inkwell.notification.repository.AuditLogRepository;
import com.inkwell.notification.repository.NotificationRepository;
import com.inkwell.notification.security.GatewayUserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;


import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
/* This class groups notification service test behavior so the module keeps a clear responsibility. */
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private AuthClient authClient;
    @Mock private MailService mailService;
    @Mock private RabbitTemplate rabbitTemplate;

    @InjectMocks private NotificationService notificationService;

    private UUID userId;
    private GatewayUserPrincipal principal;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        principal = new GatewayUserPrincipal(userId.toString(), "testuser", "test@inkwell.com", "READER");
    }

    // Defines build notification so related behavior stays grouped in one place.
    private Notification buildNotification(UUID recipientId, String title, boolean read) {
        return Notification.builder()
                .notificationId(UUID.randomUUID())
                .recipientId(recipientId)
                .title(title)
                .type(NotificationType.NEW_COMMENT)
                .read(read)
                .build();
    }

    @Test
    @DisplayName("Should fetch notifications for user")
    void mine() {
        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(buildNotification(userId, "Test", false)));

        List<NotificationResponse> result = notificationService.mine(principal);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("Test");
    }

    @Test
    @DisplayName("Should count unread notifications")
    void unreadCount() {
        when(notificationRepository.countByRecipientIdAndReadFalse(userId)).thenReturn(3L);
        assertThat(notificationService.unreadCount(principal)).isEqualTo(3L);
    }

    @Test
    @DisplayName("Should mark notification as read")
    void markRead() {
        UUID notifId = UUID.randomUUID();
        Notification notification = buildNotification(userId, "Test", false);
        notification.setNotificationId(notifId);

        when(notificationRepository.findById(notifId)).thenReturn(Optional.of(notification));

        notificationService.markRead(notifId, principal);

        assertThat(notification.isRead()).isTrue();
        verify(notificationRepository).save(notification);
    }

    @Test
    @DisplayName("Should not mark other user's notification as read")
    void markReadWrongUser() {
        UUID notifId = UUID.randomUUID();
        Notification notification = buildNotification(UUID.randomUUID(), "Test", false);
        notification.setNotificationId(notifId);

        when(notificationRepository.findById(notifId)).thenReturn(Optional.of(notification));

        notificationService.markRead(notifId, principal);

        assertThat(notification.isRead()).isFalse();
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should mark all notifications as read")
    void markAllRead() {
        Notification n1 = buildNotification(userId, "One", false);
        Notification n2 = buildNotification(userId, "Two", false);
        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(n1, n2));

        notificationService.markAllRead(principal);

        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    @Test
    @DisplayName("Should delete read notifications")
    void deleteRead() {
        notificationService.deleteRead(principal);
        verify(notificationRepository).deleteByRecipientIdAndReadTrue(userId);
    }

    @Test
    @DisplayName("Should fetch audit logs")
    void audits() {
        AuditLog log = AuditLog.builder().auditId(UUID.randomUUID()).action("TEST").source("test").build();
        when(auditLogRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(log));

        List<AuditLogResponse> result = notificationService.audits();
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Should handle comment notification event")
    void onComment() {
        UUID postAuthorId = UUID.randomUUID();
        UUID commentAuthorId = UUID.randomUUID();
        Map<String, Object> payload = Map.of(
                "postAuthorId", postAuthorId.toString(),
                "commentAuthorId", commentAuthorId.toString(),
                "postId", UUID.randomUUID().toString()
        );
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        com.inkwell.notification.dto.ApiResponse<com.inkwell.notification.dto.response.UserResponse> mockResponse = new com.inkwell.notification.dto.ApiResponse<>(
            java.time.Instant.now(), "success", new com.inkwell.notification.dto.response.UserResponse(postAuthorId, "test", "test@inkwell.com", "Test", "avatar", "bio", "123", "READER", true, java.time.LocalDateTime.now())
        );
        when(authClient.getUser(postAuthorId)).thenReturn(mockResponse);

        notificationService.onComment(payload);

        verify(notificationRepository).save(argThat(n -> n.getType() == NotificationType.NEW_COMMENT));
        verify(mailService).send(eq("test@inkwell.com"), anyString(), anyString());
    }

    @Test
    @DisplayName("Should handle reply notification event")
    void onReply() {
        Map<String, Object> payload = Map.of(
                "postAuthorId", UUID.randomUUID().toString(),
                "commentAuthorId", UUID.randomUUID().toString(),
                "postId", UUID.randomUUID().toString()
        );
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        notificationService.onReply(payload);

        verify(notificationRepository).save(argThat(n -> n.getType() == NotificationType.COMMENT_REPLY));
    }

    @Test
    @DisplayName("Should handle post published event as audit log")
    void onPostPublished() {
        Map<String, Object> payload = Map.of(
                "authorId", UUID.randomUUID().toString(),
                "title", "New Post Title"
        );

        notificationService.onPostPublished(payload);

        verify(auditLogRepository).save(argThat(a -> "POST_PUBLISHED".equals(a.getAction())));
    }

    @Test
    @DisplayName("Should delete admin notification")
    void deleteAdminNotification() {
        UUID notifId = UUID.randomUUID();
        notificationService.deleteAdminNotification(notifId);
        verify(notificationRepository).deleteById(notifId);
    }

    @Test
    @DisplayName("Should delete admin broadcast")
    void deleteAdminBroadcast() {
        notificationService.deleteAdminBroadcast("broadcast-123");
        verify(notificationRepository).deleteByRelatedIdAndRelatedType("broadcast-123", "broadcast");
    }

    @Test
    @DisplayName("Should delete admin newsletter")
    void deleteAdminNewsletter() {
        notificationService.deleteAdminNewsletter("newsletter-123");
        verify(notificationRepository).deleteByRelatedIdAndRelatedType("newsletter-123", "newsletter");
    }

    @Test
    @DisplayName("Should broadcast notification")
    void broadcast() {
        com.inkwell.notification.dto.request.BroadcastRequest request = new com.inkwell.notification.dto.request.BroadcastRequest("Title", "Message");
        
        com.inkwell.notification.dto.ApiResponse<List<com.inkwell.notification.dto.response.UserResponse>> mockResponse = new com.inkwell.notification.dto.ApiResponse<>(
            java.time.Instant.now(), "success", List.of(
                new com.inkwell.notification.dto.response.UserResponse(UUID.randomUUID(), "test", "test@inkwell.com", "Test", "avatar", "bio", "123", "READER", true, java.time.LocalDateTime.now())
            )
        );
        when(authClient.searchUsers(anyString())).thenReturn(mockResponse);
        when(authClient.getUser(any())).thenReturn(new com.inkwell.notification.dto.ApiResponse<>(
            java.time.Instant.now(), "success", new com.inkwell.notification.dto.response.UserResponse(UUID.randomUUID(), "test", "test@inkwell.com", "Test", "avatar", "bio", "123", "READER", true, java.time.LocalDateTime.now())
        ));

        notificationService.broadcast(request, principal);

        verify(notificationRepository).save(argThat(n -> n.getType() == NotificationType.ADMIN_BROADCAST));
        verify(auditLogRepository).save(argThat(a -> "ADMIN_BROADCAST".equals(a.getAction())));
        verify(rabbitTemplate).convertAndSend(eq("inkwell.exchange"), eq("admin.broadcast"), anyMap());
    }
}

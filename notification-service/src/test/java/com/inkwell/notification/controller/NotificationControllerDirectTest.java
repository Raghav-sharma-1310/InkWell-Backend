/*
 * This source file contains HTTP controller endpoints for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.notification.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.inkwell.notification.dto.request.BroadcastRequest;
import com.inkwell.notification.dto.response.AuditLogResponse;
import com.inkwell.notification.dto.response.NotificationResponse;
import com.inkwell.notification.enumtype.NotificationType;
import com.inkwell.notification.security.GatewayUserPrincipal;
import com.inkwell.notification.service.NotificationService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
/* This class groups notification controller direct test behavior so the module keeps a clear responsibility. */
class NotificationControllerDirectTest {

    @Mock
    private NotificationService notificationService;

    private NotificationController notificationController;
    private AdminNotificationController adminNotificationController;
    private GatewayUserPrincipal principal;

    @BeforeEach
    void setUp() {
        notificationController = new NotificationController(notificationService);
        adminNotificationController = new AdminNotificationController(notificationService);
        principal = new GatewayUserPrincipal(UUID.randomUUID().toString(), "admin", "admin@inkwell.com", "ADMIN");
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void mineReturnsCurrentUserNotifications() {
        NotificationResponse response = new NotificationResponse(UUID.randomUUID(), principal.userUuid(),
            UUID.randomUUID(), NotificationType.NEW_COMMENT, "Title", "Message", "post-1", "post",
            false, LocalDateTime.now());
        when(notificationService.mine(any())).thenReturn(List.of(response));

        var result = notificationController.mine();

        assertThat(result.message()).isEqualTo("Notifications fetched");
        assertThat(result.data()).containsExactly(response);
        verify(notificationService).mine(principal);
    }

    @Test
    void unreadCountDelegatesToService() {
        when(notificationService.unreadCount(any())).thenReturn(4L);

        var result = notificationController.unreadCount();

        assertThat(result.data()).isEqualTo(4L);
        verify(notificationService).unreadCount(principal);
    }

    @Test
    void mutationEndpointsDelegateWithCurrentPrincipal() {
        UUID notificationId = UUID.randomUUID();

        assertThat(notificationController.markRead(notificationId).message()).isEqualTo("Notification marked as read");
        assertThat(notificationController.markAllRead().message()).isEqualTo("All notifications marked as read");
        assertThat(notificationController.deleteRead().message()).isEqualTo("Read notifications deleted");
        assertThat(notificationController.broadcast(new BroadcastRequest("Notice", "Line one\nLine two")).message())
            .isEqualTo("Broadcast sent");

        verify(notificationService).markRead(notificationId, principal);
        verify(notificationService).markAllRead(principal);
        verify(notificationService).deleteRead(principal);
        verify(notificationService).broadcast(new BroadcastRequest("Notice", "Line one\nLine two"), principal);
    }

    @Test
    void auditsReturnsAdminAuditLogs() {
        AuditLogResponse audit = new AuditLogResponse(UUID.randomUUID(), principal.userUuid(), "ACTION",
            "notification-service", "details", LocalDateTime.now());
        when(notificationService.audits()).thenReturn(List.of(audit));

        var result = notificationController.audits();

        assertThat(result.message()).isEqualTo("Audit logs fetched");
        assertThat(result.data()).containsExactly(audit);
    }

    @Test
    void adminDeleteEndpointsDelegateToService() {
        UUID notificationId = UUID.randomUUID();

        assertThat(adminNotificationController.deleteNotification(notificationId).message()).isEqualTo("Notification deleted");
        assertThat(adminNotificationController.deleteBroadcast("broadcast-1").message()).isEqualTo("Broadcast notifications deleted");
        assertThat(adminNotificationController.deleteNewsletter("newsletter-1").message()).isEqualTo("Newsletter notifications deleted");

        verify(notificationService).deleteAdminNotification(notificationId);
        verify(notificationService).deleteAdminBroadcast("broadcast-1");
        verify(notificationService).deleteAdminNewsletter("newsletter-1");
    }

    @Test
    void serviceInfoReturnsHealthMetadata() {
        var result = new ServiceInfoController().root();

        assertThat(result).containsEntry("service", "notification-service")
            .containsEntry("status", "UP")
            .containsEntry("health", "/actuator/health");
    }
}

/*
 * This source file contains HTTP controller endpoints for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.notification.controller;

import com.inkwell.notification.dto.ApiResponse;
import com.inkwell.notification.dto.request.BroadcastRequest;
import com.inkwell.notification.dto.response.AuditLogResponse;
import com.inkwell.notification.dto.response.NotificationResponse;
import com.inkwell.notification.service.NotificationService;
import com.inkwell.notification.util.SecurityUtils;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
/* This class groups notification controller behavior so the module keeps a clear responsibility. */
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    // Defines mine so related behavior stays grouped in one place.
    public ApiResponse<List<NotificationResponse>> mine() { return ApiResponse.of("Notifications fetched", notificationService.mine(SecurityUtils.currentPrincipal())); }
    @GetMapping("/unread-count")
    // Defines unread count so related behavior stays grouped in one place.
    public ApiResponse<Long> unreadCount() { return ApiResponse.of("Unread count fetched", notificationService.unreadCount(SecurityUtils.currentPrincipal())); }
    @PatchMapping("/{notificationId}/read")
    // Performs the mark read workflow so callers do not duplicate this logic.
    public ApiResponse<Void> markRead(@PathVariable UUID notificationId) { notificationService.markRead(notificationId, SecurityUtils.currentPrincipal()); return ApiResponse.of("Notification marked as read", null); }
    @PatchMapping("/read-all")
    // Performs the mark all read workflow so callers do not duplicate this logic.
    public ApiResponse<Void> markAllRead() { notificationService.markAllRead(SecurityUtils.currentPrincipal()); return ApiResponse.of("All notifications marked as read", null); }
    @DeleteMapping("/read")
    // Performs the delete read workflow so callers do not duplicate this logic.
    public ApiResponse<Void> deleteRead() { notificationService.deleteRead(SecurityUtils.currentPrincipal()); return ApiResponse.of("Read notifications deleted", null); }
    @PostMapping("/admin/broadcast")
    @PreAuthorize("hasRole('ADMIN')")
    // Defines broadcast so related behavior stays grouped in one place.
    public ApiResponse<Void> broadcast(@Valid @RequestBody BroadcastRequest request) { notificationService.broadcast(request, SecurityUtils.currentPrincipal()); return ApiResponse.of("Broadcast sent", null); }
    @GetMapping("/admin/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    // Defines audits so related behavior stays grouped in one place.
    public ApiResponse<List<AuditLogResponse>> audits() { return ApiResponse.of("Audit logs fetched", notificationService.audits()); }
}

/*
 * This source file contains HTTP controller endpoints for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.notification.controller;

import com.inkwell.notification.dto.ApiResponse;
import com.inkwell.notification.service.NotificationService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
/* This class groups admin notification controller behavior so the module keeps a clear responsibility. */
public class AdminNotificationController {

    private final NotificationService notificationService;

    @DeleteMapping("/{notificationId}")
    @PreAuthorize("hasRole('ADMIN')")
    // Performs the delete notification workflow so callers do not duplicate this logic.
    public ApiResponse<Void> deleteNotification(@PathVariable UUID notificationId) {
        notificationService.deleteAdminNotification(notificationId);
        return ApiResponse.of("Notification deleted", null);
    }

    @DeleteMapping("/broadcast/{broadcastId}")
    @PreAuthorize("hasRole('ADMIN')")
    // Performs the delete broadcast workflow so callers do not duplicate this logic.
    public ApiResponse<Void> deleteBroadcast(@PathVariable String broadcastId) {
        notificationService.deleteAdminBroadcast(broadcastId);
        return ApiResponse.of("Broadcast notifications deleted", null);
    }

    @DeleteMapping("/newsletter/{newsletterId}")
    @PreAuthorize("hasRole('ADMIN')")
    // Performs the delete newsletter workflow so callers do not duplicate this logic.
    public ApiResponse<Void> deleteNewsletter(@PathVariable String newsletterId) {
        notificationService.deleteAdminNewsletter(newsletterId);
        return ApiResponse.of("Newsletter notifications deleted", null);
    }
}

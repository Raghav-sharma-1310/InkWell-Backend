/*
 * This source file contains request and response data shapes for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.notification.dto.response;

import com.inkwell.notification.enumtype.NotificationType;
import java.time.LocalDateTime;
import java.util.UUID;

/* This record groups notification response behavior so the module keeps a clear responsibility. */
public record NotificationResponse(UUID notificationId, UUID recipientId, UUID actorId, NotificationType type, String title, String message, String relatedId, String relatedType, boolean read, LocalDateTime createdAt) {}

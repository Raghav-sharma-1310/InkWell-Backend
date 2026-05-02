/*
 * This source file contains persistent domain data for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.notification.entity;

import com.inkwell.notification.enumtype.NotificationType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "notifications")
/* This class groups notification behavior so the module keeps a clear responsibility. */
public class Notification {
    @Id @Column(name = "notification_id", nullable = false, updatable = false) private UUID notificationId;
    @Column(name = "recipient_id", nullable = false) private UUID recipientId;
    @Column(name = "actor_id") private UUID actorId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private NotificationType type;
    @Column(nullable = false) private String title;
    @Column(nullable = false, length = 1000) private String message;
    @Column(name = "related_id") private String relatedId;
    @Column(name = "related_type") private String relatedType;
    @Column(name = "is_read", nullable = false) private boolean read;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @PrePersist void onCreate() { if (notificationId == null) { notificationId = UUID.randomUUID(); } createdAt = LocalDateTime.now(); }
}

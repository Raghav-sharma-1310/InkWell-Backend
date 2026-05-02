/*
 * This source file contains persistent domain data for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.notification.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "audit_logs")
/* This class groups audit log behavior so the module keeps a clear responsibility. */
public class AuditLog {
    @Id @Column(name = "audit_id", nullable = false, updatable = false) private UUID auditId;
    @Column(name = "actor_id") private UUID actorId;
    @Column(nullable = false) private String action;
    @Column(nullable = false) private String source;
    @Column(nullable = false, length = 1500) private String details;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @PrePersist void onCreate() { if (auditId == null) { auditId = UUID.randomUUID(); } createdAt = LocalDateTime.now(); }
}

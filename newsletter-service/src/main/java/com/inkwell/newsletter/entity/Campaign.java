/*
 * This source file contains persistent domain data for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.newsletter.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "campaigns")
/* This class groups campaign behavior so the module keeps a clear responsibility. */
public class Campaign {
    @Id @Column(name = "campaign_id", nullable = false, updatable = false) private UUID campaignId;
    @Column(nullable = false) private String subject;
    @Column(nullable = false, columnDefinition = "TEXT") private String content;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @PrePersist void onCreate() { if (campaignId == null) { campaignId = UUID.randomUUID(); } createdAt = LocalDateTime.now(); }
}

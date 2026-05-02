/*
 * This source file contains persistent domain data for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "feedback_messages")
/* This class groups feedback message behavior so the module keeps a clear responsibility. */
public class FeedbackMessage {

    @Id
    @Column(name = "message_id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private FeedbackReport report;

    @Column(name = "sender_id", nullable = false)
    private UUID senderId;

    @Column(name = "sender_name", nullable = false, length = 120)
    private String senderName;

    @Column(name = "sender_role", nullable = false, length = 20)
    private String senderRole;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (sentAt == null) {
            sentAt = LocalDateTime.now();
        }
    }
}

/*
 * This source file contains persistent domain data for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.post.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "post_history")
/* This class groups post history behavior so the module keeps a clear responsibility. */
public class PostHistory {

    @Id
    @Column(name = "history_id", nullable = false, updatable = false)
    private UUID historyId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "post_id", nullable = false)
    private UUID postId;

    @Column(name = "viewed_at", nullable = false)
    private LocalDateTime viewedAt;

    @PrePersist
    void onCreate() {
        if (historyId == null) {
            historyId = UUID.randomUUID();
        }
        viewedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        viewedAt = LocalDateTime.now();
    }
}

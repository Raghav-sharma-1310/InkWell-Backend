/*
 * This source file contains persistent domain data for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.post.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
@Table(name = "bookmarks", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "post_id"}))
/* This class groups bookmark behavior so the module keeps a clear responsibility. */
public class Bookmark {
    @Id
    @Column(name = "bookmark_id", nullable = false, updatable = false)
    private UUID bookmarkId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "post_id", nullable = false)
    private UUID postId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (bookmarkId == null) bookmarkId = UUID.randomUUID();
        createdAt = LocalDateTime.now();
    }
}

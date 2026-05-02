/*
 * This source file contains persistent domain data for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.comment.entity;

import com.inkwell.comment.enumtype.CommentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "comments")
/* This class groups comment behavior so the module keeps a clear responsibility. */
public class Comment {
    @Id
    @Column(name = "comment_id", nullable = false, updatable = false)
    private UUID commentId;
    @Column(name = "post_id", nullable = false)
    private UUID postId;
    @Column(name = "author_id", nullable = false)
    private UUID authorId;
    @Column(name = "author_name", length = 100)
    private String authorName;
    @Column(name = "parent_comment_id")
    private UUID parentCommentId;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    @Column(name = "likes_count", nullable = false)
    private long likesCount;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CommentStatus status;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    @PrePersist
    void onCreate() { if (commentId == null) { commentId = UUID.randomUUID(); } createdAt = LocalDateTime.now(); updatedAt = createdAt; }
    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }
}

/*
 * This source file contains persistent domain data for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.comment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@IdClass(CommentLike.CommentLikeId.class)
@Table(name = "comment_likes")
/* This class groups comment like behavior so the module keeps a clear responsibility. */
public class CommentLike {
    @Id
    @Column(name = "comment_id", nullable = false)
    private UUID commentId;
    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    /* This class groups comment like id behavior so the module keeps a clear responsibility. */
    public static class CommentLikeId implements Serializable {
        private UUID commentId;
        private UUID userId;
    }
}

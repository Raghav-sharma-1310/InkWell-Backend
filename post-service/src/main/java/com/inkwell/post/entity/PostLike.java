/*
 * This source file contains persistent domain data for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.post.entity;

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
@IdClass(PostLike.PostLikeId.class)
@Table(name = "post_likes")
/* This class groups post like behavior so the module keeps a clear responsibility. */
public class PostLike {

    @Id
    @Column(name = "post_id", nullable = false)
    private UUID postId;

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    /* This class groups post like id behavior so the module keeps a clear responsibility. */
    public static class PostLikeId implements Serializable {
        private UUID postId;
        private UUID userId;
    }
}

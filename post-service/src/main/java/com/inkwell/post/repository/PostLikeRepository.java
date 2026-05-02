/*
 * This source file contains database access contracts for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.post.repository;

import com.inkwell.post.entity.PostLike;
import com.inkwell.post.entity.PostLike.PostLikeId;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/* This interface groups post like repository behavior so the module keeps a clear responsibility. */
public interface PostLikeRepository extends JpaRepository<PostLike, PostLikeId> {
    boolean existsByPostIdAndUserId(UUID postId, UUID userId);

    long countByPostId(UUID postId);

    void deleteByPostIdAndUserId(UUID postId, UUID userId);

    void deleteByPostId(UUID postId);
}

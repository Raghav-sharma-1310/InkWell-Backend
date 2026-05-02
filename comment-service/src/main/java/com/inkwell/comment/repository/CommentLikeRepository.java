/*
 * This source file contains database access contracts for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.comment.repository;

import com.inkwell.comment.entity.CommentLike;
import com.inkwell.comment.entity.CommentLike.CommentLikeId;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/* This interface groups comment like repository behavior so the module keeps a clear responsibility. */
public interface CommentLikeRepository extends JpaRepository<CommentLike, CommentLikeId> {
    boolean existsByCommentIdAndUserId(UUID commentId, UUID userId);
    long countByCommentId(UUID commentId);
    void deleteByCommentIdAndUserId(UUID commentId, UUID userId);
}

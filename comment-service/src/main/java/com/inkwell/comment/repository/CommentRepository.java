/*
 * This source file contains database access contracts for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.comment.repository;

import com.inkwell.comment.entity.Comment;
import com.inkwell.comment.enumtype.CommentStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/* This interface groups comment repository behavior so the module keeps a clear responsibility. */
public interface CommentRepository extends JpaRepository<Comment, UUID> {
    List<Comment> findByPostIdOrderByCreatedAtAsc(UUID postId);
    long countByPostIdAndStatus(UUID postId, CommentStatus status);
    long countByStatusNot(CommentStatus status);
}

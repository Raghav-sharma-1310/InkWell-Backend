/*
 * This source file contains request and response data shapes for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.comment.dto.response;

import com.inkwell.comment.enumtype.CommentStatus;
import java.time.LocalDateTime;
import java.util.UUID;

/* This record groups comment response behavior so the module keeps a clear responsibility. */
public record CommentResponse(UUID commentId, UUID postId, UUID authorId, String authorName, UUID parentCommentId, String content, long likesCount, CommentStatus status, LocalDateTime createdAt, LocalDateTime updatedAt, boolean isPostAuthor) {}

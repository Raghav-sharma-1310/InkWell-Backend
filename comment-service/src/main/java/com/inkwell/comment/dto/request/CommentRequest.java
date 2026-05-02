/*
 * This source file contains request and response data shapes for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.comment.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

/* This record groups comment request behavior so the module keeps a clear responsibility. */
public record CommentRequest(UUID postId, UUID parentCommentId, @NotBlank String content) {}

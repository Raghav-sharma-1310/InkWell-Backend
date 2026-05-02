/*
 * This source file contains request and response data shapes for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.comment.dto.request;

import jakarta.validation.constraints.NotBlank;

/* This record groups update comment request behavior so the module keeps a clear responsibility. */
public record UpdateCommentRequest(@NotBlank String content) {}

/*
 * This source file contains request and response data shapes for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.comment.dto.response;

import java.util.UUID;

/* This record groups post meta response behavior so the module keeps a clear responsibility. */
public record PostMetaResponse(UUID postId, UUID authorId, String title, String slug) {}

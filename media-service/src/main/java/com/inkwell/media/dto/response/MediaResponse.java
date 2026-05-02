/*
 * This source file contains request and response data shapes for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.media.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

/* This record groups media response behavior so the module keeps a clear responsibility. */
public record MediaResponse(UUID mediaId, UUID uploaderId, String filename, String originalName, String url, String mimeType, long sizeKb, String altText, UUID linkedPostId, LocalDateTime uploadedAt, boolean deleted) {}

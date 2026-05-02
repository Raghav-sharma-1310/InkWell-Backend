/*
 * This source file contains request and response data shapes for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.post.dto.response;

import com.inkwell.post.enumtype.PostStatus;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/* This record groups post response behavior so the module keeps a clear responsibility. */
public record PostResponse(
    UUID postId,
    UUID authorId,
    String title,
    String slug,
    String content,
    String excerpt,
    String featuredImageUrl,
    PostStatus status,
    Integer readTimeMin,
    Long viewCount,
    Long likesCount,
    String categorySlug,
    Set<String> tagSlugs,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    LocalDateTime publishedAt,
    boolean featured,
    boolean pinned,
    com.inkwell.post.enumtype.PostVisibility visibility,
    LocalDateTime scheduledAt
) {
}

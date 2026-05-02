/*
 * This source file contains request and response data shapes for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.post.dto.request;

import com.inkwell.post.enumtype.PostStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

/* This record groups save post request behavior so the module keeps a clear responsibility. */
public record SavePostRequest(
    @NotBlank @Size(max = 180) String title,
    @NotBlank String content,
    @Size(max = 500) String excerpt,
    @Size(max = 500) String featuredImageUrl,
    String categorySlug,
    Set<String> tagSlugs,
    @NotNull PostStatus status,
    boolean featured,
    boolean pinned,
    com.inkwell.post.enumtype.PostVisibility visibility,
    java.time.LocalDateTime scheduledAt
) {
}

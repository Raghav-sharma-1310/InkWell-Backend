/*
 * This source file contains request and response data shapes for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.category.dto.request;

import java.util.Set;
import java.util.UUID;

/* This record groups taxonomy sync request behavior so the module keeps a clear responsibility. */
public record TaxonomySyncRequest(UUID postId, String categorySlug, Set<String> tagSlugs, boolean published) {}

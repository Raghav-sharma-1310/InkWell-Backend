/*
 * This source file contains request and response data shapes for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.category.dto.response;

import java.util.UUID;

/* This record groups category response behavior so the module keeps a clear responsibility. */
public record CategoryResponse(UUID categoryId, String name, String slug, String description, UUID parentCategoryId, long postCount) {}

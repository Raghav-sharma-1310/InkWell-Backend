/*
 * This source file contains request and response data shapes for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.category.dto.response;

import java.util.UUID;

/* This record groups tag response behavior so the module keeps a clear responsibility. */
public record TagResponse(UUID tagId, String name, String slug, long postCount) {}

/*
 * This source file contains HTTP controller endpoints for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.category.controller;

import com.inkwell.category.dto.ApiResponse;
import com.inkwell.category.dto.request.TaxonomySyncRequest;
import com.inkwell.category.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories/internal")
@RequiredArgsConstructor
/* This class groups internal category controller behavior so the module keeps a clear responsibility. */
public class InternalCategoryController {

    private final CategoryService categoryService;

    @PostMapping("/posts/{postId}/taxonomy")
    // Performs the sync workflow so callers do not duplicate this logic.
    public ApiResponse<Void> sync(@PathVariable String postId, @RequestBody TaxonomySyncRequest request) {
        categoryService.syncTaxonomy(request);
        return ApiResponse.of("Taxonomy synced", null);
    }
}

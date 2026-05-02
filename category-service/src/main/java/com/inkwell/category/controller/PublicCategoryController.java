/*
 * This source file contains HTTP controller endpoints for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.category.controller;

import com.inkwell.category.dto.ApiResponse;
import com.inkwell.category.dto.response.CategoryResponse;
import com.inkwell.category.dto.response.TagResponse;
import com.inkwell.category.service.CategoryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories/public")
@RequiredArgsConstructor
/* This class groups public category controller behavior so the module keeps a clear responsibility. */
public class PublicCategoryController {

    private final CategoryService categoryService;

    @GetMapping("/categories")
    // Defines categories so related behavior stays grouped in one place.
    public ApiResponse<List<CategoryResponse>> categories() { return ApiResponse.of("Categories fetched", categoryService.getCategories()); }

    @GetMapping("/categories/top")
    // Defines top categories so related behavior stays grouped in one place.
    public ApiResponse<List<CategoryResponse>> topCategories() { return ApiResponse.of("Top categories fetched", categoryService.getTop5Categories()); }

    @GetMapping("/categories/active")
    // Defines active categories so related behavior stays grouped in one place.
    public ApiResponse<List<CategoryResponse>> activeCategories() { return ApiResponse.of("Active categories fetched", categoryService.getActiveCategories()); }

    @GetMapping("/tags")
    // Defines tags so related behavior stays grouped in one place.
    public ApiResponse<List<TagResponse>> tags() { return ApiResponse.of("Tags fetched", categoryService.getTags()); }

    @GetMapping("/tags/active")
    // Defines active tags so related behavior stays grouped in one place.
    public ApiResponse<List<TagResponse>> activeTags() { return ApiResponse.of("Active tags fetched", categoryService.getActiveTags()); }

    @GetMapping("/tags/trending")
    // Defines trending tags so related behavior stays grouped in one place.
    public ApiResponse<List<TagResponse>> trendingTags() { return ApiResponse.of("Trending tags fetched", categoryService.trendingTags()); }

    @GetMapping("/categories/{slug}")
    // Performs the get category by slug workflow so callers do not duplicate this logic.
    public ApiResponse<CategoryResponse> getCategoryBySlug(@org.springframework.web.bind.annotation.PathVariable String slug) {
        return ApiResponse.of("Category fetched", categoryService.getBySlug(slug));
    }

    @GetMapping("/tags/{slug}")
    // Performs the get tag by slug workflow so callers do not duplicate this logic.
    public ApiResponse<TagResponse> getTagBySlug(@org.springframework.web.bind.annotation.PathVariable String slug) {
        return ApiResponse.of("Tag fetched", categoryService.getTagBySlug(slug));
    }

    @GetMapping("/posts/{postId}/tags")
    // Performs the get tags by post workflow so callers do not duplicate this logic.
    public ApiResponse<List<TagResponse>> getTagsByPost(@org.springframework.web.bind.annotation.PathVariable java.util.UUID postId) {
        return ApiResponse.of("Tags fetched for post", categoryService.getTagsByPost(postId));
    }

    @GetMapping("/posts/{postId}/categories")
    // Performs the get categories by post workflow so callers do not duplicate this logic.
    public ApiResponse<List<CategoryResponse>> getCategoriesByPost(@org.springframework.web.bind.annotation.PathVariable java.util.UUID postId) {
        return ApiResponse.of("Categories fetched for post", categoryService.getCategoriesByPost(postId));
    }
}

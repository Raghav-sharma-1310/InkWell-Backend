/*
 * This source file contains HTTP controller endpoints for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.post.controller;

import com.inkwell.post.dto.ApiResponse;
import com.inkwell.post.dto.response.PageResponse;
import com.inkwell.post.dto.response.PostResponse;
import com.inkwell.post.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts/public")
@RequiredArgsConstructor
/* This class groups public post controller behavior so the module keeps a clear responsibility. */
public class PublicPostController {

    private final PostService postService;

    @GetMapping
    public ApiResponse<PageResponse<PostResponse>> feed(
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "10") int size,
        @RequestParam(name = "categorySlug", required = false) String categorySlug,
        @RequestParam(name = "tagSlug", required = false) String tagSlug,
        @RequestParam(name = "query", required = false) String query
    ) {
        return ApiResponse.of("Published posts fetched", postService.publicFeed(page, size, categorySlug, tagSlug, query));
    }

    @GetMapping("/{slug}")
    // Defines by slug so related behavior stays grouped in one place.
    public ApiResponse<PostResponse> bySlug(@PathVariable String slug) {
        return ApiResponse.of("Post fetched", postService.getBySlug(slug));
    }

    @GetMapping("/stats")
    // Defines stats so related behavior stays grouped in one place.
    public ApiResponse<java.util.Map<String, Object>> stats() {
        return ApiResponse.of("Platform stats", postService.platformStats());
    }
}

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts/explore")
@RequiredArgsConstructor
/* This class groups explore post controller behavior so the module keeps a clear responsibility. */
public class ExplorePostController {

    private final PostService postService;

    @GetMapping
    public ApiResponse<PageResponse<PostResponse>> explore(
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "10") int size,
        @RequestParam(name = "category", required = false) String category,
        @RequestParam(name = "tag", required = false) String tag,
        @RequestParam(name = "search", required = false) String search
    ) {
        return ApiResponse.of("Explore posts fetched", postService.publicFeed(page, size, category, tag, search));
    }
}

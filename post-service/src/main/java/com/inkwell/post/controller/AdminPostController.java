/*
 * This source file contains HTTP controller endpoints for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.post.controller;

import com.inkwell.post.dto.ApiResponse;
import com.inkwell.post.dto.response.PageResponse;
import com.inkwell.post.dto.response.PostResponse;
import com.inkwell.post.service.PostService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
/* This class groups admin post controller behavior so the module keeps a clear responsibility. */
public class AdminPostController {

    private final PostService postService;

    @GetMapping
    public ApiResponse<PageResponse<PostResponse>> search(
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "10") int size,
        @RequestParam(name = "status", required = false) String status,
        @RequestParam(name = "query", required = false) String query
    ) {
        return ApiResponse.of("Admin posts fetched", postService.adminSearch(page, size, status, query));
    }

    @PatchMapping("/{postId}/feature")
    // Defines feature so related behavior stays grouped in one place.
    public ApiResponse<PostResponse> feature(@PathVariable UUID postId, @RequestParam(name = "featured") boolean featured) {
        return ApiResponse.of("Post feature status updated", postService.featurePost(postId, featured));
    }

    @DeleteMapping("/{postId}")
    // Performs the delete workflow so callers do not duplicate this logic.
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID postId) {
        postService.adminDeletePost(postId);
        return ResponseEntity.ok(ApiResponse.of("Post deleted successfully", null));
    }
}

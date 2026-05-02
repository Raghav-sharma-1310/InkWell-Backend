/*
 * This source file contains HTTP controller endpoints for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.post.controller;

import com.inkwell.post.dto.ApiResponse;
import com.inkwell.post.dto.request.SavePostRequest;
import com.inkwell.post.dto.response.LikeResponse;
import com.inkwell.post.dto.response.PageResponse;
import com.inkwell.post.dto.response.PostResponse;
import com.inkwell.post.service.PostService;
import com.inkwell.post.util.SecurityUtils;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts/author")
@RequiredArgsConstructor
/* This class groups author post controller behavior so the module keeps a clear responsibility. */
public class AuthorPostController {

    private final PostService postService;

    @PostMapping
    // Performs the create workflow so callers do not duplicate this logic.
    public ApiResponse<PostResponse> create(@Valid @RequestBody SavePostRequest request) {
        return ApiResponse.of("Post created", postService.createPost(SecurityUtils.currentPrincipal(), request));
    }

    @PutMapping("/{postId}")
    // Performs the update workflow so callers do not duplicate this logic.
    public ApiResponse<PostResponse> update(@PathVariable UUID postId, @Valid @RequestBody SavePostRequest request) {
        return ApiResponse.of("Post updated", postService.updatePost(postId, SecurityUtils.currentPrincipal(), request));
    }

    @GetMapping
    // Defines my posts so related behavior stays grouped in one place.
    public ApiResponse<PageResponse<PostResponse>> myPosts(@RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "10") int size) {
        return ApiResponse.of("Author posts fetched", postService.authorPosts(SecurityUtils.currentPrincipal(), page, size));
    }

    @GetMapping("/{postId}")
    // Defines by id so related behavior stays grouped in one place.
    public ApiResponse<PostResponse> byId(@PathVariable UUID postId) {
        return ApiResponse.of("Post fetched", postService.getById(postId, SecurityUtils.currentPrincipal()));
    }

    @PostMapping("/{postId}/like")
    // Defines like so related behavior stays grouped in one place.
    public ApiResponse<LikeResponse> like(@PathVariable UUID postId) {
        return ApiResponse.of("Post like status updated", postService.toggleLike(postId, SecurityUtils.currentPrincipal()));
    }

    @DeleteMapping("/{postId}")
    // Performs the delete workflow so callers do not duplicate this logic.
    public ApiResponse<Void> delete(@PathVariable UUID postId) {
        postService.deletePost(postId, SecurityUtils.currentPrincipal());
        return ApiResponse.of("Post deleted", null);
    }
}

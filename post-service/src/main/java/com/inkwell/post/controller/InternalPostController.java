/*
 * This source file contains HTTP controller endpoints for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.post.controller;

import com.inkwell.post.dto.ApiResponse;
import com.inkwell.post.dto.response.PostMetaResponse;
import com.inkwell.post.service.PostService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts/internal")
@RequiredArgsConstructor
/* This class groups internal post controller behavior so the module keeps a clear responsibility. */
public class InternalPostController {

    private final PostService postService;

    @GetMapping("/{postId}/meta")
    // Defines meta so related behavior stays grouped in one place.
    public ApiResponse<PostMetaResponse> meta(@PathVariable UUID postId) {
        com.inkwell.post.dto.response.PostResponse post = postService.getMeta(postId);
        return ApiResponse.of("Post meta fetched", new PostMetaResponse(post.postId(), post.authorId(), post.title(), post.slug()));
    }
}

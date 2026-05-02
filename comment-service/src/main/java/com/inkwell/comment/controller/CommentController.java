/*
 * This source file contains HTTP controller endpoints for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.comment.controller;

import com.inkwell.comment.dto.ApiResponse;
import com.inkwell.comment.dto.request.CommentRequest;
import com.inkwell.comment.dto.request.UpdateCommentRequest;
import com.inkwell.comment.dto.response.CommentResponse;
import com.inkwell.comment.dto.response.LikeResponse;
import com.inkwell.comment.enumtype.CommentStatus;
import com.inkwell.comment.service.CommentService;
import com.inkwell.comment.util.SecurityUtils;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
/* This class groups comment controller behavior so the module keeps a clear responsibility. */
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/public/post/{postId}")
    // Defines by post so related behavior stays grouped in one place.
    public ApiResponse<List<CommentResponse>> byPost(@PathVariable UUID postId) { return ApiResponse.of("Comments fetched", commentService.byPost(postId)); }

    @PostMapping
    // Defines add so related behavior stays grouped in one place.
    public ApiResponse<CommentResponse> add(@Valid @RequestBody CommentRequest request) { return ApiResponse.of("Comment added", commentService.addComment(SecurityUtils.currentPrincipal(), request)); }

    @PutMapping("/{commentId}")
    // Performs the update workflow so callers do not duplicate this logic.
    public ApiResponse<CommentResponse> update(@PathVariable UUID commentId, @Valid @RequestBody UpdateCommentRequest request) { return ApiResponse.of("Comment updated", commentService.updateOwn(commentId, SecurityUtils.currentPrincipal(), request)); }

    @DeleteMapping("/{commentId}")
    // Performs the delete workflow so callers do not duplicate this logic.
    public ApiResponse<Void> delete(@PathVariable UUID commentId) { commentService.deleteOwn(commentId, SecurityUtils.currentPrincipal()); return ApiResponse.of("Comment deleted", null); }

    @PostMapping("/{commentId}/like")
    // Defines like so related behavior stays grouped in one place.
    public ApiResponse<LikeResponse> like(@PathVariable UUID commentId) { return ApiResponse.of("Comment like toggled", commentService.toggleLike(commentId, SecurityUtils.currentPrincipal())); }

    /**
     * Author reply to a comment on their own post.
     * Only the post author can use this endpoint — enforced in service layer.
     */
    @PostMapping("/{commentId}/reply")
    public ApiResponse<CommentResponse> reply(@PathVariable UUID commentId, @RequestBody Map<String, String> body) {
        String content = body.getOrDefault("content", "");
        return ApiResponse.of("Author reply added", commentService.replyToComment(commentId, content, SecurityUtils.currentPrincipal()));
    }

    @PatchMapping("/author/{commentId}/approve")
    // Defines approve so related behavior stays grouped in one place.
    public ApiResponse<CommentResponse> approve(@PathVariable UUID commentId, @RequestParam(name = "postId") UUID postId) { return ApiResponse.of("Comment approved", commentService.moderate(commentId, postId, SecurityUtils.currentPrincipal(), CommentStatus.APPROVED)); }

    @PatchMapping("/author/{commentId}/reject")
    // Defines reject so related behavior stays grouped in one place.
    public ApiResponse<CommentResponse> reject(@PathVariable UUID commentId, @RequestParam(name = "postId") UUID postId) { return ApiResponse.of("Comment rejected", commentService.moderate(commentId, postId, SecurityUtils.currentPrincipal(), CommentStatus.REJECTED)); }

    @PatchMapping("/admin/{commentId}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    // Performs the admin delete workflow so callers do not duplicate this logic.
    public ApiResponse<CommentResponse> adminDelete(@PathVariable UUID commentId, @RequestParam(name = "postId") UUID postId) { return ApiResponse.of("Comment deleted by admin", commentService.moderate(commentId, postId, SecurityUtils.currentPrincipal(), CommentStatus.DELETED)); }

    @GetMapping("/admin/count")
    @PreAuthorize("hasRole('ADMIN')")
    // Defines count all so related behavior stays grouped in one place.
    public ApiResponse<Long> countAll() {
        return ApiResponse.of("Total comments count fetched", commentService.countAll());
    }
}

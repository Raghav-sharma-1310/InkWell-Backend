/*
 * This source file contains HTTP controller endpoints for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.post.controller;

import com.inkwell.post.dto.ApiResponse;
import com.inkwell.post.dto.response.LikeResponse;
import com.inkwell.post.dto.response.PostResponse;
import com.inkwell.post.service.FollowBookmarkService;
import com.inkwell.post.service.PostService;
import com.inkwell.post.util.SecurityUtils;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authenticated endpoints accessible to ALL logged-in users (READER, AUTHOR, ADMIN).
 * Separated from AuthorPostController which requires AUTHOR/ADMIN role.
 */
@RestController
@RequestMapping("/api/posts/reader")
@RequiredArgsConstructor
/* This class groups reader post controller behavior so the module keeps a clear responsibility. */
public class ReaderPostController {

    private final PostService postService;
    private final FollowBookmarkService followBookmarkService;

    // ── Like ────────────────────────────────────────────

    @PostMapping("/{postId}/like")
    public ApiResponse<LikeResponse> like(@PathVariable UUID postId) {
        return ApiResponse.of("Post like status updated", postService.toggleLike(postId, SecurityUtils.currentPrincipal()));
    }

    // ── Follow Author ────────────────────────────────────

    @PostMapping("/{authorId}/follow")
    public ApiResponse<Map<String, Object>> toggleFollow(@PathVariable UUID authorId) {
        return ApiResponse.of("Follow status updated",
            followBookmarkService.toggleFollow(authorId, SecurityUtils.currentPrincipal()));
    }

    @GetMapping("/following")
    // Performs the get following workflow so callers do not duplicate this logic.
    public ApiResponse<List<UUID>> getFollowing() {
        return ApiResponse.of("Following list",
            followBookmarkService.getFollowingIds(SecurityUtils.currentPrincipal()));
    }

    @GetMapping("/{authorId}/follow/status")
    // Performs the get follow status workflow so callers do not duplicate this logic.
    public ApiResponse<Map<String, Object>> getFollowStatus(@PathVariable UUID authorId) {
        return ApiResponse.of("Follow status",
            followBookmarkService.getFollowStatus(authorId, SecurityUtils.currentPrincipal()));
    }

    // ── Bookmark / Save ─────────────────────────────────

    @PostMapping("/{postId}/bookmark")
    public ApiResponse<Map<String, Object>> toggleBookmark(@PathVariable UUID postId) {
        return ApiResponse.of("Bookmark status updated",
            followBookmarkService.toggleBookmark(postId, SecurityUtils.currentPrincipal()));
    }

    @GetMapping("/bookmarks")
    // Performs the get bookmarks workflow so callers do not duplicate this logic.
    public ApiResponse<List<PostResponse>> getBookmarks() {
        return ApiResponse.of("Bookmarked posts",
            followBookmarkService.getBookmarkedPosts(SecurityUtils.currentPrincipal()));
    }

    @GetMapping("/history")
    public ApiResponse<com.inkwell.post.dto.response.PageResponse<PostResponse>> getHistory(
        @org.springframework.web.bind.annotation.RequestParam(name = "page", defaultValue = "0") int page,
        @org.springframework.web.bind.annotation.RequestParam(name = "size", defaultValue = "10") int size
    ) {
        return ApiResponse.of("Reading history",
            followBookmarkService.getHistory(SecurityUtils.currentPrincipal(), page, size));
    }

    @DeleteMapping("/history/clear")
    // Defines clear history so related behavior stays grouped in one place.
    public ApiResponse<Void> clearHistory() {
        followBookmarkService.clearHistory(SecurityUtils.currentPrincipal());
        return ApiResponse.of("History cleared", null);
    }
}

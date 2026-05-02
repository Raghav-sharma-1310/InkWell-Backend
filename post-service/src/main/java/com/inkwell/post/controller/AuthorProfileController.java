/*
 * This source file contains HTTP controller endpoints for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.post.controller;

import com.inkwell.post.dto.ApiResponse;
import com.inkwell.post.service.FollowBookmarkService;
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
 * Public + authenticated endpoints for author profiles & follow feature.
 * Public endpoints (no auth): /api/posts/authors/{authorId}/followers/count, /api/posts/authors/{authorId}/posts
 * Authenticated: follow/unfollow
 */
@RestController
@RequestMapping("/api/posts/authors")
@RequiredArgsConstructor
/* This class groups author profile controller behavior so the module keeps a clear responsibility. */
public class AuthorProfileController {

    private final FollowBookmarkService followBookmarkService;

    // ── Public: anyone can see follower count ──────────────────

    @GetMapping("/{authorId}/followers/count")
    public ApiResponse<Map<String, Object>> followersCount(@PathVariable UUID authorId) {
        long count = followBookmarkService.getFollowersCount(authorId);
        return ApiResponse.of("Followers count", Map.of("followersCount", count));
    }

    // ── Authenticated: follow / unfollow ───────────────────────

    @PostMapping("/{authorId}/follow")
    public ApiResponse<Map<String, Object>> follow(@PathVariable UUID authorId) {
        return ApiResponse.of("Follow status updated",
            followBookmarkService.toggleFollow(authorId, SecurityUtils.currentPrincipal()));
    }

    @DeleteMapping("/{authorId}/unfollow")
    // Defines unfollow so related behavior stays grouped in one place.
    public ApiResponse<Map<String, Object>> unfollow(@PathVariable UUID authorId) {
        return ApiResponse.of("Unfollowed",
            followBookmarkService.toggleFollow(authorId, SecurityUtils.currentPrincipal()));
    }

    @GetMapping("/{authorId}/follow/status")
    // Defines follow status so related behavior stays grouped in one place.
    public ApiResponse<Map<String, Object>> followStatus(@PathVariable UUID authorId) {
        return ApiResponse.of("Follow status",
            followBookmarkService.getFollowStatus(authorId, SecurityUtils.currentPrincipal()));
    }

    // ── Author Dashboard: my followers ─────────────────────────

    @GetMapping("/me/followers")
    public ApiResponse<List<Map<String, Object>>> myFollowers() {
        return ApiResponse.of("My followers",
            followBookmarkService.getMyFollowers(SecurityUtils.currentPrincipal()));
    }

    @GetMapping("/me/followers/count")
    // Defines my followers count so related behavior stays grouped in one place.
    public ApiResponse<Map<String, Object>> myFollowersCount() {
        long count = followBookmarkService.getFollowersCount(SecurityUtils.currentPrincipal().userUuid());
        return ApiResponse.of("My followers count", Map.of("followersCount", count));
    }
}

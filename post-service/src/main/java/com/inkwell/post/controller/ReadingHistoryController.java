/*
 * This source file contains HTTP controller endpoints for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.post.controller;

import com.inkwell.post.dto.ApiResponse;
import com.inkwell.post.dto.response.PageResponse;
import com.inkwell.post.dto.response.PostResponse;
import com.inkwell.post.service.FollowBookmarkService;
import com.inkwell.post.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.Map;

@RestController
@RequestMapping("/api/reading-history")
@RequiredArgsConstructor
/* This class groups reading history controller behavior so the module keeps a clear responsibility. */
public class ReadingHistoryController {

    private final FollowBookmarkService followBookmarkService;

    @PostMapping
    // Performs the save history workflow so callers do not duplicate this logic.
    public ApiResponse<Map<String, String>> saveHistory(@RequestBody Map<String, String> body) {
        String slug = body.get("postSlug");
        if (slug == null || slug.isBlank()) {
            slug = body.get("slug");
        }
        followBookmarkService.recordHistory(slug, SecurityUtils.currentPrincipal());
        return ApiResponse.of("History recorded", Map.of("status", "success"));
    }

    @GetMapping("/me")
    public ApiResponse<PageResponse<PostResponse>> getMyHistory(
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "50") int size
    ) {
        return ApiResponse.of("Reading history",
            followBookmarkService.getHistory(SecurityUtils.currentPrincipal(), page, size));
    }

    @DeleteMapping("/{id}")
    // Performs the delete history item workflow so callers do not duplicate this logic.
    public ApiResponse<Void> deleteHistoryItem(@PathVariable UUID id) {
        followBookmarkService.deleteHistoryItem(id, SecurityUtils.currentPrincipal());
        return ApiResponse.of("History item deleted", null);
    }

    @DeleteMapping("/clear")
    // Defines clear history so related behavior stays grouped in one place.
    public ApiResponse<Void> clearHistory() {
        followBookmarkService.clearHistory(SecurityUtils.currentPrincipal());
        return ApiResponse.of("History cleared", null);
    }
}

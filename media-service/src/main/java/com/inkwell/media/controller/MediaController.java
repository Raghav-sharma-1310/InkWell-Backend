/*
 * This source file contains HTTP controller endpoints for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.media.controller;

import com.inkwell.media.dto.ApiResponse;
import com.inkwell.media.dto.response.MediaResponse;
import com.inkwell.media.service.MediaService;
import com.inkwell.media.util.SecurityUtils;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
/* This class groups media controller behavior so the module keeps a clear responsibility. */
public class MediaController {

    private final MediaService mediaService;

    @Value("${app.storage.local-dir:uploads/media}")
    private String localDir;

    @PostMapping("/author/upload")
    // Performs the upload workflow so callers do not duplicate this logic.
    public ApiResponse<MediaResponse> upload(@RequestPart MultipartFile file, @RequestParam(name = "altText", required = false) String altText, @RequestParam(name = "linkedPostId", required = false) UUID linkedPostId) {
        return ApiResponse.of("Media uploaded", mediaService.upload(SecurityUtils.currentPrincipal(), file, altText, linkedPostId));
    }

    @PostMapping("/user/upload-avatar")
    // Performs the upload avatar workflow so callers do not duplicate this logic.
    public ApiResponse<MediaResponse> uploadAvatar(@RequestPart MultipartFile file) {
        return ApiResponse.of("Avatar uploaded", mediaService.upload(SecurityUtils.currentPrincipal(), file, "Profile Avatar", null));
    }

    @GetMapping("/author/library")
    // Defines my library so related behavior stays grouped in one place.
    public ApiResponse<List<MediaResponse>> myLibrary() { return ApiResponse.of("Media library fetched", mediaService.myLibrary(SecurityUtils.currentPrincipal())); }

    @GetMapping("/public/post/{postId}")
    // Defines by post so related behavior stays grouped in one place.
    public ApiResponse<List<MediaResponse>> byPost(@PathVariable UUID postId) { return ApiResponse.of("Post media fetched", mediaService.byPost(postId)); }

    @GetMapping("/public/files/{filename:.+}")
    // Defines file so related behavior stays grouped in one place.
    public ResponseEntity<Resource> file(@PathVariable String filename) throws java.io.IOException {
        Resource resource = new UrlResource(java.nio.file.Path.of(localDir, filename).toUri());
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(resource);
    }

    @PatchMapping("/{mediaId}/alt")
    // Performs the update alt workflow so callers do not duplicate this logic.
    public ApiResponse<MediaResponse> updateAlt(@PathVariable UUID mediaId, @RequestParam(name = "altText") String altText) { return ApiResponse.of("Alt text updated", mediaService.updateAlt(mediaId, altText, SecurityUtils.currentPrincipal())); }

    @PatchMapping("/{mediaId}/link")
    // Defines link so related behavior stays grouped in one place.
    public ApiResponse<MediaResponse> link(@PathVariable UUID mediaId, @RequestParam(name = "linkedPostId") UUID linkedPostId) { return ApiResponse.of("Media linked", mediaService.linkToPost(mediaId, linkedPostId, SecurityUtils.currentPrincipal())); }

    @DeleteMapping("/{mediaId}")
    // Performs the delete workflow so callers do not duplicate this logic.
    public ApiResponse<Void> delete(@PathVariable UUID mediaId) { mediaService.delete(mediaId, SecurityUtils.currentPrincipal()); return ApiResponse.of("Media deleted", null); }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    // Defines all so related behavior stays grouped in one place.
    public ApiResponse<List<MediaResponse>> all() { return ApiResponse.of("All media fetched", mediaService.all()); }
}

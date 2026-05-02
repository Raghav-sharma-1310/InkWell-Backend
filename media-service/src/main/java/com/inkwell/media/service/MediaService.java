/*
 * This source file contains business workflow and validation logic for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.media.service;

import com.inkwell.media.dto.response.MediaResponse;
import com.inkwell.media.entity.MediaFile;
import com.inkwell.media.exception.ResourceNotFoundException;
import com.inkwell.media.repository.MediaRepository;
import com.inkwell.media.security.GatewayUserPrincipal;
import com.inkwell.media.storage.StorageService;
import com.inkwell.media.storage.StoredFile;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
/* This class groups media service behavior so the module keeps a clear responsibility. */
public class MediaService {

    private final MediaRepository mediaRepository;
    private final StorageService storageService;

    @Transactional
    // Performs the upload workflow so callers do not duplicate this logic.
    public MediaResponse upload(GatewayUserPrincipal principal, MultipartFile file, String altText, UUID linkedPostId) {
        StoredFile storedFile = storageService.store(file);
        MediaFile media = mediaRepository.save(MediaFile.builder()
            .uploaderId(principal.userUuid())
            .filename(storedFile.filename())
            .originalName(file.getOriginalFilename())
            .url(storedFile.url())
            .mimeType(file.getContentType())
            .sizeKb(storedFile.sizeKb())
            .altText(altText)
            .linkedPostId(linkedPostId)
            .deleted(false)
            .build());
        return toResponse(media);
    }

    @Transactional(readOnly = true)
    // Defines my library so related behavior stays grouped in one place.
    public List<MediaResponse> myLibrary(GatewayUserPrincipal principal) { return mediaRepository.findByUploaderIdAndDeletedFalse(principal.userUuid()).stream().map(this::toResponse).toList(); }

    @Transactional(readOnly = true)
    // Defines by post so related behavior stays grouped in one place.
    public List<MediaResponse> byPost(UUID postId) { return mediaRepository.findByLinkedPostIdAndDeletedFalse(postId).stream().map(this::toResponse).toList(); }

    @Transactional(readOnly = true)
    // Defines all so related behavior stays grouped in one place.
    public List<MediaResponse> all() { return mediaRepository.findByDeletedFalse().stream().map(this::toResponse).toList(); }

    @Transactional
    // Performs the update alt workflow so callers do not duplicate this logic.
    public MediaResponse updateAlt(UUID mediaId, String altText, GatewayUserPrincipal principal) {
        MediaFile media = getMedia(mediaId);
        if (!principal.isAdmin() && !media.getUploaderId().equals(principal.userUuid())) { throw new IllegalStateException("Access denied"); }
        media.setAltText(altText);
        return toResponse(mediaRepository.save(media));
    }

    @Transactional
    // Defines link to post so related behavior stays grouped in one place.
    public MediaResponse linkToPost(UUID mediaId, UUID postId, GatewayUserPrincipal principal) {
        MediaFile media = getMedia(mediaId);
        if (!principal.isAdmin() && !media.getUploaderId().equals(principal.userUuid())) { throw new IllegalStateException("Access denied"); }
        media.setLinkedPostId(postId);
        return toResponse(mediaRepository.save(media));
    }

    @Transactional
    // Performs the delete workflow so callers do not duplicate this logic.
    public void delete(UUID mediaId, GatewayUserPrincipal principal) {
        MediaFile media = getMedia(mediaId);
        if (!principal.isAdmin() && !media.getUploaderId().equals(principal.userUuid())) { throw new IllegalStateException("Access denied"); }
        media.setDeleted(true);
        mediaRepository.save(media);
        storageService.delete(media.getFilename());
    }

    // Performs the get media workflow so callers do not duplicate this logic.
    private MediaFile getMedia(UUID mediaId) { return mediaRepository.findById(mediaId).orElseThrow(() -> new ResourceNotFoundException("Media not found")); }

    // Defines to response so related behavior stays grouped in one place.
    private MediaResponse toResponse(MediaFile media) { return new MediaResponse(media.getMediaId(), media.getUploaderId(), media.getFilename(), media.getOriginalName(), media.getUrl(), media.getMimeType(), media.getSizeKb(), media.getAltText(), media.getLinkedPostId(), media.getUploadedAt(), media.isDeleted()); }
}

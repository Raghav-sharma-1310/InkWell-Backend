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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
/* This class groups media service test behavior so the module keeps a clear responsibility. */
class MediaServiceTest {

    @Mock private MediaRepository mediaRepository;
    @Mock private StorageService storageService;
    @Mock private MultipartFile multipartFile;

    @InjectMocks private MediaService mediaService;

    private UUID uploaderId;
    private UUID mediaId;
    private GatewayUserPrincipal principal;
    private GatewayUserPrincipal adminPrincipal;
    private MediaFile sampleMedia;

    @BeforeEach
    void setUp() {
        uploaderId = UUID.randomUUID();
        mediaId = UUID.randomUUID();
        principal = new GatewayUserPrincipal(uploaderId.toString(), "testuser", "test@inkwell.com", "AUTHOR");
        adminPrincipal = new GatewayUserPrincipal(UUID.randomUUID().toString(), "admin", "admin@inkwell.com", "ADMIN");
        sampleMedia = MediaFile.builder()
                .mediaId(mediaId)
                .uploaderId(uploaderId)
                .filename("test.jpg")
                .originalName("test.jpg")
                .url("/uploads/test.jpg")
                .mimeType("image/jpeg")
                .sizeKb(100)
                .altText("Test image")
                .deleted(false)
                .build();
    }

    @Test
    @DisplayName("Should upload a file")
    void upload() {
        StoredFile storedFile = new StoredFile("stored.jpg", "/uploads/stored.jpg", 100);
        when(storageService.store(multipartFile)).thenReturn(storedFile);
        when(multipartFile.getOriginalFilename()).thenReturn("original.jpg");
        when(multipartFile.getContentType()).thenReturn("image/jpeg");
        when(mediaRepository.save(any(MediaFile.class))).thenReturn(sampleMedia);

        MediaResponse response = mediaService.upload(principal, multipartFile, "Alt text", null);

        assertThat(response).isNotNull();
        assertThat(response.filename()).isEqualTo("test.jpg");
        verify(storageService).store(multipartFile);
    }

    @Test
    @DisplayName("Should list user's media library")
    void myLibrary() {
        when(mediaRepository.findByUploaderIdAndDeletedFalse(uploaderId)).thenReturn(List.of(sampleMedia));
        List<MediaResponse> result = mediaService.myLibrary(principal);
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Should list media by post")
    void byPost() {
        UUID postId = UUID.randomUUID();
        when(mediaRepository.findByLinkedPostIdAndDeletedFalse(postId)).thenReturn(List.of(sampleMedia));
        assertThat(mediaService.byPost(postId)).hasSize(1);
    }

    @Test
    @DisplayName("Should list all media")
    void all() {
        when(mediaRepository.findByDeletedFalse()).thenReturn(List.of(sampleMedia));
        assertThat(mediaService.all()).hasSize(1);
    }

    @Test
    @DisplayName("Should update alt text by owner")
    void updateAlt() {
        when(mediaRepository.findById(mediaId)).thenReturn(Optional.of(sampleMedia));
        when(mediaRepository.save(any(MediaFile.class))).thenReturn(sampleMedia);

        MediaResponse response = mediaService.updateAlt(mediaId, "New alt", principal);
        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("Should update alt text by admin")
    void updateAltAdmin() {
        when(mediaRepository.findById(mediaId)).thenReturn(Optional.of(sampleMedia));
        when(mediaRepository.save(any(MediaFile.class))).thenReturn(sampleMedia);

        MediaResponse response = mediaService.updateAlt(mediaId, "Admin alt", adminPrincipal);
        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("Should deny alt text update by non-owner")
    void updateAltDenied() {
        GatewayUserPrincipal other = new GatewayUserPrincipal(UUID.randomUUID().toString(), "other", "other@inkwell.com", "AUTHOR");
        when(mediaRepository.findById(mediaId)).thenReturn(Optional.of(sampleMedia));

        assertThatThrownBy(() -> mediaService.updateAlt(mediaId, "New alt", other))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Should link media to post")
    void linkToPost() {
        UUID postId = UUID.randomUUID();
        when(mediaRepository.findById(mediaId)).thenReturn(Optional.of(sampleMedia));
        when(mediaRepository.save(any(MediaFile.class))).thenReturn(sampleMedia);

        mediaService.linkToPost(mediaId, postId, principal);
        verify(mediaRepository).save(any(MediaFile.class));
    }

    @Test
    @DisplayName("Should soft delete media by owner")
    void delete() {
        when(mediaRepository.findById(mediaId)).thenReturn(Optional.of(sampleMedia));

        mediaService.delete(mediaId, principal);

        verify(mediaRepository).save(argThat(MediaFile::isDeleted));
        verify(storageService).delete("test.jpg");
    }

    @Test
    @DisplayName("Should throw when media not found")
    void notFound() {
        when(mediaRepository.findById(mediaId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> mediaService.updateAlt(mediaId, "alt", principal))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

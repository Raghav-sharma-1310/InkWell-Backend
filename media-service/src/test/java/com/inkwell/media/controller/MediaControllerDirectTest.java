/*
 * This source file contains HTTP controller endpoints for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.media.controller;

import com.inkwell.media.dto.response.MediaResponse;
import com.inkwell.media.security.GatewayUserPrincipal;
import com.inkwell.media.service.MediaService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/* This class groups media controller direct test behavior so the module keeps a clear responsibility. */
class MediaControllerDirectTest {

    private final MediaService mediaService = mock(MediaService.class);
    private final MediaController controller = new MediaController(mediaService);
    private final UUID mediaId = UUID.randomUUID();
    private final UUID postId = UUID.randomUUID();
    private final GatewayUserPrincipal principal = new GatewayUserPrincipal(UUID.randomUUID().toString(), "author", "author@test.com", "AUTHOR");
    private final MediaResponse media = new MediaResponse(mediaId, principal.userUuid(), "file.png", "file.png", "/files/file.png", "image/png", 12L, "alt", postId, LocalDateTime.now(), false);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void delegatesMediaEndpoints() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
        MockMultipartFile file = new MockMultipartFile("file", "file.png", "image/png", "content".getBytes());
        ReflectionTestUtils.setField(controller, "localDir", ".");
        when(mediaService.upload(principal, file, "alt", postId)).thenReturn(media);
        when(mediaService.upload(principal, file, "Profile Avatar", null)).thenReturn(media);
        when(mediaService.myLibrary(principal)).thenReturn(List.of(media));
        when(mediaService.byPost(postId)).thenReturn(List.of(media));
        when(mediaService.updateAlt(mediaId, "new alt", principal)).thenReturn(media);
        when(mediaService.linkToPost(mediaId, postId, principal)).thenReturn(media);
        when(mediaService.all()).thenReturn(List.of(media));

        assertThat(controller.upload(file, "alt", postId).data()).isEqualTo(media);
        assertThat(controller.uploadAvatar(file).data()).isEqualTo(media);
        assertThat(controller.myLibrary().data()).containsExactly(media);
        assertThat(controller.byPost(postId).data()).containsExactly(media);
        assertThat(controller.updateAlt(mediaId, "new alt").data()).isEqualTo(media);
        assertThat(controller.link(mediaId, postId).data()).isEqualTo(media);
        assertThat(controller.delete(mediaId).message()).isEqualTo("Media deleted");
        assertThat(controller.all().data()).containsExactly(media);
        assertThat(new ServiceInfoController().root()).containsEntry("service", "media-service");
        verify(mediaService).delete(mediaId, principal);
    }
}

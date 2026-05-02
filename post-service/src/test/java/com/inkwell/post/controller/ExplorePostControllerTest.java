/*
 * This source file contains HTTP controller endpoints for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.post.controller;

import com.inkwell.post.dto.ApiResponse;
import com.inkwell.post.dto.response.PageResponse;
import com.inkwell.post.dto.response.PostResponse;
import com.inkwell.post.service.PostService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
/* This class groups explore post controller test behavior so the module keeps a clear responsibility. */
class ExplorePostControllerTest {

    @Mock private PostService postService;
    @InjectMocks private ExplorePostController controller;

    @Test
    @DisplayName("Should call publicFeed with correct parameters")
    void exploreDefaultParams() {
        PageResponse<PostResponse> pageResponse = new PageResponse<>(List.of(), 0, 10, 0, 0, true, true);
        when(postService.publicFeed(0, 10, null, null, null)).thenReturn(pageResponse);

        ApiResponse<PageResponse<PostResponse>> response = controller.explore(0, 10, null, null, null);

        assertThat(response.data()).isEqualTo(pageResponse);
        verify(postService).publicFeed(0, 10, null, null, null);
    }

    @Test
    @DisplayName("Should pass filter parameters correctly")
    void exploreWithFilters() {
        PageResponse<PostResponse> pageResponse = new PageResponse<>(List.of(), 0, 10, 0, 0, true, true);
        when(postService.publicFeed(0, 10, "tech", "java", "spring")).thenReturn(pageResponse);

        controller.explore(0, 10, "tech", "java", "spring");

        verify(postService).publicFeed(0, 10, "tech", "java", "spring");
    }
}

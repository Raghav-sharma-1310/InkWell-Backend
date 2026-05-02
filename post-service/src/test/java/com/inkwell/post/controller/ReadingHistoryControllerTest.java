/*
 * This source file contains HTTP controller endpoints for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.post.controller;

import com.inkwell.post.dto.ApiResponse;
import com.inkwell.post.dto.response.PageResponse;
import com.inkwell.post.dto.response.PostResponse;
import com.inkwell.post.security.GatewayUserPrincipal;
import com.inkwell.post.service.FollowBookmarkService;
import com.inkwell.post.util.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
/* This class groups reading history controller test behavior so the module keeps a clear responsibility. */
class ReadingHistoryControllerTest {

    @Mock
    private FollowBookmarkService followBookmarkService;

    @InjectMocks
    private ReadingHistoryController readingHistoryController;

    private MockedStatic<SecurityUtils> securityUtilsMock;
    private GatewayUserPrincipal principal;

    @BeforeEach
    void setUp() {
        principal = new GatewayUserPrincipal(UUID.randomUUID().toString(), "user", "READER", "user@test.com", "PRO", "ACTIVE");
        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::currentPrincipal).thenReturn(principal);
    }

    @AfterEach
    void tearDown() {
        if (securityUtilsMock != null) {
            securityUtilsMock.close();
        }
    }

    @Test
    void testSaveHistory() {
        Map<String, String> body = Map.of("postSlug", "test-slug");
        ApiResponse<Map<String, String>> res = readingHistoryController.saveHistory(body);
        assertNotNull(res.data());
        assertEquals("History recorded", res.message());
        verify(followBookmarkService).recordHistory("test-slug", principal);
    }

    @Test
    void testGetMyHistory() {
        PageResponse<PostResponse> page = new PageResponse<PostResponse>(List.of(), 0, 10, 0L, 1, true, true);
        when(followBookmarkService.getHistory(principal, 0, 50)).thenReturn(page);
        ApiResponse<PageResponse<PostResponse>> res = readingHistoryController.getMyHistory(0, 50);
        assertNotNull(res.data());
        assertEquals("Reading history", res.message());
    }

    @Test
    void testDeleteHistoryItem() {
        UUID id = UUID.randomUUID();
        ApiResponse<Void> res = readingHistoryController.deleteHistoryItem(id);
        assertEquals("History item deleted", res.message());
        verify(followBookmarkService).deleteHistoryItem(id, principal);
    }

    @Test
    void testClearHistory() {
        ApiResponse<Void> res = readingHistoryController.clearHistory();
        assertEquals("History cleared", res.message());
        verify(followBookmarkService).clearHistory(principal);
    }
}

/*
 * This source file contains HTTP controller endpoints for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.controller;

import com.inkwell.auth.dto.request.FeedbackMessageRequest;
import com.inkwell.auth.dto.request.FeedbackStatusUpdateRequest;
import com.inkwell.auth.dto.response.FeedbackReportResponse;
import com.inkwell.auth.enumtype.FeedbackStatus;
import com.inkwell.auth.service.FeedbackService;
import com.inkwell.auth.util.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
/* This class groups feedback controller test behavior so the module keeps a clear responsibility. */
class FeedbackControllerTest {

    @Mock
    private FeedbackService feedbackService;

    @InjectMocks
    private FeedbackController feedbackController;

    @InjectMocks
    private AdminFeedbackController adminFeedbackController;

    private UUID reportId;
    private UUID userId;
    private FeedbackReportResponse response;
    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        reportId = UUID.randomUUID();
        userId = UUID.randomUUID();
        response = new FeedbackReportResponse(reportId, userId, "user", "test@test.com", "User Full Name", FeedbackStatus.OPEN, "url", List.of(), LocalDateTime.now(), LocalDateTime.now());

        com.inkwell.auth.security.GatewayUserPrincipal principal = new com.inkwell.auth.security.GatewayUserPrincipal(
            userId.toString(), "admin", "ADMIN", "admin@test.com");
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
    void testSubmitFeedback() {
        when(feedbackService.submitFeedback(eq(userId), any())).thenReturn(response);
        var res = feedbackController.submitFeedback(new FeedbackMessageRequest("msg", "url"));
        assertEquals(HttpStatus.OK.value(), res.getStatusCode().value());
    }

    @Test
    void testGetMyReports() {
        when(feedbackService.getUserReports(userId)).thenReturn(List.of(response));
        var res = feedbackController.getMyReports();
        assertEquals(HttpStatus.OK.value(), res.getStatusCode().value());
    }

    @Test
    void testAdminGetAll() {
        when(feedbackService.getAllReports()).thenReturn(List.of(response));
        var res = adminFeedbackController.getAllReports();
        assertEquals(HttpStatus.OK.value(), res.getStatusCode().value());
    }

    @Test
    void testAdminGetReport() {
        when(feedbackService.getReportById(reportId)).thenReturn(response);
        var res = adminFeedbackController.getReport(reportId);
        assertEquals(HttpStatus.OK.value(), res.getStatusCode().value());
    }

    @Test
    void testAdminUpdateStatus() {
        when(feedbackService.updateStatus(eq(reportId), any())).thenReturn(response);
        var res = adminFeedbackController.updateStatus(reportId, new FeedbackStatusUpdateRequest(FeedbackStatus.RESOLVED));
        assertEquals(HttpStatus.OK.value(), res.getStatusCode().value());
    }

    @Test
    void testAdminReply() {
        when(feedbackService.addAdminReply(eq(reportId), eq(userId), any(), any())).thenReturn(response);
        var res = adminFeedbackController.reply(reportId, new FeedbackMessageRequest("msg", null));
        assertEquals(HttpStatus.OK.value(), res.getStatusCode().value());
    }
}

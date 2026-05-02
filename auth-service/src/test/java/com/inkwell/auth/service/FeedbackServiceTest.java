/*
 * Codex documentation pass: this source file contains business workflow and validation logic for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.service;

import com.inkwell.auth.dto.request.FeedbackMessageRequest;
import com.inkwell.auth.dto.response.FeedbackReportResponse;
import com.inkwell.auth.entity.FeedbackReport;
import com.inkwell.auth.entity.User;
import com.inkwell.auth.enumtype.FeedbackStatus;
import com.inkwell.auth.enumtype.Role;
import com.inkwell.auth.repository.FeedbackReportRepository;
import com.inkwell.auth.repository.UserRepository;
import com.inkwell.auth.security.GatewayUserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
/* This class groups feedback service test behavior so the module keeps a clear responsibility. */
class FeedbackServiceTest {

    @Mock
    private FeedbackReportRepository feedbackReportRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FeedbackService feedbackService;

    private UUID userId;
    private UUID reportId;
    private GatewayUserPrincipal adminPrincipal;
    private User mockUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        reportId = UUID.randomUUID();
        adminPrincipal = new GatewayUserPrincipal(UUID.randomUUID().toString(), "admin", "ADMIN", "admin@test.com");

        mockUser = User.builder()
                .userId(userId)
                .email("user@test.com")
                .role(Role.READER)
                .build();
    }

    @Test
    void testSubmitFeedback_NewReport() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(feedbackReportRepository.findByUserOrderByCreatedAtDesc(mockUser)).thenReturn(List.of());

        FeedbackReport savedReport = FeedbackReport.builder()
                .id(reportId)
                .user(mockUser)
                .status(FeedbackStatus.OPEN)
                .build();
        when(feedbackReportRepository.save(any(FeedbackReport.class))).thenReturn(savedReport);

        FeedbackMessageRequest request = new FeedbackMessageRequest("Test message", "/test");
        FeedbackReportResponse response = feedbackService.submitFeedback(userId, request);

        assertNotNull(response);
        assertEquals(FeedbackStatus.OPEN, response.status());
        verify(feedbackReportRepository, times(2)).save(any(FeedbackReport.class));
    }

    @Test
    void testGetUserReports() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(feedbackReportRepository.findByUserOrderByCreatedAtDesc(mockUser)).thenReturn(List.of());

        List<FeedbackReportResponse> responses = feedbackService.getUserReports(userId);

        assertNotNull(responses);
        assertTrue(responses.isEmpty());
    }

    @Test
    void testGetAllReports() {
        when(feedbackReportRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

        List<FeedbackReportResponse> responses = feedbackService.getAllReports();

        assertNotNull(responses);
        assertTrue(responses.isEmpty());
    }

    @Test
    void testUpdateStatus() {
        FeedbackReport report = FeedbackReport.builder()
                .id(reportId)
                .user(mockUser)
                .status(FeedbackStatus.OPEN)
                .build();
        when(feedbackReportRepository.findById(reportId)).thenReturn(Optional.of(report));
        when(feedbackReportRepository.save(any(FeedbackReport.class))).thenReturn(report);

        FeedbackReportResponse response = feedbackService.updateStatus(reportId, FeedbackStatus.RESOLVED);

        assertNotNull(response);
        assertEquals(FeedbackStatus.RESOLVED, response.status());
    }

    @Test
    void testAdminReply() {
        FeedbackReport report = FeedbackReport.builder()
                .id(reportId)
                .user(mockUser)
                .status(FeedbackStatus.OPEN)
                .build();
        when(feedbackReportRepository.findById(reportId)).thenReturn(Optional.of(report));
        when(feedbackReportRepository.save(any(FeedbackReport.class))).thenReturn(report);

        FeedbackReportResponse response = feedbackService.addAdminReply(reportId, adminPrincipal.userUuid(), adminPrincipal.username(), "Admin reply");

        assertNotNull(response);
        verify(feedbackReportRepository).save(any(FeedbackReport.class));
    }
}

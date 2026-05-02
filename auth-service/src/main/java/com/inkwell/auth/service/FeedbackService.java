/*
 * This source file contains business workflow and validation logic for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.service;

import com.inkwell.auth.dto.request.FeedbackMessageRequest;
import com.inkwell.auth.dto.response.FeedbackMessageResponse;
import com.inkwell.auth.dto.response.FeedbackReportResponse;
import com.inkwell.auth.entity.FeedbackMessage;
import com.inkwell.auth.entity.FeedbackReport;
import com.inkwell.auth.entity.User;
import com.inkwell.auth.enumtype.FeedbackStatus;
import com.inkwell.auth.exception.ResourceNotFoundException;
import com.inkwell.auth.repository.FeedbackReportRepository;
import com.inkwell.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
/* This class groups feedback service behavior so the module keeps a clear responsibility. */
public class FeedbackService {

    private static final String FEEDBACK_REPORT_NOT_FOUND = "Feedback report not found";

    private final FeedbackReportRepository feedbackReportRepository;
    private final UserRepository userRepository;

    /**
     * Submit a new feedback report or add a message to an existing open report.
     */
    @Transactional
    public FeedbackReportResponse submitFeedback(UUID userId, FeedbackMessageRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Find existing open report for this user, or create a new one
        List<FeedbackReport> openReports = feedbackReportRepository.findByUserOrderByCreatedAtDesc(user)
            .stream()
            .filter(r -> r.getStatus() == FeedbackStatus.OPEN || r.getStatus() == FeedbackStatus.IN_PROGRESS)
            .toList();

        FeedbackReport report;
        if (!openReports.isEmpty()) {
            report = openReports.get(0);
        } else {
            report = FeedbackReport.builder()
                .user(user)
                .status(FeedbackStatus.OPEN)
                .pageUrl(request.pageUrl())
                .build();
            report = feedbackReportRepository.save(report);
        }

        // Add the message
        FeedbackMessage message = FeedbackMessage.builder()
            .senderId(userId)
            .senderName(user.getFullName())
            .senderRole(user.getRole().name())
            .content(request.message())
            .build();
        report.addMessage(message);

        if (request.pageUrl() != null && !request.pageUrl().isBlank()) {
            report.setPageUrl(request.pageUrl());
        }

        report = feedbackReportRepository.save(report);
        log.info("Feedback message submitted by user {} on report {}", user.getEmail(), report.getId());

        return toResponse(report);
    }

    /**
     * Get the current user's feedback reports.
     */
    @Transactional(readOnly = true)
    public List<FeedbackReportResponse> getUserReports(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return feedbackReportRepository.findByUserOrderByCreatedAtDesc(user)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    /**
     * Admin: Get all feedback reports.
     */
    @Transactional(readOnly = true)
    public List<FeedbackReportResponse> getAllReports() {
        return feedbackReportRepository.findAllByOrderByCreatedAtDesc()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    /**
     * Admin: Get a specific report by ID.
     */
    @Transactional(readOnly = true)
    public FeedbackReportResponse getReportById(UUID reportId) {
        FeedbackReport report = feedbackReportRepository.findById(reportId)
            .orElseThrow(() -> new ResourceNotFoundException(FEEDBACK_REPORT_NOT_FOUND));
        return toResponse(report);
    }

    /**
     * Admin: Update report status.
     */
    @Transactional
    public FeedbackReportResponse updateStatus(UUID reportId, FeedbackStatus status) {
        FeedbackReport report = feedbackReportRepository.findById(reportId)
            .orElseThrow(() -> new ResourceNotFoundException(FEEDBACK_REPORT_NOT_FOUND));

        report.setStatus(status);
        report = feedbackReportRepository.save(report);
        log.info("Feedback report {} status updated to {}", reportId, status);

        return toResponse(report);
    }

    /**
     * Admin: Reply to a feedback report.
     */
    @Transactional
    public FeedbackReportResponse addAdminReply(UUID reportId, UUID adminId, String adminName, String content) {
        FeedbackReport report = feedbackReportRepository.findById(reportId)
            .orElseThrow(() -> new ResourceNotFoundException(FEEDBACK_REPORT_NOT_FOUND));

        FeedbackMessage message = FeedbackMessage.builder()
            .senderId(adminId)
            .senderName(adminName)
            .senderRole("ADMIN")
            .content(content)
            .build();
        report.addMessage(message);

        if (report.getStatus() == FeedbackStatus.OPEN) {
            report.setStatus(FeedbackStatus.IN_PROGRESS);
        }

        report = feedbackReportRepository.save(report);
        log.info("Admin replied to feedback report {}", reportId);

        return toResponse(report);
    }

    // Defines to response so related behavior stays grouped in one place.
    private FeedbackReportResponse toResponse(FeedbackReport report) {
        User user = report.getUser();
        List<FeedbackMessageResponse> messages = report.getMessages().stream()
            .map(m -> new FeedbackMessageResponse(
                m.getId(),
                m.getSenderId(),
                m.getSenderName(),
                m.getSenderRole(),
                m.getContent(),
                m.getSentAt()
            ))
            .toList();

        return new FeedbackReportResponse(
            report.getId(),
            user.getUserId(),
            user.getUsername(),
            user.getEmail(),
            user.getFullName(),
            report.getStatus(),
            report.getPageUrl(),
            messages,
            report.getCreatedAt(),
            report.getUpdatedAt()
        );
    }
}

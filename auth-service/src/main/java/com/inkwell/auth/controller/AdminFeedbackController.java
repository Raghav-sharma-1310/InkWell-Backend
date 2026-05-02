/*
 * Codex documentation pass: this source file contains HTTP controller endpoints for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.controller;

import com.inkwell.auth.dto.ApiResponse;
import com.inkwell.auth.dto.request.FeedbackMessageRequest;
import com.inkwell.auth.dto.request.FeedbackStatusUpdateRequest;
import com.inkwell.auth.dto.response.FeedbackReportResponse;
import com.inkwell.auth.service.FeedbackService;
import com.inkwell.auth.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/feedback")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
/* This class groups admin feedback controller behavior so the module keeps a clear responsibility. */
public class AdminFeedbackController {

    private final FeedbackService feedbackService;

    /**
     * Admin: List all feedback reports.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<FeedbackReportResponse>>> getAllReports() {
        return ResponseEntity.ok(ApiResponse.of("All feedback reports fetched", feedbackService.getAllReports()));
    }

    /**
     * Admin: Get a specific report.
     */
    @GetMapping("/{reportId}")
    public ResponseEntity<ApiResponse<FeedbackReportResponse>> getReport(@PathVariable UUID reportId) {
        return ResponseEntity.ok(ApiResponse.of("Feedback report fetched", feedbackService.getReportById(reportId)));
    }

    /**
     * Admin: Update report status.
     */
    @PutMapping("/{reportId}/status")
    public ResponseEntity<ApiResponse<FeedbackReportResponse>> updateStatus(
        @PathVariable UUID reportId,
        @Valid @RequestBody FeedbackStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.of("Status updated", feedbackService.updateStatus(reportId, request.status())));
    }

    /**
     * Admin: Reply to a feedback report.
     */
    @PostMapping("/{reportId}/reply")
    public ResponseEntity<ApiResponse<FeedbackReportResponse>> reply(
        @PathVariable UUID reportId,
        @Valid @RequestBody FeedbackMessageRequest request
    ) {
        var principal = SecurityUtils.currentPrincipal();
        return ResponseEntity.ok(ApiResponse.of("Reply sent",
            feedbackService.addAdminReply(reportId, principal.userUuid(), principal.username(), request.message())
        ));
    }
}

/*
 * Codex documentation pass: this source file contains HTTP controller endpoints for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.controller;

import com.inkwell.auth.dto.ApiResponse;
import com.inkwell.auth.dto.request.FeedbackMessageRequest;
import com.inkwell.auth.dto.response.FeedbackReportResponse;
import com.inkwell.auth.service.FeedbackService;
import com.inkwell.auth.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
/* This class groups feedback controller behavior so the module keeps a clear responsibility. */
public class FeedbackController {

    private final FeedbackService feedbackService;

    /**
     * Submit a feedback report or add a message to an existing open report.
     */
    @PostMapping("/report")
    public ResponseEntity<ApiResponse<FeedbackReportResponse>> submitFeedback(
        @Valid @RequestBody FeedbackMessageRequest request
    ) {
        FeedbackReportResponse response = feedbackService.submitFeedback(
            SecurityUtils.currentPrincipal().userUuid(),
            request
        );
        return ResponseEntity.ok(ApiResponse.of("Feedback submitted successfully", response));
    }

    /**
     * Get the current user's feedback reports.
     */
    @GetMapping("/my-reports")
    public ResponseEntity<ApiResponse<List<FeedbackReportResponse>>> getMyReports() {
        return ResponseEntity.ok(ApiResponse.of("Your feedback reports fetched",
            feedbackService.getUserReports(SecurityUtils.currentPrincipal().userUuid())
        ));
    }
}

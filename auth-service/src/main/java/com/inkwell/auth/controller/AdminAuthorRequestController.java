/*
 * Codex documentation pass: this source file contains HTTP controller endpoints for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.controller;

import com.inkwell.auth.dto.ApiResponse;
import com.inkwell.auth.dto.request.AdminRemarkRequest;
import com.inkwell.auth.dto.response.AuthorRequestResponse;
import com.inkwell.auth.service.AuthorRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/author-requests")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
/* This class groups admin author request controller behavior so the module keeps a clear responsibility. */
public class AdminAuthorRequestController {

    private final AuthorRequestService authorRequestService;

    /**
     * Admin: List all author requests.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<AuthorRequestResponse>>> getAllRequests() {
        return ResponseEntity.ok(ApiResponse.of("All author requests fetched", authorRequestService.getAllRequests()));
    }

    /**
     * Admin: Approve an author request.
     */
    @PutMapping("/{requestId}/approve")
    public ResponseEntity<ApiResponse<AuthorRequestResponse>> approve(
        @PathVariable UUID requestId,
        @Valid @RequestBody(required = false) AdminRemarkRequest request
    ) {
        String remarks = request != null ? request.remarks() : null;
        return ResponseEntity.ok(ApiResponse.of("Author request approved", authorRequestService.approveRequest(requestId, remarks)));
    }

    /**
     * Admin: Reject an author request.
     */
    @PutMapping("/{requestId}/reject")
    public ResponseEntity<ApiResponse<AuthorRequestResponse>> reject(
        @PathVariable UUID requestId,
        @Valid @RequestBody(required = false) AdminRemarkRequest request
    ) {
        String remarks = request != null ? request.remarks() : null;
        return ResponseEntity.ok(ApiResponse.of("Author request rejected", authorRequestService.rejectRequest(requestId, remarks)));
    }
}

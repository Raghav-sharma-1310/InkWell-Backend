/*
 * This source file contains HTTP controller endpoints for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.controller;

import com.inkwell.auth.dto.ApiResponse;
import com.inkwell.auth.dto.response.AuthorRequestResponse;
import com.inkwell.auth.service.AuthorRequestService;
import com.inkwell.auth.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/author-request")
@RequiredArgsConstructor
/* This class groups author request controller behavior so the module keeps a clear responsibility. */
public class AuthorRequestController {

    private final AuthorRequestService authorRequestService;

    /**
     * Reader submits a request to become an author.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<AuthorRequestResponse>> submitRequest() {
        AuthorRequestResponse response = authorRequestService.submitRequest(
            SecurityUtils.currentPrincipal().userUuid()
        );
        return ResponseEntity.ok(ApiResponse.of("Author request submitted successfully", response));
    }

    /**
     * Reader checks their current author request status.
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<AuthorRequestResponse>> getStatus() {
        AuthorRequestResponse response = authorRequestService.getRequestStatus(
            SecurityUtils.currentPrincipal().userUuid()
        );
        return ResponseEntity.ok(ApiResponse.of("Author request status fetched", response));
    }
}

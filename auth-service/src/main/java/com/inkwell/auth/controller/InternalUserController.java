/*
 * This source file contains HTTP controller endpoints for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.controller;

import com.inkwell.auth.dto.ApiResponse;
import com.inkwell.auth.dto.response.UserResponse;
import com.inkwell.auth.service.AuthService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/internal")
@RequiredArgsConstructor
/* This class groups internal user controller behavior so the module keeps a clear responsibility. */
public class InternalUserController {

    private final AuthService authService;

    @GetMapping("/users/{userId}")
    // Performs the get internal user workflow so callers do not duplicate this logic.
    public ApiResponse<UserResponse> getInternalUser(@PathVariable UUID userId) {
        return ApiResponse.of("User fetched", authService.getUserResponse(userId));
    }
}

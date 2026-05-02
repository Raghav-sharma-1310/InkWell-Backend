/*
 * This source file contains HTTP controller endpoints for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.controller;

import com.inkwell.auth.dto.ApiResponse;
import com.inkwell.auth.dto.request.RoleUpdateRequest;
import com.inkwell.auth.dto.response.UserResponse;
import com.inkwell.auth.service.AuthService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
/* This class groups admin user controller behavior so the module keeps a clear responsibility. */
public class AdminUserController {

    private final AuthService authService;

    @GetMapping
    // Performs the get all users workflow so callers do not duplicate this logic.
    public ApiResponse<List<UserResponse>> getAllUsers() {
        return ApiResponse.of("All users fetched", authService.searchUsers(""));
    }

    @PatchMapping("/{userId}/role")
    // Performs the update role workflow so callers do not duplicate this logic.
    public ApiResponse<UserResponse> updateRole(@PathVariable UUID userId, @Valid @RequestBody RoleUpdateRequest request) {
        return ApiResponse.of("Role updated", authService.updateRole(userId, request));
    }

    @PatchMapping("/{userId}/suspend")
    // Defines suspend so related behavior stays grouped in one place.
    public ApiResponse<UserResponse> suspend(@PathVariable UUID userId) {
        return ApiResponse.of("User suspended", authService.toggleUserActive(userId, false));
    }

    @PatchMapping("/{userId}/reactivate")
    // Defines reactivate so related behavior stays grouped in one place.
    public ApiResponse<UserResponse> reactivate(@PathVariable UUID userId) {
        return ApiResponse.of("User reactivated", authService.toggleUserActive(userId, true));
    }

    @DeleteMapping("/{userId}")
    // Performs the delete user workflow so callers do not duplicate this logic.
    public ApiResponse<Void> deleteUser(@PathVariable UUID userId) {
        authService.deleteUser(userId);
        return ApiResponse.of("User deleted", null);
    }
}

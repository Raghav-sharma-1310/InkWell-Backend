/*
 * This source file contains HTTP controller endpoints for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.controller;

import com.inkwell.auth.dto.ApiResponse;
import com.inkwell.auth.dto.response.UserResponse;
import com.inkwell.auth.security.GatewayUserPrincipal;
import com.inkwell.auth.service.AdminConsoleService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin Console Controller — exposes privileged endpoints that only the DEFAULT_ADMIN
 * (identified at the service layer by email) can invoke.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>PUT  /api/auth/admin/console/admins/{adminId}/remove-role — demote an admin to USER</li>
 *   <li>DELETE /api/auth/admin/console/admins/{adminId}           — delete an admin account</li>
 * </ul>
 *
 * <p>Access is first gated to ROLE_ADMIN by @PreAuthorize, then further restricted
 * inside AdminConsoleService to the default admin email only.
 * No existing controllers, services, or security config were modified.
 */
@RestController
@RequestMapping("/api/auth/admin/console")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
/* This class groups admin console controller behavior so the module keeps a clear responsibility. */
public class AdminConsoleController {

    private final AdminConsoleService adminConsoleService;

    /**
     * Removes the ADMIN role from the target admin, downgrading them to USER.
     * The account remains active after demotion.
     *
     * <p>Only the default admin may call this endpoint.
     *
     * @param principal the authenticated caller (injected from the security context)
     * @param adminId   UUID of the admin to demote
     * @return          ApiResponse containing the updated user record
     */
    @PutMapping("/admins/{adminId}/remove-role")
    // Performs the remove admin role workflow so callers do not duplicate this logic.
    public ApiResponse<UserResponse> removeAdminRole(
            @AuthenticationPrincipal GatewayUserPrincipal principal,
            @PathVariable UUID adminId) {

        UserResponse updated = adminConsoleService.removeAdminRole(principal, adminId);
        return ApiResponse.of("Admin role removed successfully", updated);
    }

    /**
     * Permanently deletes the specified admin account.
     * Only the default admin may call this endpoint.
     *
     * @param principal the authenticated caller (injected from the security context)
     * @param adminId   UUID of the admin account to delete
     * @return          ApiResponse with a confirmation message and no body
     */
    @DeleteMapping("/admins/{adminId}")
    // Performs the delete admin workflow so callers do not duplicate this logic.
    public ApiResponse<Void> deleteAdmin(
            @AuthenticationPrincipal GatewayUserPrincipal principal,
            @PathVariable UUID adminId) {

        adminConsoleService.deleteAdmin(principal, adminId);
        return ApiResponse.of("Admin deleted successfully", null);
    }
}

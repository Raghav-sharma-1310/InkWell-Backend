/*
 * This source file contains business workflow and validation logic for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.service;

import com.inkwell.auth.dto.response.UserResponse;
import com.inkwell.auth.entity.User;
import com.inkwell.auth.enumtype.Role;
import com.inkwell.auth.exception.BadRequestException;
import com.inkwell.auth.exception.ResourceNotFoundException;
import com.inkwell.auth.mapper.UserMapper;
import com.inkwell.auth.repository.EmailVerificationTokenRepository;
import com.inkwell.auth.repository.UserRepository;
import com.inkwell.auth.security.GatewayUserPrincipal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin Console Service — handles privileged operations that only the DEFAULT_ADMIN
 * (the seeded admin@inkwell.dev account) is allowed to perform.
 *
 * <p>Rules enforced here:
 * <ul>
 *   <li>Only the default admin can remove another admin's role (demote to USER).</li>
 *   <li>Only the default admin can delete another admin account.</li>
 *   <li>The default admin cannot act on himself.</li>
 *   <li>Regular admins cannot reach these methods at all (blocked by @PreAuthorize on the controller).</li>
 * </ul>
 *
 * <p>No existing service or repository was modified to add this feature.
 */
@Slf4j
@Service
@RequiredArgsConstructor
/* This class groups admin console service behavior so the module keeps a clear responsibility. */
public class AdminConsoleService {

    // The email of the default (seeded) admin — must match AdminSeeder.ADMIN_EMAIL
    @Value("${app.admin.default-email:admin@inkwell.dev}")
    private String defaultAdminEmail;

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final RefreshTokenService refreshTokenService;
    private final AuditLogService auditLogService;
    private final EmailVerificationTokenRepository verificationTokenRepository;

    /**
     * Removes the ADMIN role from the specified admin account and downgrades the user to USER role.
     * The target user account remains active after demotion.
     *
     * @param caller    the authenticated principal making the request (must be the default admin)
     * @param adminId   UUID of the admin whose role should be removed
     * @return          updated UserResponse with role set to USER
     */
    @Transactional
    // Performs the remove admin role workflow so callers do not duplicate this logic.
    public UserResponse removeAdminRole(GatewayUserPrincipal caller, UUID adminId) {

        // Guard 1 — only the default admin may call this operation
        assertCallerIsDefaultAdmin(caller);

        // Guard 2 — default admin cannot demote himself
        assertNotSelf(caller, adminId, "You cannot perform this action on yourself");

        // Resolve the target user — throws 404 if not found
        User target = findAdminById(adminId);

        // Guard 3 — the default admin's own account must never be demoted
        assertNotDefaultAdmin(target);

        // Downgrade role from ADMIN → READER; account stays active
        target.setRole(Role.READER);
        userRepository.save(target);

        log.info("[AdminConsole] Default admin demoted user {} from ADMIN to READER", target.getEmail());
        auditLogService.logAction(
            null,
            caller.email(),
            "ADMIN_ROLE_REMOVED",
            "USER",
            adminId.toString(),
            "Admin role removed from " + target.getEmail() + " — role changed to READER"
        );

        return userMapper.toResponse(target);
    }

    /**
     * Deletes the specified admin account entirely.
     * Revokes all tokens and removes verification tokens before deletion.
     * No other users or services are affected.
     *
     * @param caller    the authenticated principal making the request (must be the default admin)
     * @param adminId   UUID of the admin account to delete
     */
    @Transactional
    // Performs the delete admin workflow so callers do not duplicate this logic.
    public void deleteAdmin(GatewayUserPrincipal caller, UUID adminId) {

        // Guard 1 — only the default admin may call this operation
        assertCallerIsDefaultAdmin(caller);

        // Guard 2 — default admin cannot delete himself
        assertNotSelf(caller, adminId, "You cannot perform this action on yourself");

        // Resolve the target user — throws 404 if not found
        User target = findAdminById(adminId);

        // Guard 3 — the default admin's own account must never be deleted
        assertNotDefaultAdmin(target);

        // Revoke active tokens to immediately invalidate sessions
        refreshTokenService.revokeAll(target);

        // Remove email verification tokens referencing this user to satisfy FK constraints
        verificationTokenRepository.deleteAll(verificationTokenRepository.findAllByUser(target));

        // Delete the admin user account
        userRepository.delete(target);

        log.info("[AdminConsole] Default admin deleted admin account {}", target.getEmail());
        auditLogService.logAction(
            null,
            caller.email(),
            "ADMIN_DELETED",
            "USER",
            adminId.toString(),
            "Admin account deleted: " + target.getEmail()
        );
    }

    // ─── private helpers ──────────────────────────────────────────────────────

    /**
     * Ensures the caller is the seeded default admin.
     * Throws BadRequestException with a clear message if not.
     */
    private void assertCallerIsDefaultAdmin(GatewayUserPrincipal caller) {
        if (!defaultAdminEmail.equalsIgnoreCase(caller.email())) {
            throw new BadRequestException("Only default admin can perform this action");
        }
    }

    /**
     * Ensures the caller is not targeting their own account.
     */
    private void assertNotSelf(GatewayUserPrincipal caller, UUID targetId, String message) {
        if (caller.userUuid().equals(targetId)) {
            throw new BadRequestException(message);
        }
    }

    /**
     * Ensures the target user is not the default admin account itself.
     * This protects against edge-cases where the caller UUID resolves differently.
     */
    private void assertNotDefaultAdmin(User target) {
        if (defaultAdminEmail.equalsIgnoreCase(target.getEmail())) {
            throw new BadRequestException("Default admin cannot be modified");
        }
    }

    /**
     * Looks up a user by ID and verifies they currently hold the ADMIN role.
     * Throws ResourceNotFoundException if the user does not exist.
     * Throws BadRequestException if the user exists but is not an admin.
     */
    private User findAdminById(UUID adminId) {
        User user = userRepository.findById(adminId)
            .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        if (user.getRole() != Role.ADMIN) {
            throw new BadRequestException("Admin not found");
        }

        return user;
    }
}

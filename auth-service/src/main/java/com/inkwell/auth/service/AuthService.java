/*
 * This source file contains business workflow and validation logic for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.service;

import com.inkwell.auth.dto.request.ChangePasswordRequest;
import com.inkwell.auth.dto.request.LoginRequest;
import com.inkwell.auth.dto.request.RegisterRequest;
import com.inkwell.auth.dto.request.RoleUpdateRequest;
import com.inkwell.auth.dto.request.UpdateProfileRequest;
import com.inkwell.auth.dto.response.AuthResponse;
import com.inkwell.auth.dto.response.UserResponse;
import com.inkwell.auth.entity.EmailVerificationToken;
import com.inkwell.auth.entity.RefreshToken;
import com.inkwell.auth.entity.User;
import com.inkwell.auth.enumtype.AuthProvider;
import com.inkwell.auth.enumtype.Role;
import com.inkwell.auth.exception.BadRequestException;
import com.inkwell.auth.exception.ResourceNotFoundException;
import com.inkwell.auth.exception.UnauthorizedException;
import com.inkwell.auth.mapper.UserMapper;
import com.inkwell.auth.repository.EmailVerificationTokenRepository;
import com.inkwell.auth.repository.UserRepository;
import com.inkwell.auth.security.GatewayUserPrincipal;
import com.inkwell.auth.security.JwtService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
/* This class groups auth service behavior so the module keeps a clear responsibility. */
public class AuthService {

    private static final String SYSTEM_ADMIN_ACTOR = "System/Admin";

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final EmailService emailService;
    private final LoginRateLimiter loginRateLimiter;
    private final AuditLogService auditLogService;
    private final EmailVerificationTokenRepository verificationTokenRepository;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Transactional
    // Performs the register workflow so callers do not duplicate this logic.
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        String normalizedUsername = normalizeUsername(request.username());

        validateUnique(normalizedEmail, normalizedUsername);

        Role requestedRole = request.role() == null ? Role.READER : request.role();
        if (request.role() == Role.ADMIN) {
            throw new BadRequestException("Admin accounts must be provisioned by an existing admin");
        }

        User user = userRepository.save(
            User.builder()
                .username(normalizedUsername)
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(requireText(request.fullName(), "Full name"))
                .role(requestedRole)
                .provider(AuthProvider.LOCAL)
                .active(true)
                .build()
        );

        emailService.sendWelcomeEmail(user.getEmail(), user.getFullName(), user.getRole().name());

        return buildAuthResponse(user);
    }



    @Transactional
    // Performs the login workflow so callers do not duplicate this logic.
    public AuthResponse login(LoginRequest request) {
        String identifier = requireText(request.email(), "Email or username");
        String normalizedIdentifier = identifier.trim().toLowerCase(Locale.ROOT);

        loginRateLimiter.checkAndIncrement(normalizedIdentifier);

        User user = userRepository.findByEmailIgnoreCase(identifier.trim())
            .or(() -> userRepository.findByUsernameIgnoreCase(identifier.trim()))
            .orElseThrow(() -> new UnauthorizedException("Invalid email/username or password"));

        if (!user.isActive()) {
            throw new UnauthorizedException("Account is deactivated");
        }

        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email/username or password");
        }

        // Reset using both possible identifiers to avoid stale rate limit entries
        loginRateLimiter.resetOnSuccess(user.getEmail());
        loginRateLimiter.resetOnSuccess(user.getUsername());

        emailService.sendLoginNotificationEmail(user.getEmail(), user.getFullName(), "Email / Password");

        return buildAuthResponse(user);
    }

    @Transactional
    // Performs the refresh workflow so callers do not duplicate this logic.
    public AuthResponse refresh(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenService.verify(refreshTokenValue);
        refreshTokenService.revoke(refreshTokenValue);
        RefreshToken newToken = refreshTokenService.createForUser(refreshToken.getUser());

        return new AuthResponse(
            jwtService.generateAccessToken(refreshToken.getUser()),
            newToken.getToken(),
            userMapper.toResponse(refreshToken.getUser())
        );
    }

    @Transactional
    // Defines reset password so related behavior stays grouped in one place.
    public void resetPassword(String email, String newPassword) {
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(email))
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setPasswordHash(passwordEncoder.encode(requireText(newPassword, "New password")));
        userRepository.save(user);
        refreshTokenService.revokeAll(user);
    }

    @Transactional
    // Defines logout so related behavior stays grouped in one place.
    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    @Transactional(readOnly = true)
    // Performs the get current user workflow so callers do not duplicate this logic.
    public UserResponse getCurrentUser(GatewayUserPrincipal principal) {
        return userMapper.toResponse(getUser(principal.userUuid()));
    }

    @Transactional
    // Performs the update profile workflow so callers do not duplicate this logic.
    public UserResponse updateProfile(GatewayUserPrincipal principal, UpdateProfileRequest request) {
        User user = getUser(principal.userUuid());

        if (request.fullName() != null && !request.fullName().isBlank()) {
            user.setFullName(request.fullName().trim());
        }
        if (request.username() != null && !request.username().isBlank()) {
            String newUsername = request.username().trim();
            if (!newUsername.equalsIgnoreCase(user.getUsername())) {
                if (userRepository.existsByUsernameIgnoreCase(newUsername)) {
                    throw new BadRequestException("Username is already taken");
                }
                user.setUsername(newUsername);
            }
        }
        user.setBio(request.bio());
        user.setAvatarUrl(request.avatarUrl());
        user.setPhoneNumber(request.phoneNumber());

        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    // Defines change password so related behavior stays grouped in one place.
    public void changePassword(GatewayUserPrincipal principal, ChangePasswordRequest request) {
        User user = getUser(principal.userUuid());

        if (user.getProvider() != AuthProvider.LOCAL) {
            throw new BadRequestException("OAuth users cannot change password here");
        }

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(requireText(request.newPassword(), "New password")));
        userRepository.save(user);
        refreshTokenService.revokeAll(user);
    }

    @Transactional
    // Defines deactivate own account so related behavior stays grouped in one place.
    public void deactivateOwnAccount(GatewayUserPrincipal principal) {
        User user = getUser(principal.userUuid());
        user.setActive(false);
        userRepository.save(user);
        refreshTokenService.revokeAll(user);
    }

    @Transactional(readOnly = true)
    // Defines search users so related behavior stays grouped in one place.
    public List<UserResponse> searchUsers(String query) {
        String safeQuery = query == null ? "" : query.trim();
        return userRepository.findByFullNameContainingIgnoreCaseOrUsernameContainingIgnoreCase(safeQuery, safeQuery)
            .stream()
            .map(userMapper::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    // Performs the get authors workflow so callers do not duplicate this logic.
    public List<UserResponse> getAuthors() {
        return userRepository.findByRole(Role.AUTHOR)
            .stream()
            .map(userMapper::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    // Performs the get user response workflow so callers do not duplicate this logic.
    public UserResponse getUserResponse(UUID userId) {
        return userMapper.toResponse(getUser(userId));
    }

    @Transactional
    // Performs the update role workflow so callers do not duplicate this logic.
    public UserResponse updateRole(UUID userId, RoleUpdateRequest request) {
        User user = getUser(userId);
        String oldRole = user.getRole().name();

        user.setRole(request.role());

        log.info("Updated role for user {} to {}", user.getEmail(), request.role());

        auditLogService.logAction(
            null,
            SYSTEM_ADMIN_ACTOR,
            "USER_ROLE_CHANGED",
            "USER",
            userId.toString(),
            "Role changed from " + oldRole + " to " + request.role().name()
        );

        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    // Defines toggle user active so related behavior stays grouped in one place.
    public UserResponse toggleUserActive(UUID userId, boolean active) {
        User user = getUser(userId);
        boolean oldActive = user.isActive();

        user.setActive(active);
        if (!active) {
            refreshTokenService.revokeAll(user);
        }

        auditLogService.logAction(
            null,
            SYSTEM_ADMIN_ACTOR,
            "USER_ACTIVE_TOGGLED",
            "USER",
            userId.toString(),
            "Active status changed from " + oldActive + " to " + active
        );

        return userMapper.toResponse(userRepository.save(user));
    }

    // Performs the get user workflow so callers do not duplicate this logic.
    public User getUser(UUID userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Transactional
    // Performs the delete user workflow so callers do not duplicate this logic.
    public void deleteUser(UUID userId) {
        User user = getUser(userId);
        if (user.getRole() == Role.ADMIN) {
            throw new BadRequestException("Cannot delete an admin account");
        }
        refreshTokenService.revokeAll(user);
        verificationTokenRepository.deleteAll(verificationTokenRepository.findAllByUser(user));
        userRepository.delete(user);
        log.info("Deleted user {}", user.getEmail());
        auditLogService.logAction(null, SYSTEM_ADMIN_ACTOR, "USER_DELETED", "USER", userId.toString(), "Deleted user " + user.getEmail());
    }

    // Provides build auth response wiring so the framework can apply the expected runtime behavior.
    private AuthResponse buildAuthResponse(User user) {
        return new AuthResponse(
            jwtService.generateAccessToken(user),
            refreshTokenService.createForUser(user).getToken(),
            userMapper.toResponse(user)
        );
    }

    // Defines validate unique so related behavior stays grouped in one place.
    private void validateUnique(String email, String username) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BadRequestException("Email is already registered");
        }
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new BadRequestException("Username is already taken");
        }
    }



    // Defines normalize email so related behavior stays grouped in one place.
    private String normalizeEmail(String email) {
        return requireText(email, "Email").toLowerCase(Locale.ROOT);
    }

    // Defines normalize username so related behavior stays grouped in one place.
    private String normalizeUsername(String username) {
        return requireText(username, "Username");
    }

    // Defines require text so related behavior stays grouped in one place.
    private String requireText(String value, String fieldName) {
        if (value == null || value.trim().isBlank()) {
            throw new BadRequestException(fieldName + " is required");
        }
        return value.trim();
    }
}
/*
 * This source file contains business workflow and validation logic for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.service;

import com.inkwell.auth.dto.request.LoginRequest;
import com.inkwell.auth.dto.request.RegisterRequest;
import com.inkwell.auth.dto.request.ChangePasswordRequest;
import com.inkwell.auth.dto.request.UpdateProfileRequest;
import com.inkwell.auth.dto.response.UserResponse;
import com.inkwell.auth.dto.response.AuthResponse;
import com.inkwell.auth.entity.RefreshToken;
import com.inkwell.auth.entity.User;
import com.inkwell.auth.enumtype.AuthProvider;
import com.inkwell.auth.enumtype.Role;
import com.inkwell.auth.exception.BadRequestException;
import com.inkwell.auth.exception.ResourceNotFoundException;
import com.inkwell.auth.exception.UnauthorizedException;
import com.inkwell.auth.mapper.UserMapper;
import com.inkwell.auth.repository.AuthorRequestRepository;
import com.inkwell.auth.repository.EmailVerificationTokenRepository;
import com.inkwell.auth.repository.FeedbackReportRepository;
import com.inkwell.auth.repository.PaymentOrderRepository;
import com.inkwell.auth.repository.UserRepository;
import com.inkwell.auth.security.GatewayUserPrincipal;
import com.inkwell.auth.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
/* This class groups auth service test behavior so the module keeps a clear responsibility. */
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private EmailService emailService;
    @Mock private LoginRateLimiter loginRateLimiter;
    @Mock private AuditLogService auditLogService;
    @Mock private EmailVerificationTokenRepository verificationTokenRepository;
    @Mock private AuthorRequestRepository authorRequestRepository;
    @Mock private FeedbackReportRepository feedbackReportRepository;
    @Mock private PaymentOrderRepository paymentOrderRepository;

    @InjectMocks private AuthService authService;

    private User testUser;
    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .userId(UUID.randomUUID())
                .username("testuser")
                .email("test@inkwell.com")
                .passwordHash("hashedPassword")
                .fullName("Test User")
                .role(Role.READER)
                .provider(AuthProvider.LOCAL)
                .active(true)
                .build();

        registerRequest = new RegisterRequest(
                "testuser",
                "test@inkwell.com",
                "Password@123",
                "Test User",
                Role.READER
        );
    }

    @Test
    @DisplayName("Should register new user successfully")
    void registerSuccess() {
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(userRepository.existsByUsernameIgnoreCase(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("access-token");
        when(refreshTokenService.createForUser(any(User.class))).thenReturn(new RefreshToken(null, "refresh-token", null, null, null));

        AuthResponse response = authService.register(registerRequest);

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("access-token");
        verify(emailService).sendWelcomeEmail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should throw exception when registering with existing email")
    void registerDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Email is already registered");
    }

    @Test
    @DisplayName("Should login successfully")
    void loginSuccess() {
        LoginRequest loginRequest = new LoginRequest("test@inkwell.com", "Password@123");
        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("access-token");
        when(refreshTokenService.createForUser(any(User.class))).thenReturn(new RefreshToken(null, "refresh-token", null, null, null));

        AuthResponse response = authService.login(loginRequest);

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("access-token");
        verify(loginRateLimiter).checkAndIncrement(anyString());
        verify(loginRateLimiter, atLeastOnce()).resetOnSuccess(anyString());
    }

    @Test
    @DisplayName("Should throw exception on invalid password")
    void loginInvalidPassword() {
        LoginRequest loginRequest = new LoginRequest("test@inkwell.com", "wrong-password");
        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid email/username or password");
    }

    @Test
    @DisplayName("Should logout by revoking refresh token")
    void logoutSuccess() {
        authService.logout("refresh-token");
        verify(refreshTokenService).revoke("refresh-token");
    }

    @Test
    @DisplayName("Should update role successfully")
    void updateRoleSuccess() {
        when(userRepository.findById(any())).thenReturn(Optional.of(testUser));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        com.inkwell.auth.dto.request.RoleUpdateRequest roleRequest = new com.inkwell.auth.dto.request.RoleUpdateRequest(Role.AUTHOR);
        authService.updateRole(testUser.getUserId(), roleRequest);

        assertThat(testUser.getRole()).isEqualTo(Role.AUTHOR);
        verify(auditLogService).logAction(any(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should delete user successfully")
    void deleteUserSuccess() {
        when(userRepository.findById(any())).thenReturn(Optional.of(testUser));

        authService.deleteUser(testUser.getUserId());

        verify(refreshTokenService).revokeAll(testUser);
        verify(userRepository).delete(testUser);
        verify(auditLogService).logAction(any(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void registerDefaultsReaderRoleAndNormalizesEmail() {
        RegisterRequest request = new RegisterRequest(" newuser ", "NEW@INKWELL.COM ", "Password@123", " New User ", null);
        when(userRepository.existsByEmailIgnoreCase("new@inkwell.com")).thenReturn(false);
        when(userRepository.existsByUsernameIgnoreCase("newuser")).thenReturn(false);
        when(passwordEncoder.encode("Password@123")).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("access-token");
        when(refreshTokenService.createForUser(any(User.class))).thenReturn(new RefreshToken(null, "refresh-token", testUser, null, null));

        authService.register(request);

        verify(userRepository).save(argThat(user -> user.getRole() == Role.READER
            && user.getEmail().equals("new@inkwell.com")
            && user.getFullName().equals("New User")));
    }

    @Test
    void registerRejectsAdminRoleAndDuplicateUsername() {
        RegisterRequest adminRequest = new RegisterRequest("admin", "admin@inkwell.com", "Password@123", "Admin User", Role.ADMIN);
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(userRepository.existsByUsernameIgnoreCase("admin")).thenReturn(false);

        assertThatThrownBy(() -> authService.register(adminRequest))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("Admin accounts");

        RegisterRequest duplicateUsername = new RegisterRequest("taken", "new@inkwell.com", "Password@123", "New User", Role.READER);
        when(userRepository.existsByUsernameIgnoreCase("taken")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(duplicateUsername))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("Username is already taken");
    }

    @Test
    void loginSupportsUsernameFallbackAndRejectsInactiveUser() {
        LoginRequest byUsername = new LoginRequest("testuser", "Password@123");
        when(userRepository.findByEmailIgnoreCase("testuser")).thenReturn(Optional.empty());
        when(userRepository.findByUsernameIgnoreCase("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("Password@123", "hashedPassword")).thenReturn(true);
        when(jwtService.generateAccessToken(testUser)).thenReturn("access-token");
        when(refreshTokenService.createForUser(testUser)).thenReturn(new RefreshToken(null, "refresh-token", testUser, null, null));

        assertThat(authService.login(byUsername).accessToken()).isEqualTo("access-token");

        testUser.setActive(false);
        assertThatThrownBy(() -> authService.login(byUsername))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessageContaining("deactivated");
    }

    @Test
    void refreshRotatesRefreshToken() {
        RefreshToken oldToken = new RefreshToken(null, "old", testUser, null, null);
        RefreshToken newToken = new RefreshToken(null, "new", testUser, null, null);
        when(refreshTokenService.verify("old")).thenReturn(oldToken);
        when(refreshTokenService.createForUser(testUser)).thenReturn(newToken);
        when(jwtService.generateAccessToken(testUser)).thenReturn("new-access");

        AuthResponse response = authService.refresh("old");

        assertThat(response.accessToken()).isEqualTo("new-access");
        assertThat(response.refreshToken()).isEqualTo("new");
        verify(refreshTokenService).revoke("old");
    }

    @Test
    void resetPasswordUpdatesPasswordAndRevokesTokens() {
        when(userRepository.findByEmailIgnoreCase("test@inkwell.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("Password@456")).thenReturn("new-hash");

        authService.resetPassword(" TEST@INKWELL.COM ", " Password@456 ");

        assertThat(testUser.getPasswordHash()).isEqualTo("new-hash");
        verify(userRepository).save(testUser);
        verify(refreshTokenService).revokeAll(testUser);
    }

    @Test
    void resetPasswordRejectsUnknownUser() {
        when(userRepository.findByEmailIgnoreCase("missing@inkwell.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.resetPassword("missing@inkwell.com", "Password@456"))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void profileReadAndUpdateUseCurrentPrincipal() {
        GatewayUserPrincipal principal = new GatewayUserPrincipal(testUser.getUserId().toString(), "testuser", testUser.getEmail(), "READER");
        UserResponse mapped = userResponse(testUser);
        when(userRepository.findById(testUser.getUserId())).thenReturn(Optional.of(testUser));
        when(userMapper.toResponse(testUser)).thenReturn(mapped);
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(userRepository.existsByUsernameIgnoreCase("updated")).thenReturn(false);

        assertThat(authService.getCurrentUser(principal)).isEqualTo(mapped);
        UserResponse updated = authService.updateProfile(principal,
            new com.inkwell.auth.dto.request.UpdateProfileRequest(" Updated User ", "updated", "bio", "+919999999999", "avatar.png"));

        assertThat(updated).isEqualTo(mapped);
        assertThat(testUser.getUsername()).isEqualTo("updated");
        assertThat(testUser.getFullName()).isEqualTo("Updated User");
    }

    @Test
    void updateProfileRejectsTakenUsername() {
        GatewayUserPrincipal principal = new GatewayUserPrincipal(testUser.getUserId().toString(), "testuser", testUser.getEmail(), "READER");
        when(userRepository.findById(testUser.getUserId())).thenReturn(Optional.of(testUser));
        when(userRepository.existsByUsernameIgnoreCase("taken")).thenReturn(true);

        UpdateProfileRequest request = new UpdateProfileRequest("Test User", "taken", null, null, null);

        assertThatThrownBy(() -> authService.updateProfile(principal, request))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("Username is already taken");
    }

    @Test
    void changePasswordCoversSuccessAndFailurePaths() {
        GatewayUserPrincipal principal = new GatewayUserPrincipal(testUser.getUserId().toString(), "testuser", testUser.getEmail(), "READER");
        when(userRepository.findById(testUser.getUserId())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("old", "hashedPassword")).thenReturn(true);
        when(passwordEncoder.encode("Password@456")).thenReturn("new-hash");

        ChangePasswordRequest successRequest = new ChangePasswordRequest("old", "Password@456");
        authService.changePassword(principal, successRequest);

        assertThat(testUser.getPasswordHash()).isEqualTo("new-hash");
        verify(refreshTokenService).revokeAll(testUser);

        when(passwordEncoder.matches("bad", "new-hash")).thenReturn(false);
        ChangePasswordRequest badCurrentPasswordRequest = new ChangePasswordRequest("bad", "Password@789");
        assertThatThrownBy(() -> authService.changePassword(principal, badCurrentPasswordRequest))
            .isInstanceOf(UnauthorizedException.class);

        testUser.setProvider(AuthProvider.GOOGLE);
        ChangePasswordRequest oauthPasswordRequest = new ChangePasswordRequest("old", "Password@456");
        assertThatThrownBy(() -> authService.changePassword(principal, oauthPasswordRequest))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("OAuth users");
    }

    @Test
    void deactivateSearchAuthorsGetAndToggleUsers() {
        GatewayUserPrincipal principal = new GatewayUserPrincipal(testUser.getUserId().toString(), "testuser", testUser.getEmail(), "READER");
        UserResponse mapped = userResponse(testUser);
        when(userRepository.findById(testUser.getUserId())).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(userMapper.toResponse(testUser)).thenReturn(mapped);

        authService.deactivateOwnAccount(principal);
        assertThat(testUser.isActive()).isFalse();

        when(userRepository.findByFullNameContainingIgnoreCaseOrUsernameContainingIgnoreCase("", "")).thenReturn(List.of(testUser));
        when(userRepository.findByRole(Role.AUTHOR)).thenReturn(List.of(testUser));

        assertThat(authService.searchUsers(null)).containsExactly(mapped);
        assertThat(authService.getAuthors()).containsExactly(mapped);
        assertThat(authService.getUserResponse(testUser.getUserId())).isEqualTo(mapped);
        assertThat(authService.toggleUserActive(testUser.getUserId(), true)).isEqualTo(mapped);
        assertThat(testUser.isActive()).isTrue();
        authService.toggleUserActive(testUser.getUserId(), false);
        verify(refreshTokenService, atLeast(2)).revokeAll(testUser);
    }

    @Test
    void deleteUserRejectsAdminsAndDeletesVerificationTokensForReaders() {
        when(userRepository.findById(testUser.getUserId())).thenReturn(Optional.of(testUser));
        when(verificationTokenRepository.findAllByUser(testUser)).thenReturn(List.of());

        authService.deleteUser(testUser.getUserId());

        verify(verificationTokenRepository).deleteAll(List.of());
        verify(userRepository).delete(testUser);

        testUser.setRole(Role.ADMIN);
        UUID userId = testUser.getUserId();
        assertThatThrownBy(() -> authService.deleteUser(userId))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("Cannot delete an admin");
    }

    // Defines user response so related behavior stays grouped in one place.
    private static UserResponse userResponse(User user) {
        return new UserResponse(user.getUserId(), user.getUsername(), user.getEmail(), user.getFullName(),
            user.getRole(), user.getBio(), user.getAvatarUrl(), user.getPhoneNumber(), user.getProvider(),
            user.isActive(), LocalDateTime.now(), null, null, null);
    }
}

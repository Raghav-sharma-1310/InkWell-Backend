/*
 * This source file contains HTTP controller endpoints for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.inkwell.auth.dto.request.ChangePasswordRequest;
import com.inkwell.auth.dto.request.ForgotPasswordRequest;
import com.inkwell.auth.dto.request.LoginRequest;
import com.inkwell.auth.dto.request.RefreshTokenRequest;
import com.inkwell.auth.dto.request.RegisterRequest;
import com.inkwell.auth.dto.request.ResetPasswordRequest;
import com.inkwell.auth.dto.request.RoleUpdateRequest;
import com.inkwell.auth.dto.request.UpdateProfileRequest;
import com.inkwell.auth.dto.request.VerifyOtpRequest;
import com.inkwell.auth.dto.response.AuthResponse;
import com.inkwell.auth.dto.response.UserResponse;
import com.inkwell.auth.enumtype.AuthProvider;
import com.inkwell.auth.enumtype.Role;
import com.inkwell.auth.security.GatewayUserPrincipal;
import com.inkwell.auth.service.AuthService;
import com.inkwell.auth.service.EmailService;
import com.inkwell.auth.service.OtpService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
/* This class groups auth controller direct test behavior so the module keeps a clear responsibility. */
class AuthControllerDirectTest {

    @Mock
    private AuthService authService;
    @Mock
    private OtpService otpService;
    @Mock
    private EmailService emailService;

    private AuthController controller;
    private GatewayUserPrincipal principal;

    @BeforeEach
    void setUp() {
        controller = new AuthController(authService, otpService, emailService);
        principal = new GatewayUserPrincipal(UUID.randomUUID().toString(), "reader", "reader@inkwell.com", "READER");
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authEndpointsReturnExpectedMessages() {
        RegisterRequest register = new RegisterRequest("reader", "reader@inkwell.com", "Password@123", "Reader", Role.READER);
        LoginRequest login = new LoginRequest("reader@inkwell.com", "Password@123");
        RefreshTokenRequest refresh = new RefreshTokenRequest("refresh-token");
        AuthResponse authResponse = new AuthResponse("access", "refresh", userResponse());
        when(authService.register(register)).thenReturn(authResponse);
        when(authService.login(login)).thenReturn(authResponse);
        when(authService.refresh("refresh-token")).thenReturn(authResponse);

        assertThat(controller.register(register).getBody().message()).isEqualTo("Registration successful");
        assertThat(controller.login(login).getBody().message()).isEqualTo("Login successful");
        assertThat(controller.refresh(refresh).getBody().message()).isEqualTo("Token refreshed");
        assertThat(controller.logout(refresh).getBody().message()).isEqualTo("Logout successful");

        verify(authService).logout("refresh-token");
    }

    @Test
    void currentUserEndpointsUseSecurityPrincipal() {
        UserResponse response = userResponse();
        UpdateProfileRequest update = new UpdateProfileRequest("Reader", "reader", "bio", "+919999999999", "avatar");
        ChangePasswordRequest password = new ChangePasswordRequest("Oldpass1", "Newpass1");
        when(authService.getCurrentUser(principal)).thenReturn(response);
        when(authService.updateProfile(principal, update)).thenReturn(response);

        assertThat(controller.me().getBody().data()).isEqualTo(response);
        assertThat(controller.updateProfile(update).getBody().message()).isEqualTo("Profile updated");
        assertThat(controller.changePassword(password).getBody().message()).isEqualTo("Password changed successfully");
        assertThat(controller.deactivateSelf().getBody().message()).isEqualTo("Account deactivated");

        verify(authService).changePassword(principal, password);
        verify(authService).deactivateOwnAccount(principal);
    }

    @Test
    void forgotPasswordOtpAndResetFlowDelegatesInOrder() {
        ForgotPasswordRequest forgot = new ForgotPasswordRequest("reader@inkwell.com");
        VerifyOtpRequest verifyOtp = new VerifyOtpRequest("reader@inkwell.com", "123456");
        ResetPasswordRequest reset = new ResetPasswordRequest("reader@inkwell.com", "Newpass1");

        assertThat(controller.forgotPassword(forgot).getBody().message()).isEqualTo("OTP sent to your email");
        assertThat(controller.verifyOtp(verifyOtp).getBody().message()).isEqualTo("OTP verified successfully");
        assertThat(controller.resetPassword(reset).getBody().message()).isEqualTo("Password reset successfully");

        verify(otpService).generateAndSend("reader@inkwell.com");
        verify(otpService).verify("reader@inkwell.com", "123456");
        verify(otpService).ensureVerified("reader@inkwell.com");
        verify(authService).resetPassword("reader@inkwell.com", "Newpass1");
        verify(otpService).cleanup("reader@inkwell.com");
    }

    @Test
    void adminUserControllerDelegatesToAuthService() {
        AdminUserController adminController = new AdminUserController(authService);
        UserResponse response = userResponse();
        UUID userId = response.userId();
        RoleUpdateRequest roleUpdate = new RoleUpdateRequest(Role.AUTHOR);
        when(authService.searchUsers("")).thenReturn(List.of(response));
        when(authService.updateRole(userId, roleUpdate)).thenReturn(response);
        when(authService.toggleUserActive(userId, false)).thenReturn(response);
        when(authService.toggleUserActive(userId, true)).thenReturn(response);

        assertThat(adminController.getAllUsers().data()).containsExactly(response);
        assertThat(adminController.updateRole(userId, roleUpdate).message()).isEqualTo("Role updated");
        assertThat(adminController.suspend(userId).message()).isEqualTo("User suspended");
        assertThat(adminController.reactivate(userId).message()).isEqualTo("User reactivated");
        assertThat(adminController.deleteUser(userId).message()).isEqualTo("User deleted");
        verify(authService).deleteUser(userId);
    }

    // Defines user response so related behavior stays grouped in one place.
    private static UserResponse userResponse() {
        return new UserResponse(UUID.randomUUID(), "reader", "reader@inkwell.com", "Reader", Role.READER,
            "bio", "avatar", "+919999999999", AuthProvider.LOCAL, true, LocalDateTime.now(), null, null, null);
    }
}

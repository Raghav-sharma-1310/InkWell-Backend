/*
 * Codex documentation pass: this source file contains HTTP controller endpoints for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.controller;

import com.inkwell.auth.dto.ApiResponse;
import com.inkwell.auth.dto.request.ChangePasswordRequest;
import com.inkwell.auth.dto.request.ForgotPasswordRequest;
import com.inkwell.auth.dto.request.LoginRequest;
import com.inkwell.auth.dto.request.RefreshTokenRequest;
import com.inkwell.auth.dto.request.RegisterRequest;
import com.inkwell.auth.dto.request.ResetPasswordRequest;
import com.inkwell.auth.dto.request.UpdateProfileRequest;
import com.inkwell.auth.dto.request.VerifyOtpRequest;
import com.inkwell.auth.dto.response.AuthResponse;
import com.inkwell.auth.dto.response.UserResponse;
import com.inkwell.auth.service.AuthService;
import com.inkwell.auth.service.EmailService;
import com.inkwell.auth.service.OtpService;
import com.inkwell.auth.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
/* This class groups auth controller behavior so the module keeps a clear responsibility. */
public class AuthController {

    private final AuthService authService;
    private final OtpService otpService;
    private final EmailService emailService;

    @PostMapping("/register")
    // Performs the register workflow so callers do not duplicate this logic.
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(ApiResponse.of("Registration successful", response));
    }



    @PostMapping("/login")
    // Performs the login workflow so callers do not duplicate this logic.
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.of("Login successful", authService.login(request)));
    }

    @PostMapping("/refresh")
    // Performs the refresh workflow so callers do not duplicate this logic.
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(ApiResponse.of("Token refreshed", authService.refresh(request.refreshToken())));
    }

    @PostMapping("/logout")
    // Defines logout so related behavior stays grouped in one place.
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.of("Logout successful", null));
    }

    @GetMapping("/me")
    // Defines me so related behavior stays grouped in one place.
    public ResponseEntity<ApiResponse<UserResponse>> me() {
        return ResponseEntity.ok(ApiResponse.of("Current user fetched", authService.getCurrentUser(SecurityUtils.currentPrincipal())));
    }

    @PatchMapping("/me")
    // Performs the update profile workflow so callers do not duplicate this logic.
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.of("Profile updated", authService.updateProfile(SecurityUtils.currentPrincipal(), request)));
    }

    @PatchMapping("/me/password")
    // Defines change password so related behavior stays grouped in one place.
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(SecurityUtils.currentPrincipal(), request);
        return ResponseEntity.ok(ApiResponse.of("Password changed successfully", null));
    }

    @PatchMapping("/me/deactivate")
    // Defines deactivate self so related behavior stays grouped in one place.
    public ResponseEntity<ApiResponse<Void>> deactivateSelf() {
        authService.deactivateOwnAccount(SecurityUtils.currentPrincipal());
        return ResponseEntity.ok(ApiResponse.of("Account deactivated", null));
    }

    // --- Forgot Password Flow ---

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        otpService.generateAndSend(request.email());
        return ResponseEntity.ok(ApiResponse.of("OTP sent to your email", null));
    }

    @PostMapping("/verify-otp")
    // Performs the verify otp workflow so callers do not duplicate this logic.
    public ResponseEntity<ApiResponse<Void>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        otpService.verify(request.email(), request.otp());
        return ResponseEntity.ok(ApiResponse.of("OTP verified successfully", null));
    }

    @PostMapping("/reset-password")
    // Defines reset password so related behavior stays grouped in one place.
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        otpService.ensureVerified(request.email());
        authService.resetPassword(request.email(), request.newPassword());
        otpService.cleanup(request.email());
        return ResponseEntity.ok(ApiResponse.of("Password reset successfully", null));
    }
}

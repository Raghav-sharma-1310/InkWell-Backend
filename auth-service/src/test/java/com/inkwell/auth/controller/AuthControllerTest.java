/*
 * This source file contains HTTP controller endpoints for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inkwell.auth.dto.request.LoginRequest;
import com.inkwell.auth.dto.request.RegisterRequest;
import com.inkwell.auth.dto.response.AuthResponse;
import com.inkwell.auth.enumtype.Role;
import com.inkwell.auth.service.AuthService;
import com.inkwell.auth.service.EmailService;
import com.inkwell.auth.service.OtpService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;



import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for controller unit tests
@ActiveProfiles("test")
/* This class groups auth controller test behavior so the module keeps a clear responsibility. */
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AuthService authService;
    @MockBean private OtpService otpService;
    @MockBean private EmailService emailService;

    @Test
    @DisplayName("POST /api/auth/register - Success")
    void registerSuccess() throws Exception {
        RegisterRequest request = new RegisterRequest("testuser", "test@inkwell.com", "Password@123", "Test User", Role.READER);
        AuthResponse response = new AuthResponse("access", "refresh", null);

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Registration successful"))
                .andExpect(jsonPath("$.data.accessToken").value("access"));
    }

    @Test
    @DisplayName("POST /api/auth/login - Success")
    void loginSuccess() throws Exception {
        LoginRequest request = new LoginRequest("test@inkwell.com", "Password@123");
        AuthResponse response = new AuthResponse("access", "refresh", null);

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login successful"));
    }

    @Test
    @DisplayName("POST /api/auth/register - Validation Failure")
    void registerValidationFailure() throws Exception {
        RegisterRequest request = new RegisterRequest("", "invalid-email", "short", "", null);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}

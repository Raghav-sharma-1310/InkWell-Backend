/*
 * This source file contains HTTP controller endpoints for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.newsletter.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inkwell.newsletter.dto.request.SubscribeRequest;
import com.inkwell.newsletter.dto.response.SubscriberResponse;
import com.inkwell.newsletter.enumtype.SubscriberStatus;
import com.inkwell.newsletter.service.NewsletterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NewsletterController.class)
@Import({com.inkwell.newsletter.security.SecurityConfig.class, com.inkwell.newsletter.exception.GlobalExceptionHandler.class})
/* This class groups newsletter controller test behavior so the module keeps a clear responsibility. */
class NewsletterControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private NewsletterService newsletterService;
    @MockBean private com.inkwell.newsletter.security.GatewayAuthenticationFilter gatewayAuthenticationFilter;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> {
            jakarta.servlet.FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(gatewayAuthenticationFilter).doFilter(any(), any(), any());
    }

    @Test
    @DisplayName("Should successfully subscribe")
    void subscribeSuccess() throws Exception {
        SubscribeRequest request = new SubscribeRequest("test@inkwell.com", "Test User", "test");
        SubscriberResponse response = new SubscriberResponse(UUID.randomUUID(), "test@inkwell.com", null, "Test User", SubscriberStatus.PENDING, null, null, "test");
        
        when(newsletterService.subscribe(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/newsletter/public/subscribe")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Subscription requested"));
    }

    @Test
    @DisplayName("Should return 400 when subscribe with invalid data")
    void subscribeInvalid() throws Exception {
        SubscribeRequest request = new SubscribeRequest("invalid-email", "", "");
        
        mockMvc.perform(post("/api/newsletter/public/subscribe")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should confirm subscription")
    void confirmSuccess() throws Exception {
        SubscriberResponse response = new SubscriberResponse(UUID.randomUUID(), "test@inkwell.com", null, "Test User", SubscriberStatus.ACTIVE, null, null, "test");
        when(newsletterService.confirm("valid-token")).thenReturn(response);

        mockMvc.perform(get("/api/newsletter/public/confirm")
                .param("token", "valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Subscription verified"));
    }
}

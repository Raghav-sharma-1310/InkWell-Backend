/*
 * This source file contains authentication and authorization support for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.notification.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
/* This class groups gateway authentication filter test behavior so the module keeps a clear responsibility. */
class GatewayAuthenticationFilterTest {

    @InjectMocks
    private GatewayAuthenticationFilter filter;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should authenticate when headers are present")
    void testValidHeaders() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "123e4567-e89b-12d3-a456-426614174000");
        request.addHeader("X-Username", "testuser");
        request.addHeader("X-User-Email", "test@inkwell.com");
        request.addHeader("X-User-Role", "READER");

        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        GatewayUserPrincipal principal = (GatewayUserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertThat(principal.userId()).isEqualTo("123e4567-e89b-12d3-a456-426614174000");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should not authenticate when headers are missing")
    void testMissingHeaders() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}

/*
 * This source file contains authentication and authorization support for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.media.security;

import jakarta.servlet.FilterChain;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/* This class groups gateway authentication filter test behavior so the module keeps a clear responsibility. */
class GatewayAuthenticationFilterTest {

    private final GatewayAuthenticationFilter filter = new GatewayAuthenticationFilter();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesGatewayHeaders() throws Exception {
        UUID userId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", userId.toString());
        request.addHeader("X-Username", "author");
        request.addHeader("X-User-Email", "author@test.com");
        request.addHeader("X-User-Role", "AUTHOR");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        GatewayUserPrincipal principal = (GatewayUserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertThat(principal.userUuid()).isEqualTo(userId);
        assertThat(principal.isAdmin()).isFalse();
        verify(chain).doFilter(request, response);
    }

    @Test
    void skipsMissingHeaders() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }
}

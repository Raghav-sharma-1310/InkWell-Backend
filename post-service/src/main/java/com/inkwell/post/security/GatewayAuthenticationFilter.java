/*
 * This source file contains authentication and authorization support for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.post.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
/* This class groups gateway authentication filter behavior so the module keeps a clear responsibility. */
public class GatewayAuthenticationFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        String userId = request.getHeader("X-User-Id");
        String username = request.getHeader("X-Username");
        String email = request.getHeader("X-User-Email");
        String role = request.getHeader("X-User-Role");
        String subscriptionTier = request.getHeader("X-User-Subscription-Tier");
        String subscriptionStatus = request.getHeader("X-User-Subscription-Status");
        if (StringUtils.hasText(userId) && StringUtils.hasText(username) && StringUtils.hasText(role)
            && SecurityContextHolder.getContext().getAuthentication() == null) {
            GatewayUserPrincipal principal = new GatewayUserPrincipal(userId, username, email, role, subscriptionTier, subscriptionStatus);
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
            );
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        filterChain.doFilter(request, response);
    }
}

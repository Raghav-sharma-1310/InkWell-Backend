/*
 * Codex documentation pass: this source file contains authentication and authorization support for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.gateway.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
/* This class groups jwt authentication filter behavior so the module keeps a clear responsibility. */
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final List<String> PUBLIC_PATHS = List.of(
        "/",
        "/api/auth/login",
        "/api/auth/register",
        "/api/auth/refresh",
        "/api/auth/oauth2/**",
        "/api/auth/forgot-password",
        "/api/auth/verify-otp",
        "/api/auth/reset-password",
        "/oauth2/**",
        "/login/**",
        "/api/posts/public/**",
        "/api/posts/explore/**",
        "/api/posts/authors/*/followers/count",
        "/api/auth/public/**",
        "/api/comments/public/**",
        "/api/categories/public/**",
        "/api/newsletter/public/**",
        "/api/newsletter/verify",
        "/api/media/public/**",
        "/actuator/**",
        // Swagger / OpenAPI paths
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/v3/api-docs/**",
        "/v3/api-docs",
        "/webjars/**",
        "/auth-service/v3/api-docs",
        "/post-service/v3/api-docs",
        "/comment-service/v3/api-docs",
        "/category-service/v3/api-docs",
        "/media-service/v3/api-docs",
        "/newsletter-service/v3/api-docs",
        "/notification-service/v3/api-docs",
        "/payment-service/v3/api-docs"
    );

    private static final List<String> ADMIN_PATHS = List.of(
        "/api/auth/admin/**",
        "/api/posts/admin/**",
        "/api/comments/admin/**",
        "/api/categories/admin/**",
        "/api/media/admin/**",
        "/api/newsletter/admin/**",
        "/api/notifications/admin/**"
    );

    private static final List<String> PREMIUM_PATHS = List.of(
        "/api/posts/reader/bookmarks",
        "/api/posts/reader/*/bookmark",
        "/api/reading-history",
        "/api/reading-history/**"
    );

    private static final List<String> AUTHOR_PATHS = List.of(
        "/api/posts/author/**",
        "/api/comments/author/**",
        "/api/media/author/**"
    );

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    // Provides filter wiring so the framework can apply the expected runtime behavior.
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        HttpMethod method = exchange.getRequest().getMethod();

        if (HttpMethod.OPTIONS.equals(method)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        boolean isPublic = isPublicPath(path);

        if (!hasBearerToken(authHeader)) {
            return handleMissingToken(exchange, chain, isPublic);
        }

        try {
            Claims claims = jwtService.parseToken(authHeader.substring("Bearer ".length()));
            Mono<Void> authorizationFailure = authorizationFailure(exchange, path, claims);
            if (authorizationFailure != null) {
                return authorizationFailure;
            }

            return chain.filter(exchange.mutate().request(withGatewayHeaders(exchange, claims)).build());
        } catch (JwtException ex) {
            log.debug("JWT validation failed for path {}: {}", path, ex.getMessage());
            if (isPublic) {
                return chain.filter(exchange);
            }
            return writeUnauthorized(exchange, "Token expired or invalid");
        }
    }

    @Override
    // Performs the get order workflow so callers do not duplicate this logic.
    public int getOrder() {
        return -1;
    }

    // Defines is public path so related behavior stays grouped in one place.
    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    // Defines requires admin so related behavior stays grouped in one place.
    private boolean requiresAdmin(String path) {
        return ADMIN_PATHS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    // Provides requires author wiring so the framework can apply the expected runtime behavior.
    private boolean requiresAuthor(String path) {
        return AUTHOR_PATHS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    // Defines requires premium so related behavior stays grouped in one place.
    private boolean requiresPremium(String path) {
        return PREMIUM_PATHS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    // Defines has bearer token so related behavior stays grouped in one place.
    private boolean hasBearerToken(String authHeader) {
        return authHeader != null && authHeader.startsWith("Bearer ");
    }

    // Defines handle missing token so related behavior stays grouped in one place.
    private Mono<Void> handleMissingToken(ServerWebExchange exchange, GatewayFilterChain chain, boolean isPublic) {
        if (isPublic) {
            return chain.filter(exchange);
        }
        return writeUnauthorized(exchange, "Missing or invalid authorization header");
    }

    // Provides authorization failure wiring so the framework can apply the expected runtime behavior.
    private Mono<Void> authorizationFailure(ServerWebExchange exchange, String path, Claims claims) {
        String role = claims.get("role", String.class);
        if (requiresAdmin(path) && !"ADMIN".equals(role)) {
            return writeForbidden(exchange, "Access denied - Admin only");
        }
        if (requiresAuthor(path) && !List.of("AUTHOR", "ADMIN").contains(role)) {
            return writeForbidden(exchange, "Author or admin access required");
        }
        if (requiresPremium(path) && !hasActiveProSubscription(claims)) {
            return writeForbidden(exchange, "Premium subscription required");
        }
        return null;
    }

    // Defines has active pro subscription so related behavior stays grouped in one place.
    private boolean hasActiveProSubscription(Claims claims) {
        String subTier = claims.get("subscriptionTier", String.class);
        String subStatus = claims.get("subscriptionStatus", String.class);
        return "PRO".equals(subTier) && "ACTIVE".equals(subStatus);
    }

    // Defines with gateway headers so related behavior stays grouped in one place.
    private ServerHttpRequest withGatewayHeaders(ServerWebExchange exchange, Claims claims) {
        return exchange.getRequest()
            .mutate()
            .header("X-User-Id", claims.get("userId", String.class))
            .header("X-Username", claims.get("username", String.class))
            .header("X-User-Role", claims.get("role", String.class))
            .header("X-User-Email", claims.get("email", String.class))
            .header("X-User-Subscription-Tier", claims.get("subscriptionTier", String.class))
            .header("X-User-Subscription-Status", claims.get("subscriptionStatus", String.class))
            .build();
    }

    // Provides write unauthorized wiring so the framework can apply the expected runtime behavior.
    private Mono<Void> writeUnauthorized(ServerWebExchange exchange, String message) {
        return writeError(exchange, HttpStatus.UNAUTHORIZED, message);
    }

    // Defines write forbidden so related behavior stays grouped in one place.
    private Mono<Void> writeForbidden(ServerWebExchange exchange, String message) {
        return writeError(exchange, HttpStatus.FORBIDDEN, message);
    }

    // Defines write error so related behavior stays grouped in one place.
    private Mono<Void> writeError(ServerWebExchange exchange, HttpStatus status, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body;
        try {
            body = objectMapper.writeValueAsString(Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message,
                "path", exchange.getRequest().getPath().value()
            )).getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException ex) {
            body = ("{\"message\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
        }
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
    }
}

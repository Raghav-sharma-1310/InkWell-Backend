/*
 * This source file contains authentication and authorization support for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.gateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;

import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
/* This class groups jwt authentication filter test behavior so the module keeps a clear responsibility. */
class JwtAuthenticationFilterTest {

    private static final String SECRET = "dGhpc2lzYXZlcnlsb25nc2VjcmV0a2V5Zm9ydGVzdGluZ3B1cnBvc2VzMTIzNDU2Nzg5MA==";
    private SecretKey signingKey;
    private JwtAuthenticationFilter filter;
    private ObjectMapper objectMapper;
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
        JwtService jwtService = new JwtService(SECRET);
        objectMapper = new ObjectMapper();
        filter = new JwtAuthenticationFilter(jwtService, objectMapper);
        chain = mock(GatewayFilterChain.class);
        lenient().when(chain.filter(any())).thenReturn(Mono.empty());
    }

    // Defines generate token so related behavior stays grouped in one place.
    private String generateToken(String role, String subTier, String subStatus) {
        return Jwts.builder()
                .claim("userId", UUID.randomUUID().toString())
                .claim("username", "testuser")
                .claim("email", "test@inkwell.com")
                .claim("role", role)
                .claim("subscriptionTier", subTier)
                .claim("subscriptionStatus", subStatus)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(signingKey)
                .compact();
    }

    @Test
    @DisplayName("Should allow public path without token")
    void publicPathAllowed() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/posts/public/feed").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        verify(chain).filter(exchange);
    }

    @Test
    @DisplayName("Should allow OPTIONS requests")
    void optionsAllowed() {
        MockServerHttpRequest request = MockServerHttpRequest.options("/api/posts/author/my").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        verify(chain).filter(exchange);
    }

    @Test
    @DisplayName("Should reject request without auth header")
    void missingAuthHeader() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/posts/author/my").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Should reject request with invalid Bearer token")
    void invalidToken() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/posts/author/my")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.token.here")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Should allow authenticated user with valid token")
    void validToken() {
        String token = generateToken("READER", "FREE", null);
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/notifications/mine")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        verify(chain).filter(any());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
            "/api/auth/admin/users",
            "/api/posts/author/my",
            "/api/posts/reader/bookmarks"
    })
    @DisplayName("Should deny non-authorized access to restricted paths")
    void restrictedPathsDenied(String path) {
        String token = generateToken("READER", "FREE", null);
        MockServerHttpRequest request = MockServerHttpRequest.get(path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Should allow admin access to admin paths")
    void adminPathAllowed() {
        String token = generateToken("ADMIN", "PRO", "ACTIVE");
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/auth/admin/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        verify(chain).filter(any());
    }

    @Test
    @DisplayName("Should allow premium user to premium paths")
    void premiumPathAllowed() {
        String token = generateToken("READER", "PRO", "ACTIVE");
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/posts/reader/bookmarks")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        verify(chain).filter(any());
    }

    @Test
    @DisplayName("Should return order -1")
    void getOrder() {
        assertThat(filter.getOrder()).isEqualTo(-1);
    }
}

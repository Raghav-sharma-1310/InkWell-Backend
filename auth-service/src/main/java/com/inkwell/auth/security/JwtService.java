/*
 * This source file contains authentication and authorization support for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.security;

import com.inkwell.auth.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
/* This class groups jwt service behavior so the module keeps a clear responsibility. */
public class JwtService {

    private final Key signingKey;
    private final long accessMinutes;

    public JwtService(
        @Value("${security.jwt.secret}") String secret,
        @Value("${security.jwt.access-minutes:60}") long accessMinutes
    ) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.accessMinutes = accessMinutes;
    }

    // Defines generate access token so related behavior stays grouped in one place.
    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getUserId().toString());
        claims.put("username", user.getUsername());
        claims.put("email", user.getEmail());
        claims.put("role", user.getRole().name());
        claims.put("subscriptionTier", user.getSubscriptionTier() != null ? user.getSubscriptionTier().name() : "FREE");
        claims.put("subscriptionStatus", user.getSubscriptionStatus() != null ? user.getSubscriptionStatus().name() : null);
        return Jwts.builder()
            .claims(claims)
            .subject(user.getEmail())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(accessMinutes, ChronoUnit.MINUTES)))
            .signWith((javax.crypto.SecretKey) signingKey)
            .compact();
    }

    // Defines parse token so related behavior stays grouped in one place.
    public Claims parseToken(String token) {
        return Jwts.parser()
            .verifyWith((javax.crypto.SecretKey) signingKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}

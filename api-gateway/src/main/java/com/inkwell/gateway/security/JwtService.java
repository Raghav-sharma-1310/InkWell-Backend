/*
 * This source file contains authentication and authorization support for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
/* This class groups jwt service behavior so the module keeps a clear responsibility. */
public class JwtService {

    private final Key signingKey;

    // Performs the jwt service workflow so callers do not duplicate this logic.
    public JwtService(@Value("${security.jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
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

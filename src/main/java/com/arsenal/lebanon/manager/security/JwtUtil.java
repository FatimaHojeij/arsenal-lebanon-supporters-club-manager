package com.arsenal.lebanon.manager.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    // NOTE: This key is regenerated on every restart (tokens invalidated on reboot).
    // Will be replaced with a stable env-var secret when hosting.
    private final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    private final long EXPIRATION_TIME = 86400000; // 24 hours

    // Role claim key used in token payload
    public static final String CLAIM_ROLE = "role";

    public String generateToken(String email, String role) {
        return Jwts.builder()
                .setSubject(email)
                .claim(CLAIM_ROLE, role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(key)
                .compact();
    }

    public String extractEmail(String token) {
        return getClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return (String) getClaims(token).get(CLAIM_ROLE);
    }

    public boolean isTokenValid(String token, String email) {
        return extractEmail(token).equals(email) && !getClaims(token).getExpiration().before(new Date());
    }

    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}

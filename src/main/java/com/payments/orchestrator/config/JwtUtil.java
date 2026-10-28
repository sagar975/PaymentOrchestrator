package com.payments.orchestrator.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT utility — generates and validates JWT tokens.
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration.ms}")
    private long expirationMs;

    /**
     * Generates JWT token for authenticated merchant.
     *
     * @param merchantId merchant identifier
     * @param email      merchant email
     * @param role       MERCHANT or ADMIN
     * @return signed JWT token
     */
    public String generateToken(String merchantId, String email, String role) {

        Map<String, Object> claims = new HashMap<>();
        claims.put("merchantId", merchantId);
        claims.put("email", email);
        claims.put("role", role);

        return Jwts.builder().setClaims(claims).setSubject(email).setIssuedAt(new Date()).setExpiration(new Date(System.currentTimeMillis() + expirationMs)).signWith(getSigningKey(), SignatureAlgorithm.HS256).compact();
    }

    /**
     * Extracts merchantId from JWT token.
     */
    public String extractMerchantId(String token) {
        return extractClaims(token).get("merchantId", String.class);
    }

    /**
     * Extracts email from JWT token.
     */
    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }

    /**
     * Extracts role from JWT token.
     */
    public String extractRole(String token) {
        return extractClaims(token).get("role", String.class);
    }

    /**
     * Validates JWT token.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    private Claims extractClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token).getBody();
    }

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }
}
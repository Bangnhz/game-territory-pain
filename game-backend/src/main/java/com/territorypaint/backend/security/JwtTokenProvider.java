package com.territorypaint.backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * JWT token provider using JJWT library.
 * Requirements: 1.9, 1.10
 * 
 * Generates, validates, and extracts user information from JWT tokens.
 * Uses HS256 signing algorithm with 2-hour expiration.
 */
@Component
public class JwtTokenProvider {
    
    private final SecretKey secretKey;
    private final long expirationMs;
    
    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms:7200000}") long expirationMs) {
        // Use HS256 signing with a secret key
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }
    
    /**
     * Generate JWT token for a user.
     * Requirements: 1.9, 1.10
     * 
     * @param userId the user ID to include as subject
     * @param username the username to include as claim
     * @return JWT token string
     */
    public String generateToken(Long userId, String username) {
        Instant now = Instant.now();
        Instant expiration = now.plusMillis(expirationMs);
        
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }
    
    /**
     * Validate JWT token.
     * Requirements: 1.9, 2.3
     * 
     * @param token JWT token to validate
     * @return true if token is valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            // Invalid JWT signature or structure
            return false;
        } catch (ExpiredJwtException e) {
            // Token expired
            return false;
        } catch (UnsupportedJwtException e) {
            // Unsupported JWT token
            return false;
        } catch (IllegalArgumentException e) {
            // Empty or invalid JWT claims
            return false;
        } catch (JwtException e) {
            // Any other JWT-related exception
            return false;
        }
    }
    
    /**
     * Extract user ID from JWT token subject.
     * Requirements: 1.9, 2.3
     * 
     * @param token JWT token
     * @return user ID from token subject
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        
        return Long.parseLong(claims.getSubject());
    }
    
    /**
     * Extract username from JWT token.
     * 
     * @param token JWT token
     * @return username from token claims
     */
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        
        return claims.get("username", String.class);
    }
    
    /**
     * Extract expiration time from JWT token.
     * 
     * @param token JWT token
     * @return expiration instant
     */
    public Instant getExpirationFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        
        return claims.getExpiration().toInstant();
    }
}

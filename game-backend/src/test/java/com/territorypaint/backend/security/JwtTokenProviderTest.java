package com.territorypaint.backend.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JwtTokenProvider.
 * Tests token generation, validation, and extraction methods.
 */
class JwtTokenProviderTest {
    
    private JwtTokenProvider tokenProvider;
    private static final String TEST_SECRET = "test-secret-key-for-jwt-signing-must-be-at-least-256-bits";
    private static final long EXPIRATION_MS = 7200000; // 2 hours
    
    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(TEST_SECRET, EXPIRATION_MS);
    }
    
    @Test
    void generateToken_shouldCreateValidToken() {
        // Given
        Long userId = 123L;
        String username = "testuser";
        
        // When
        String token = tokenProvider.generateToken(userId, username);
        
        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.split("\\.").length == 3); // JWT has 3 parts
    }
    
    @Test
    void validateToken_shouldReturnTrueForValidToken() {
        // Given
        String token = tokenProvider.generateToken(123L, "testuser");
        
        // When
        boolean isValid = tokenProvider.validateToken(token);
        
        // Then
        assertTrue(isValid);
    }
    
    @Test
    void validateToken_shouldReturnFalseForInvalidToken() {
        // Given
        String invalidToken = "invalid.jwt.token";
        
        // When
        boolean isValid = tokenProvider.validateToken(invalidToken);
        
        // Then
        assertFalse(isValid);
    }
    
    @Test
    void validateToken_shouldReturnFalseForEmptyToken() {
        // Given
        String emptyToken = "";
        
        // When
        boolean isValid = tokenProvider.validateToken(emptyToken);
        
        // Then
        assertFalse(isValid);
    }
    
    @Test
    void getUserIdFromToken_shouldExtractCorrectUserId() {
        // Given
        Long expectedUserId = 456L;
        String token = tokenProvider.generateToken(expectedUserId, "testuser");
        
        // When
        Long actualUserId = tokenProvider.getUserIdFromToken(token);
        
        // Then
        assertEquals(expectedUserId, actualUserId);
    }
    
    @Test
    void getUsernameFromToken_shouldExtractCorrectUsername() {
        // Given
        String expectedUsername = "testuser123";
        String token = tokenProvider.generateToken(123L, expectedUsername);
        
        // When
        String actualUsername = tokenProvider.getUsernameFromToken(token);
        
        // Then
        assertEquals(expectedUsername, actualUsername);
    }
    
    @Test
    void getExpirationFromToken_shouldReturnFutureTime() {
        // Given
        String token = tokenProvider.generateToken(123L, "testuser");
        Instant now = Instant.now();
        
        // When
        Instant expiration = tokenProvider.getExpirationFromToken(token);
        
        // Then
        assertTrue(expiration.isAfter(now));
    }
    
    @Test
    void getExpirationFromToken_shouldMatchConfiguredExpiration() {
        // Given
        String token = tokenProvider.generateToken(123L, "testuser");
        Instant now = Instant.now();
        
        // When
        Instant expiration = tokenProvider.getExpirationFromToken(token);
        
        // Then
        long actualDuration = expiration.toEpochMilli() - now.toEpochMilli();
        // Allow 1 second tolerance for execution time
        assertTrue(Math.abs(actualDuration - EXPIRATION_MS) < 1000);
    }
    
    @Test
    void validateToken_shouldReturnFalseForTamperedToken() {
        // Given
        String validToken = tokenProvider.generateToken(123L, "testuser");
        // Tamper with the signature part (last part after last dot)
        int lastDot = validToken.lastIndexOf('.');
        String tamperedToken = validToken.substring(0, lastDot + 1) + "tampered_signature";
        
        // When
        boolean isValid = tokenProvider.validateToken(tamperedToken);
        
        // Then
        assertFalse(isValid);
    }
    
    @Test
    void generateToken_shouldCreateDifferentTokensForDifferentUsers() {
        // Given
        Long userId1 = 100L;
        Long userId2 = 200L;
        String username = "testuser";
        
        // When
        String token1 = tokenProvider.generateToken(userId1, username);
        String token2 = tokenProvider.generateToken(userId2, username);
        
        // Then
        assertNotEquals(token1, token2);
    }
    
    @Test
    void generateToken_shouldCreateDifferentTokensForSameUserCalledTwice() {
        // Given
        Long userId = 123L;
        String username = "testuser";
        
        // When
        String token1 = tokenProvider.generateToken(userId, username);
        // Sleep to ensure different issuedAt time (1 second resolution)
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        String token2 = tokenProvider.generateToken(userId, username);
        
        // Then
        assertNotEquals(token1, token2);
    }
    
    @Test
    void getUserIdFromToken_shouldThrowExceptionForInvalidToken() {
        // Given
        String invalidToken = "invalid.jwt.token";
        
        // When/Then
        assertThrows(Exception.class, () -> tokenProvider.getUserIdFromToken(invalidToken));
    }
    
    @Test
    void getUsernameFromToken_shouldThrowExceptionForInvalidToken() {
        // Given
        String invalidToken = "invalid.jwt.token";
        
        // When/Then
        assertThrows(Exception.class, () -> tokenProvider.getUsernameFromToken(invalidToken));
    }
}

package com.territorypaint.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SecurityConfig.
 * Tests BCryptPasswordEncoder configuration.
 */
class SecurityConfigTest {
    
    @Test
    void passwordEncoder_shouldReturnBCryptPasswordEncoder() {
        // Given
        SecurityConfig securityConfig = new SecurityConfig(null, null);
        
        // When
        PasswordEncoder encoder = securityConfig.passwordEncoder();
        
        // Then
        assertNotNull(encoder);
        assertTrue(encoder instanceof BCryptPasswordEncoder);
    }
    
    @Test
    void passwordEncoder_shouldEncodePassword() {
        // Given
        SecurityConfig securityConfig = new SecurityConfig(null, null);
        PasswordEncoder encoder = securityConfig.passwordEncoder();
        String rawPassword = "testPassword123";
        
        // When
        String encodedPassword = encoder.encode(rawPassword);
        
        // Then
        assertNotNull(encodedPassword);
        assertNotEquals(rawPassword, encodedPassword);
        assertTrue(encodedPassword.startsWith("$2a$") || encodedPassword.startsWith("$2b$"));
    }
    
    @Test
    void passwordEncoder_shouldValidateCorrectPassword() {
        // Given
        SecurityConfig securityConfig = new SecurityConfig(null, null);
        PasswordEncoder encoder = securityConfig.passwordEncoder();
        String rawPassword = "testPassword123";
        String encodedPassword = encoder.encode(rawPassword);
        
        // When
        boolean matches = encoder.matches(rawPassword, encodedPassword);
        
        // Then
        assertTrue(matches);
    }
    
    @Test
    void passwordEncoder_shouldRejectIncorrectPassword() {
        // Given
        SecurityConfig securityConfig = new SecurityConfig(null, null);
        PasswordEncoder encoder = securityConfig.passwordEncoder();
        String rawPassword = "testPassword123";
        String wrongPassword = "wrongPassword456";
        String encodedPassword = encoder.encode(rawPassword);
        
        // When
        boolean matches = encoder.matches(wrongPassword, encodedPassword);
        
        // Then
        assertFalse(matches);
    }
    
    @Test
    void passwordEncoder_shouldGenerateDifferentHashesForSamePassword() {
        // Given
        SecurityConfig securityConfig = new SecurityConfig(null, null);
        PasswordEncoder encoder = securityConfig.passwordEncoder();
        String password = "testPassword123";
        
        // When
        String hash1 = encoder.encode(password);
        String hash2 = encoder.encode(password);
        
        // Then
        assertNotEquals(hash1, hash2);
        assertTrue(encoder.matches(password, hash1));
        assertTrue(encoder.matches(password, hash2));
    }
}

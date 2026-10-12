package com.territorypaint.backend.security;

/**
 * JWT user principal for authenticated requests.
 * Requirements: 2.3
 * 
 * This class represents the authenticated user extracted from JWT token.
 */
public class JwtUser {
    
    private final Long userId;
    private final String username;
    
    public JwtUser(Long userId, String username) {
        this.userId = userId;
        this.username = username;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public String getUsername() {
        return username;
    }
}

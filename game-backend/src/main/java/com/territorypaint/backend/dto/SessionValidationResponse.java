package com.territorypaint.backend.dto;

import java.time.Instant;

/**
 * Session validation response to Game Server.
 * Requirements: 2.3, 2.4
 * 
 * Returns validated user information if token is valid, or valid=false if invalid/expired.
 */
public class SessionValidationResponse {
    
    private boolean valid;
    private Long userId;
    private String username;
    private Long totalScore;
    private Instant tokenExpiration;
    
    public SessionValidationResponse() {
    }
    
    /**
     * Create response for invalid token.
     */
    public static SessionValidationResponse invalid() {
        SessionValidationResponse response = new SessionValidationResponse();
        response.valid = false;
        return response;
    }
    
    /**
     * Create response for valid token.
     */
    public static SessionValidationResponse valid(Long userId, String username, Long totalScore, Instant tokenExpiration) {
        SessionValidationResponse response = new SessionValidationResponse();
        response.valid = true;
        response.userId = userId;
        response.username = username;
        response.totalScore = totalScore;
        response.tokenExpiration = tokenExpiration;
        return response;
    }
    
    public boolean isValid() {
        return valid;
    }
    
    public void setValid(boolean valid) {
        this.valid = valid;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public Long getTotalScore() {
        return totalScore;
    }
    
    public void setTotalScore(Long totalScore) {
        this.totalScore = totalScore;
    }
    
    public Instant getTokenExpiration() {
        return tokenExpiration;
    }
    
    public void setTokenExpiration(Instant tokenExpiration) {
        this.tokenExpiration = tokenExpiration;
    }
}

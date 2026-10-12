package com.territorypaint.backend.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Session validation request from Game Server.
 * Requirements: 2.3, 2.4
 * 
 * Contains the JWT token received from a player during Game Server login.
 */
public class SessionValidationRequest {
    
    @NotBlank(message = "Token is required")
    private String token;
    
    public SessionValidationRequest() {
    }
    
    public SessionValidationRequest(String token) {
        this.token = token;
    }
    
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
}

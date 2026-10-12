package com.territorypaint.backend.dto;

import java.time.Instant;

/**
 * Registration response DTO.
 * Requirements: 1.7, 1.9, 1.10
 */
public class RegisterResponse {
    
    private String token;
    private Instant expiresAt;
    private ProfileInfo profile;
    
    public RegisterResponse() {
    }
    
    public RegisterResponse(String token, Instant expiresAt, ProfileInfo profile) {
        this.token = token;
        this.expiresAt = expiresAt;
        this.profile = profile;
    }
    
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public Instant getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    public ProfileInfo getProfile() {
        return profile;
    }
    
    public void setProfile(ProfileInfo profile) {
        this.profile = profile;
    }
}

package com.territorypaint.backend.dto;

/**
 * Profile response DTO.
 * Requirements: 10.1, 10.2
 */
public class ProfileResponse {
    
    private ProfileInfo profile;
    
    public ProfileResponse() {
    }
    
    public ProfileResponse(ProfileInfo profile) {
        this.profile = profile;
    }
    
    public ProfileInfo getProfile() {
        return profile;
    }
    
    public void setProfile(ProfileInfo profile) {
        this.profile = profile;
    }
}

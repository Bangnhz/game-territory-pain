package com.territorypaint.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Registration request DTO.
 * Requirements: 1.1, 1.2, 1.3, 1.4
 */
public class RegisterRequest {
    
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 30, message = "Username must be 3-30 characters")
    @Pattern(regexp = "^[a-z0-9_]+$", message = "Username must contain only lowercase letters, digits, and underscores")
    private String username;
    
    @NotBlank(message = "Password is required")
    @Size(min = 3, max = 72, message = "Password must be 3-72 characters")
    private String password;
    
    @NotBlank(message = "Full name is required")
    @Size(min = 1, max = 100, message = "Full name must be 1-100 characters")
    private String fullName;
    
    public RegisterRequest() {
    }
    
    public RegisterRequest(String username, String password, String fullName) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getFullName() {
        return fullName;
    }
    
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
}

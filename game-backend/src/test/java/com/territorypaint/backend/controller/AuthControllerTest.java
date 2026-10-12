package com.territorypaint.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.territorypaint.backend.dto.LoginRequest;
import com.territorypaint.backend.dto.LoginResponse;
import com.territorypaint.backend.dto.ProfileInfo;
import com.territorypaint.backend.dto.RegisterRequest;
import com.territorypaint.backend.dto.RegisterResponse;
import com.territorypaint.backend.exception.InvalidCredentialsException;
import com.territorypaint.backend.exception.UsernameAlreadyExistsException;
import com.territorypaint.backend.security.JwtTokenProvider;
import com.territorypaint.backend.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AuthController.
 * Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for controller tests
class AuthControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private AuthService authService;
    
    @MockBean
    private JwtTokenProvider jwtTokenProvider;
    
    @Test
    void testRegister_Success() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest("testuser", "password123", "John Doe");
        
        ProfileInfo profile = new ProfileInfo(1L, "testuser", "John Doe", 0L, 0, 0, 0, 0, 0.00);
        RegisterResponse response = new RegisterResponse(
                "mock_jwt_token",
                Instant.now().plusSeconds(7200),
                profile
        );
        
        when(authService.register("testuser", "password123", "John Doe"))
                .thenReturn(response);
        
        // Act & Assert (Requirement 1.1, 1.7)
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", is("mock_jwt_token")))
                .andExpect(jsonPath("$.expiresAt", notNullValue()))
                .andExpect(jsonPath("$.profile.userId", is(1)))
                .andExpect(jsonPath("$.profile.username", is("testuser")))
                .andExpect(jsonPath("$.profile.fullName", is("John Doe")))
                .andExpect(jsonPath("$.profile.totalScore", is(0)))
                .andExpect(jsonPath("$.profile.matchesPlayed", is(0)));
    }
    
    @Test
    void testRegister_UsernameAlreadyExists() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest("existinguser", "password123", "John Doe");
        
        when(authService.register("existinguser", "password123", "John Doe"))
                .thenThrow(new UsernameAlreadyExistsException("existinguser"));
        
        // Act & Assert (Requirement 1.6)
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("USERNAME_ALREADY_EXISTS")))
                .andExpect(jsonPath("$.message", containsString("existinguser")));
    }
    
    @Test
    void testRegister_ValidationError_UsernameEmpty() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest("", "password123", "John Doe");
        
        // Act & Assert (Requirement 1.2)
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.fields.username", notNullValue()));
    }
    
    @Test
    void testRegister_ValidationError_UsernameTooShort() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest("ab", "password123", "John Doe");
        
        // Act & Assert (Requirement 1.2)
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.fields.username", containsString("3-30")));
    }
    
    @Test
    void testRegister_ValidationError_UsernameTooLong() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest("a".repeat(31), "password123", "John Doe");
        
        // Act & Assert (Requirement 1.2)
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.fields.username", containsString("3-30")));
    }
    
    @Test
    void testRegister_ValidationError_UsernameInvalidCharacters() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest("Test User!", "password123", "John Doe");
        
        // Act & Assert (Requirement 1.2)
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.fields.username", containsString("lowercase letters, digits, and underscores")));
    }
    
    @Test
    void testRegister_ValidationError_PasswordTooShort() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest("testuser", "ab", "John Doe");
        
        // Act & Assert (Requirement 1.3)
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.fields.password", containsString("3-72")));
    }
    
    @Test
    void testRegister_ValidationError_PasswordTooLong() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest("testuser", "a".repeat(73), "John Doe");
        
        // Act & Assert (Requirement 1.3)
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.fields.password", containsString("3-72")));
    }
    
    @Test
    void testRegister_ValidationError_FullNameEmpty() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest("testuser", "password123", "");
        
        // Act & Assert (Requirement 1.3)
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.fields.fullName", notNullValue()));
    }
    
    @Test
    void testRegister_ValidationError_FullNameTooLong() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest("testuser", "password123", "a".repeat(101));
        
        // Act & Assert (Requirement 1.3)
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.fields.fullName", containsString("1-100")));
    }
    
    @Test
    void testRegister_PasswordByteLengthExceeded() throws Exception {
        // Arrange
        // 30 UTF-8 characters of '€' (3 bytes each = 90 bytes > 72 bytes limit, but character count 30 is <= 72)
        RegisterRequest request = new RegisterRequest("testuser", "€".repeat(30), "John Doe");
        
        when(authService.register(anyString(), anyString(), anyString()))
                .thenThrow(new IllegalArgumentException("Password exceeds 72 UTF-8 bytes"));
        
        // Act & Assert (Requirement 1.3)
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("INVALID_INPUT")))
                .andExpect(jsonPath("$.message", containsString("72 UTF-8 bytes")));
    }
    
    @Test
    void testLogin_Success() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest("testuser", "password123");
        
        ProfileInfo profile = new ProfileInfo(1L, "testuser", "John Doe", 150L, 20, 10, 7, 3, 50.0);
        LoginResponse response = new LoginResponse(
                "jwt_token",
                Instant.now().plusSeconds(7200),
                profile
        );
        
        when(authService.login("testuser", "password123")).thenReturn(response);
        
        // Act & Assert (Requirement 1.7)
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", is("jwt_token")))
                .andExpect(jsonPath("$.expiresAt", notNullValue()))
                .andExpect(jsonPath("$.profile.userId", is(1)))
                .andExpect(jsonPath("$.profile.username", is("testuser")))
                .andExpect(jsonPath("$.profile.totalScore", is(150)))
                .andExpect(jsonPath("$.profile.matchesPlayed", is(20)))
                .andExpect(jsonPath("$.profile.winRate", is(50.0)));
    }
    
    @Test
    void testLogin_InvalidCredentials() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest("testuser", "wrongpassword");
        
        when(authService.login("testuser", "wrongpassword"))
                .thenThrow(new InvalidCredentialsException());
        
        // Act & Assert (Requirement 1.8)
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("INVALID_CREDENTIALS")))
                .andExpect(jsonPath("$.message", is("Invalid credentials")));
    }
    
    @Test
    void testLogin_ValidationError_UsernameEmpty() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest("", "password123");
        
        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.fields.username", notNullValue()));
    }
    
    @Test
    void testLogin_ValidationError_PasswordEmpty() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest("testuser", "");
        
        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.fields.password", notNullValue()));
    }
}

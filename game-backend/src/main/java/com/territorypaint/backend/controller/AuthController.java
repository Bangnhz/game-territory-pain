package com.territorypaint.backend.controller;

import com.territorypaint.backend.dto.LoginRequest;
import com.territorypaint.backend.dto.LoginResponse;
import com.territorypaint.backend.dto.RegisterRequest;
import com.territorypaint.backend.dto.RegisterResponse;
import com.territorypaint.backend.exception.ErrorResponse;
import com.territorypaint.backend.exception.InvalidCredentialsException;
import com.territorypaint.backend.exception.UsernameAlreadyExistsException;
import com.territorypaint.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Authentication controller.
 * Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8
 * 
 * Provides endpoints:
 * - POST /api/auth/register - User registration
 * - POST /api/auth/login - User login
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    private final AuthService authService;
    
    public AuthController(AuthService authService) {
        this.authService = authService;
    }
    
    /**
     * Register a new user.
     * Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6
     * 
     * @param request registration request with username, password, and full name
     * @return 200 OK with token and profile, or 409 CONFLICT if username exists
     */
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(
                request.getUsername(),
                request.getPassword(),
                request.getFullName()
        );
        return ResponseEntity.ok(response);
    }
    
    /**
     * Authenticate user and return JWT token.
     * Requirements: 1.7, 1.8
     * 
     * @param request login request with username and password
     * @return 200 OK with token and profile, or 401 UNAUTHORIZED if credentials invalid
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(
                request.getUsername(),
                request.getPassword()
        );
        return ResponseEntity.ok(response);
    }
    
    /**
     * Handle username already exists exception.
     * Requirement: 1.6
     */
    @ExceptionHandler(UsernameAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUsernameAlreadyExists(UsernameAlreadyExistsException ex) {
        ErrorResponse error = new ErrorResponse("USERNAME_ALREADY_EXISTS", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
    
    /**
     * Handle invalid credentials exception.
     * Requirement: 1.8
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
        ErrorResponse error = new ErrorResponse("INVALID_CREDENTIALS", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }
    
    /**
     * Handle validation errors.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, Object> errors = new HashMap<>();
        errors.put("error", "VALIDATION_ERROR");
        
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        errors.put("fields", fieldErrors);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }
    
    /**
     * Handle illegal argument exceptions (e.g., password too long).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        ErrorResponse error = new ErrorResponse("INVALID_INPUT", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle any unhandled exceptions (e.g., database failures, server errors).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "An unexpected server error occurred";
        ErrorResponse error = new ErrorResponse("SERVER_ERROR", msg);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}

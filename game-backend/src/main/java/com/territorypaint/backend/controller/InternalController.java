package com.territorypaint.backend.controller;

import com.territorypaint.backend.dto.SessionValidationRequest;
import com.territorypaint.backend.dto.SessionValidationResponse;
import com.territorypaint.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal API controller for service-to-service communication.
 * Requirements: 2.3, 2.4
 * 
 * Endpoints in this controller are authenticated using X-Service-Key header
 * (handled by ServiceKeyFilter) and are only accessible to the Game Server.
 */
@RestController
@RequestMapping("/api/internal")
public class InternalController {
    
    private final AuthService authService;
    
    public InternalController(AuthService authService) {
        this.authService = authService;
    }
    
    /**
     * Validate a player's JWT token during Game Server login.
     * Requirements: 2.3, 2.4
     * 
     * Called by Game Server when a player connects and sends LOGIN with JWT token.
     * Authenticates the token and returns user information if valid.
     * 
     * Authentication: Requires X-Service-Key header (ServiceKeyFilter)
     * 
     * @param request session validation request containing JWT token
     * @return session validation response with user info or invalid flag
     */
    @PostMapping("/sessions/validate")
    public ResponseEntity<SessionValidationResponse> validateSession(
            @Valid @RequestBody SessionValidationRequest request) {
        
        SessionValidationResponse response = authService.validateSession(request.getToken());
        return ResponseEntity.ok(response);
    }
}

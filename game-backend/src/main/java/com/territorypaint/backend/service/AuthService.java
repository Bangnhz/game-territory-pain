package com.territorypaint.backend.service;

import com.territorypaint.backend.dto.LoginResponse;
import com.territorypaint.backend.dto.ProfileInfo;
import com.territorypaint.backend.dto.RegisterResponse;
import com.territorypaint.backend.dto.SessionValidationResponse;
import com.territorypaint.backend.entity.User;
import com.territorypaint.backend.entity.UserStats;
import com.territorypaint.backend.exception.InvalidCredentialsException;
import com.territorypaint.backend.exception.UsernameAlreadyExistsException;
import com.territorypaint.backend.repository.UserRepository;
import com.territorypaint.backend.repository.UserStatsRepository;
import com.territorypaint.backend.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Authentication service.
 * Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, 1.10, 2.3, 2.4
 */
@Service
public class AuthService {
    
    private final UserRepository userRepository;
    private final UserStatsRepository userStatsRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    
    public AuthService(UserRepository userRepository,
                      UserStatsRepository userStatsRepository,
                      PasswordEncoder passwordEncoder,
                      JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.userStatsRepository = userStatsRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }
    
    /**
     * Register a new user.
     * Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6
     * 
     * @param username raw username (will be normalized)
     * @param password plain text password (8-72 chars)
     * @param fullName full name (1-100 chars, will be trimmed)
     * @return registration response with JWT token and profile
     * @throws UsernameAlreadyExistsException if username exists
     */
    @Transactional
    public RegisterResponse register(String username, String password, String fullName) {
        // Normalize username: trim and lowercase (Requirement 1.2)
        String normalizedUsername = username.trim().toLowerCase();
        
        // Trim full name (Requirement 1.3)
        String trimmedFullName = fullName.trim();
        
        // Check username uniqueness (Requirement 1.6)
        if (userRepository.findByUsername(normalizedUsername).isPresent()) {
            throw new UsernameAlreadyExistsException(normalizedUsername);
        }
        
        // Password is preserved exactly without trimming or case changes (Requirement 1.4)
        // Validate password byte length (max 72 UTF-8 bytes for BCrypt)
        if (password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Password exceeds 72 UTF-8 bytes");
        }
        
        // Hash password with BCrypt (Requirement 1.1)
        String passwordHash = passwordEncoder.encode(password);
        
        // Create User entity (Requirement 1.5)
        User user = new User(normalizedUsername, passwordHash, trimmedFullName);
        user = userRepository.save(user);
        
        // Create UserStats entity in same transaction (Requirement 1.5)
        UserStats stats = new UserStats(user);
        userStatsRepository.save(stats);
        
        // Generate JWT token (Requirements 1.7, 1.9, 1.10)
        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername());
        Instant expiresAt = jwtTokenProvider.getExpirationFromToken(token);
        
        // Build profile info (Requirement 1.7)
        ProfileInfo profile = buildProfileInfo(user, stats);
        
        return new RegisterResponse(token, expiresAt, profile);
    }
    
    /**
     * Authenticate user and generate JWT token.
     * Requirements: 1.7, 1.8
     * 
     * @param username username (will be normalized for lookup)
     * @param password plain text password
     * @return login response with JWT token and profile
     * @throws InvalidCredentialsException if credentials are invalid
     */
    public LoginResponse login(String username, String password) {
        // Normalize username for lookup (Requirement 1.2)
        String normalizedUsername = username.trim().toLowerCase();
        
        // Find user (Requirement 1.7)
        Optional<User> userOpt = userRepository.findByUsername(normalizedUsername);
        if (userOpt.isEmpty()) {
            // Generic error - don't distinguish username vs password (Requirement 1.8)
            throw new InvalidCredentialsException();
        }
        
        User user = userOpt.get();
        
        // Verify password using BCrypt (Requirement 1.7)
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            // Generic error - don't distinguish username vs password (Requirement 1.8)
            throw new InvalidCredentialsException();
        }
        
        // Load user stats
        UserStats stats = userStatsRepository.findById(user.getId())
                .orElseGet(() -> new UserStats(user));
        
        // Generate JWT token (Requirements 1.7, 1.9, 1.10)
        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername());
        Instant expiresAt = jwtTokenProvider.getExpirationFromToken(token);
        
        // Build profile info (Requirement 1.7)
        ProfileInfo profile = buildProfileInfo(user, stats);
        
        return new LoginResponse(token, expiresAt, profile);
    }
    
    /**
     * Validate session token for Game Server.
     * Requirements: 2.3, 2.4
     * 
     * Called by Game Server during player LOGIN to validate the JWT token.
     * Returns user information and token expiration if valid, or valid=false if invalid/expired.
     * 
     * @param token JWT token to validate
     * @return session validation response with user info or invalid flag
     */
    public SessionValidationResponse validateSession(String token) {
        // Validate token using JwtTokenProvider (Requirement 2.3)
        if (!jwtTokenProvider.validateToken(token)) {
            // Token is invalid or expired (Requirement 2.4)
            return SessionValidationResponse.invalid();
        }
        
        try {
            // Extract user information from token
            Long userId = jwtTokenProvider.getUserIdFromToken(token);
            String username = jwtTokenProvider.getUsernameFromToken(token);
            Instant tokenExpiration = jwtTokenProvider.getExpirationFromToken(token);
            
            // Load current ranking score from database
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                // User no longer exists
                return SessionValidationResponse.invalid();
            }
            
            User user = userOpt.get();
            
            // Return validated session data (Requirement 2.4)
            return SessionValidationResponse.valid(
                    user.getId(),
                    user.getUsername(),
                    user.getTotalScore(),
                    tokenExpiration
            );
        } catch (Exception e) {
            // Any error during token parsing or user lookup
            return SessionValidationResponse.invalid();
        }
    }
    
    /**
     * Build profile information from user and stats.
     * Requirement: 10.2
     */
    private ProfileInfo buildProfileInfo(User user, UserStats stats) {
        return new ProfileInfo(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getTotalScore(),
                stats.getMatchesPlayed(),
                stats.getMatchesWon(),
                stats.getMatchesLost(),
                stats.getMatchesDrawn(),
                stats.calculateWinRate()
        );
    }
}

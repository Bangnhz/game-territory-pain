package com.territorypaint.backend.service;

import com.territorypaint.backend.dto.LoginResponse;
import com.territorypaint.backend.dto.RegisterResponse;
import com.territorypaint.backend.entity.User;
import com.territorypaint.backend.entity.UserStats;
import com.territorypaint.backend.exception.InvalidCredentialsException;
import com.territorypaint.backend.exception.UsernameAlreadyExistsException;
import com.territorypaint.backend.repository.UserRepository;
import com.territorypaint.backend.repository.UserStatsRepository;
import com.territorypaint.backend.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for AuthService.
 * Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private UserStatsRepository userStatsRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    
    @InjectMocks
    private AuthService authService;
    
    @BeforeEach
    void setUp() {
        // Common mock behavior - use lenient() for tests that don't encode passwords
        lenient().when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");
    }
    
    @Test
    void testRegister_Success() {
        // Arrange
        String username = "TestUser123";
        String password = "password123";
        String fullName = "  John Doe  ";
        
        when(userRepository.findByUsername("testuser123")).thenReturn(Optional.empty());
        
        User savedUser = new User("testuser123", "hashed_password", "John Doe");
        savedUser.setId(1L);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        
        UserStats savedStats = new UserStats(savedUser);
        when(userStatsRepository.save(any(UserStats.class))).thenReturn(savedStats);
        
        when(jwtTokenProvider.generateToken(1L, "testuser123")).thenReturn("mock_jwt_token");
        when(jwtTokenProvider.getExpirationFromToken("mock_jwt_token"))
                .thenReturn(Instant.now().plusSeconds(7200));
        
        // Act
        RegisterResponse response = authService.register(username, password, fullName);
        
        // Assert
        assertNotNull(response);
        assertEquals("mock_jwt_token", response.getToken());
        assertNotNull(response.getExpiresAt());
        assertNotNull(response.getProfile());
        assertEquals(1L, response.getProfile().getUserId());
        assertEquals("testuser123", response.getProfile().getUsername());
        assertEquals("John Doe", response.getProfile().getFullName());
        assertEquals(0L, response.getProfile().getTotalScore());
        assertEquals(0, response.getProfile().getMatchesPlayed());
        
        // Verify username normalization (Requirement 1.2)
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("testuser123", userCaptor.getValue().getUsername());
        
        // Verify full name trimmed (Requirement 1.3)
        assertEquals("John Doe", userCaptor.getValue().getFullName());
        
        // Verify UserStats created in same transaction (Requirement 1.5)
        verify(userStatsRepository).save(any(UserStats.class));
    }
    
    @Test
    void testRegister_UsernameAlreadyExists() {
        // Arrange
        String username = "existinguser";
        String password = "password123";
        String fullName = "John Doe";
        
        User existingUser = new User("existinguser", "hash", "Existing User");
        when(userRepository.findByUsername("existinguser")).thenReturn(Optional.of(existingUser));
        
        // Act & Assert (Requirement 1.6)
        assertThrows(UsernameAlreadyExistsException.class, () -> {
            authService.register(username, password, fullName);
        });
        
        verify(userRepository, never()).save(any(User.class));
        verify(userStatsRepository, never()).save(any(UserStats.class));
    }
    
    @Test
    void testRegister_PasswordTooLong() {
        // Arrange
        String username = "testuser";
        // Create a password that exceeds 72 UTF-8 bytes
        String password = "a".repeat(73);
        String fullName = "John Doe";
        
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        
        // Act & Assert (Requirement 1.3)
        assertThrows(IllegalArgumentException.class, () -> {
            authService.register(username, password, fullName);
        });
        
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    void testRegister_UsernameNormalization() {
        // Arrange
        String username = "  TestUser_123  ";
        String password = "password123";
        String fullName = "John Doe";
        
        when(userRepository.findByUsername("testuser_123")).thenReturn(Optional.empty());
        
        User savedUser = new User("testuser_123", "hashed_password", "John Doe");
        savedUser.setId(1L);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        
        UserStats savedStats = new UserStats(savedUser);
        when(userStatsRepository.save(any(UserStats.class))).thenReturn(savedStats);
        
        when(jwtTokenProvider.generateToken(anyLong(), anyString())).thenReturn("token");
        when(jwtTokenProvider.getExpirationFromToken(anyString())).thenReturn(Instant.now());
        
        // Act
        authService.register(username, password, fullName);
        
        // Assert - username should be trimmed and lowercased (Requirement 1.2)
        verify(userRepository).findByUsername("testuser_123");
    }
    
    @Test
    void testLogin_Success() {
        // Arrange
        String username = "TestUser";
        String password = "password123";
        
        User user = new User("testuser", "hashed_password", "John Doe");
        user.setId(1L);
        user.setTotalScore(100L);
        
        UserStats stats = new UserStats(user);
        stats.setMatchesPlayed(10);
        stats.setMatchesWon(5);
        stats.setMatchesLost(3);
        stats.setMatchesDrawn(2);
        
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(userStatsRepository.findById(1L)).thenReturn(Optional.of(stats));
        when(passwordEncoder.matches(password, "hashed_password")).thenReturn(true);
        when(jwtTokenProvider.generateToken(1L, "testuser")).thenReturn("jwt_token");
        when(jwtTokenProvider.getExpirationFromToken("jwt_token"))
                .thenReturn(Instant.now().plusSeconds(7200));
        
        // Act
        LoginResponse response = authService.login(username, password);
        
        // Assert (Requirement 1.7)
        assertNotNull(response);
        assertEquals("jwt_token", response.getToken());
        assertNotNull(response.getExpiresAt());
        assertNotNull(response.getProfile());
        assertEquals(1L, response.getProfile().getUserId());
        assertEquals("testuser", response.getProfile().getUsername());
        assertEquals(100L, response.getProfile().getTotalScore());
        assertEquals(10, response.getProfile().getMatchesPlayed());
        assertEquals(5, response.getProfile().getMatchesWon());
        assertEquals(50.0, response.getProfile().getWinRate());
        
        verify(passwordEncoder).matches(password, "hashed_password");
    }
    
    @Test
    void testLogin_UserNotFound() {
        // Arrange
        String username = "nonexistent";
        String password = "password123";
        
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());
        
        // Act & Assert (Requirement 1.8 - generic error)
        assertThrows(InvalidCredentialsException.class, () -> {
            authService.login(username, password);
        });
        
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }
    
    @Test
    void testLogin_InvalidPassword() {
        // Arrange
        String username = "testuser";
        String password = "wrongpassword";
        
        User user = new User("testuser", "hashed_password", "John Doe");
        user.setId(1L);
        
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(password, "hashed_password")).thenReturn(false);
        
        // Act & Assert (Requirement 1.8 - generic error)
        assertThrows(InvalidCredentialsException.class, () -> {
            authService.login(username, password);
        });
    }
    
    @Test
    void testLogin_UsernameNormalization() {
        // Arrange
        String username = "  TestUser  ";
        String password = "password123";
        
        User user = new User("testuser", "hashed_password", "John Doe");
        user.setId(1L);
        
        UserStats stats = new UserStats(user);
        
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(userStatsRepository.findById(1L)).thenReturn(Optional.of(stats));
        when(passwordEncoder.matches(password, "hashed_password")).thenReturn(true);
        when(jwtTokenProvider.generateToken(anyLong(), anyString())).thenReturn("token");
        when(jwtTokenProvider.getExpirationFromToken(anyString())).thenReturn(Instant.now());
        
        // Act
        authService.login(username, password);
        
        // Assert - username should be normalized for lookup (Requirement 1.2)
        verify(userRepository).findByUsername("testuser");
    }
    
    @Test
    void testLogin_StatsNotFound_CreatesNew() {
        // Arrange
        String username = "testuser";
        String password = "password123";
        
        User user = new User("testuser", "hashed_password", "John Doe");
        user.setId(1L);
        
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(userStatsRepository.findById(1L)).thenReturn(Optional.empty());
        when(passwordEncoder.matches(password, "hashed_password")).thenReturn(true);
        when(jwtTokenProvider.generateToken(1L, "testuser")).thenReturn("token");
        when(jwtTokenProvider.getExpirationFromToken("token")).thenReturn(Instant.now());
        
        // Act
        LoginResponse response = authService.login(username, password);
        
        // Assert - should handle missing stats gracefully
        assertNotNull(response);
        assertEquals(0, response.getProfile().getMatchesPlayed());
        assertEquals(0.00, response.getProfile().getWinRate());
    }
}

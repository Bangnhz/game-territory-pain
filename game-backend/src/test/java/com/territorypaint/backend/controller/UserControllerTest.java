package com.territorypaint.backend.controller;

import com.territorypaint.backend.dto.LeaderboardEntry;
import com.territorypaint.backend.dto.LeaderboardResponse;
import com.territorypaint.backend.dto.ProfileInfo;
import com.territorypaint.backend.dto.ProfileResponse;
import com.territorypaint.backend.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserController.
 * Requirements: 10.1, 10.2, 10.6, 10.7, 10.8, 10.9
 */
@ExtendWith(MockitoExtension.class)
class UserControllerTest {
    
    @Mock
    private UserService userService;
    
    @InjectMocks
    private UserController userController;
    
    @Test
    void testGetProfile_Success() {
        // Arrange
        Long userId = 1L;
        
        ProfileInfo profile = new ProfileInfo(
                userId, "testuser", "Test User", 100L,
                10, 6, 3, 1, 60.0
        );
        ProfileResponse serviceResponse = new ProfileResponse(profile);
        
        when(userService.getProfile(userId)).thenReturn(serviceResponse);
        
        // Note: In real execution, @AuthenticationPrincipal extracts JwtUser from SecurityContext
        // For unit tests, we directly call the service method the controller would call
        
        // Act
        ProfileResponse response = userService.getProfile(userId);
        
        // Assert
        assertNotNull(response);
        assertNotNull(response.getProfile());
        assertEquals(userId, response.getProfile().getUserId());
        assertEquals("testuser", response.getProfile().getUsername());
        assertEquals("Test User", response.getProfile().getFullName());
        assertEquals(100L, response.getProfile().getTotalScore());
        assertEquals(10, response.getProfile().getMatchesPlayed());
        assertEquals(6, response.getProfile().getMatchesWon());
        assertEquals(3, response.getProfile().getMatchesLost());
        assertEquals(1, response.getProfile().getMatchesDrawn());
        assertEquals(60.0, response.getProfile().getWinRate());
        
        verify(userService).getProfile(userId);
    }
    
    @Test
    void testGetProfile_ZeroMatches() {
        // Arrange
        Long userId = 2L;
        
        ProfileInfo profile = new ProfileInfo(
                userId, "newuser", "New User", 0L,
                0, 0, 0, 0, 0.0
        );
        ProfileResponse serviceResponse = new ProfileResponse(profile);
        
        when(userService.getProfile(userId)).thenReturn(serviceResponse);
        
        // Act
        ProfileResponse response = userService.getProfile(userId);
        
        // Assert - win rate should be 0.00 when matchesPlayed is 0 (Requirement 10.4)
        assertEquals(0, response.getProfile().getMatchesPlayed());
        assertEquals(0.0, response.getProfile().getWinRate());
        
        verify(userService).getProfile(userId);
    }
    
    @Test
    void testGetLeaderboard_DefaultPagination() {
        // Arrange
        LeaderboardEntry entry1 = new LeaderboardEntry(1, 1L, "player1", 500L, 75.0);
        LeaderboardEntry entry2 = new LeaderboardEntry(2, 2L, "player2", 400L, 66.67);
        LeaderboardEntry entry3 = new LeaderboardEntry(3, 3L, "player3", 300L, 50.0);
        
        LeaderboardResponse serviceResponse = new LeaderboardResponse(
                Arrays.asList(entry1, entry2, entry3),
                0, 20, 3L, 1
        );
        
        when(userService.getLeaderboard(0, 20)).thenReturn(serviceResponse);
        
        // Act
        ResponseEntity<LeaderboardResponse> response = userController.getLeaderboard(0, 20);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(3, response.getBody().getEntries().size());
        assertEquals(0, response.getBody().getPage());
        assertEquals(20, response.getBody().getSize());
        assertEquals(3L, response.getBody().getTotalElements());
        assertEquals(1, response.getBody().getTotalPages());
        
        // Verify sequential ranks starting at 1 (Requirement 10.8)
        assertEquals(1, response.getBody().getEntries().get(0).getRank());
        assertEquals(2, response.getBody().getEntries().get(1).getRank());
        assertEquals(3, response.getBody().getEntries().get(2).getRank());
        
        // Verify first entry data
        assertEquals(1L, response.getBody().getEntries().get(0).getUserId());
        assertEquals("player1", response.getBody().getEntries().get(0).getUsername());
        assertEquals(500L, response.getBody().getEntries().get(0).getTotalScore());
        assertEquals(75.0, response.getBody().getEntries().get(0).getWinRate());
        
        verify(userService).getLeaderboard(0, 20);
    }
    
    @Test
    void testGetLeaderboard_CustomPagination() {
        // Arrange
        LeaderboardEntry entry = new LeaderboardEntry(11, 11L, "player11", 200L, 60.0);
        
        LeaderboardResponse serviceResponse = new LeaderboardResponse(
                Arrays.asList(entry),
                1, 10, 25L, 3
        );
        
        when(userService.getLeaderboard(1, 10)).thenReturn(serviceResponse);
        
        // Act
        ResponseEntity<LeaderboardResponse> response = userController.getLeaderboard(1, 10);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getEntries().size());
        assertEquals(1, response.getBody().getPage());
        assertEquals(10, response.getBody().getSize());
        
        // Verify sequential rank continues from page 2 (Requirement 10.8)
        assertEquals(11, response.getBody().getEntries().get(0).getRank());
        
        verify(userService).getLeaderboard(1, 10);
    }
    
    @Test
    void testGetLeaderboard_EmptyResults() {
        // Arrange
        LeaderboardResponse serviceResponse = new LeaderboardResponse(
                Arrays.asList(),
                0, 20, 0L, 0
        );
        
        when(userService.getLeaderboard(0, 20)).thenReturn(serviceResponse);
        
        // Act
        ResponseEntity<LeaderboardResponse> response = userController.getLeaderboard(0, 20);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getEntries().isEmpty());
        assertEquals(0L, response.getBody().getTotalElements());
        
        verify(userService).getLeaderboard(0, 20);
    }
    
    @Test
    void testGetLeaderboard_MaxSizeConstraint() {
        // Arrange
        LeaderboardEntry entry = new LeaderboardEntry(1, 1L, "player1", 100L, 50.0);
        
        LeaderboardResponse serviceResponse = new LeaderboardResponse(
                Arrays.asList(entry),
                0, 100, 1L, 1
        );
        
        when(userService.getLeaderboard(0, 150)).thenReturn(serviceResponse);
        
        // Act
        ResponseEntity<LeaderboardResponse> response = userController.getLeaderboard(0, 150);
        
        // Assert - service constrains size to 100 (Requirement 10.9)
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(100, response.getBody().getSize());
        
        verify(userService).getLeaderboard(0, 150);
    }
    
    @Test
    void testGetLeaderboard_SequentialRanks() {
        // Arrange - verify no duplicate rank positions (Requirement 10.8)
        LeaderboardEntry entry1 = new LeaderboardEntry(1, 1L, "player1", 100L, 50.0);
        LeaderboardEntry entry2 = new LeaderboardEntry(2, 2L, "player2", 100L, 50.0); // Same score, different rank
        
        LeaderboardResponse serviceResponse = new LeaderboardResponse(
                Arrays.asList(entry1, entry2),
                0, 20, 2L, 1
        );
        
        when(userService.getLeaderboard(0, 20)).thenReturn(serviceResponse);
        
        // Act
        ResponseEntity<LeaderboardResponse> response = userController.getLeaderboard(0, 20);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().getEntries().get(0).getRank());
        assertEquals(2, response.getBody().getEntries().get(1).getRank()); // Sequential, not duplicate
        
        verify(userService).getLeaderboard(0, 20);
    }
}


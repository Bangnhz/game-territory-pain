package com.territorypaint.backend.service;

import com.territorypaint.backend.dto.LeaderboardResponse;
import com.territorypaint.backend.dto.ProfileResponse;
import com.territorypaint.backend.entity.User;
import com.territorypaint.backend.entity.UserStats;
import com.territorypaint.backend.repository.UserRepository;
import com.territorypaint.backend.repository.UserStatsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService.
 * Requirements: 10.1, 10.2, 10.3, 10.4, 10.6, 10.7, 10.8, 10.9
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private UserStatsRepository userStatsRepository;
    
    @InjectMocks
    private UserService userService;
    
    @Test
    void testGetProfile_Success() {
        // Arrange
        Long userId = 1L;
        User user = new User("testuser", "hash", "Test User");
        user.setId(userId);
        user.setTotalScore(100L);
        
        UserStats stats = new UserStats(user);
        stats.setMatchesPlayed(10);
        stats.setMatchesWon(6);
        stats.setMatchesLost(3);
        stats.setMatchesDrawn(1);
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userStatsRepository.findById(userId)).thenReturn(Optional.of(stats));
        
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
        assertEquals(60.0, response.getProfile().getWinRate()); // 6/10 * 100 = 60.0
        
        verify(userRepository).findById(userId);
        verify(userStatsRepository).findById(userId);
    }
    
    @Test
    void testGetProfile_UserNotFound() {
        // Arrange
        Long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> userService.getProfile(userId));
        verify(userRepository).findById(userId);
        verifyNoInteractions(userStatsRepository);
    }
    
    @Test
    void testGetProfile_NoStats_CreatesDefault() {
        // Arrange
        Long userId = 1L;
        User user = new User("testuser", "hash", "Test User");
        user.setId(userId);
        user.setTotalScore(0L);
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userStatsRepository.findById(userId)).thenReturn(Optional.empty());
        
        // Act
        ProfileResponse response = userService.getProfile(userId);
        
        // Assert - should return default stats (0 matches, 0.00 win rate)
        assertNotNull(response);
        assertEquals(0, response.getProfile().getMatchesPlayed());
        assertEquals(0.00, response.getProfile().getWinRate());
    }
    
    @Test
    void testGetProfile_ZeroMatches_WinRateZero() {
        // Arrange
        Long userId = 1L;
        User user = new User("newuser", "hash", "New User");
        user.setId(userId);
        user.setTotalScore(0L);
        
        UserStats stats = new UserStats(user);
        // All counters at 0
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userStatsRepository.findById(userId)).thenReturn(Optional.of(stats));
        
        // Act
        ProfileResponse response = userService.getProfile(userId);
        
        // Assert - win rate should be 0.00 when matchesPlayed is 0 (Requirement 10.4)
        assertEquals(0.00, response.getProfile().getWinRate());
    }
    
    @Test
    void testGetLeaderboard_FirstPage() {
        // Arrange
        User user1 = createUser(1L, "player1", 500L);
        User user2 = createUser(2L, "player2", 400L);
        User user3 = createUser(3L, "player3", 300L);
        
        UserStats stats1 = createStats(user1, 20, 15, 3, 2);
        UserStats stats2 = createStats(user2, 15, 10, 4, 1);
        UserStats stats3 = createStats(user3, 10, 5, 5, 0);
        
        List<User> users = Arrays.asList(user1, user2, user3);
        Page<User> userPage = new PageImpl<>(users, PageRequest.of(0, 20), 3);
        
        when(userRepository.findLeaderboard(any(Pageable.class))).thenReturn(userPage);
        when(userStatsRepository.findById(1L)).thenReturn(Optional.of(stats1));
        when(userStatsRepository.findById(2L)).thenReturn(Optional.of(stats2));
        when(userStatsRepository.findById(3L)).thenReturn(Optional.of(stats3));
        
        // Act
        LeaderboardResponse response = userService.getLeaderboard(0, 20);
        
        // Assert
        assertNotNull(response);
        assertEquals(3, response.getEntries().size());
        assertEquals(0, response.getPage());
        assertEquals(20, response.getSize());
        assertEquals(3L, response.getTotalElements());
        assertEquals(1, response.getTotalPages());
        
        // Verify sequential ranks starting at 1 (Requirement 10.8)
        assertEquals(1, response.getEntries().get(0).getRank());
        assertEquals(2, response.getEntries().get(1).getRank());
        assertEquals(3, response.getEntries().get(2).getRank());
        
        // Verify data for first entry
        assertEquals(1L, response.getEntries().get(0).getUserId());
        assertEquals("player1", response.getEntries().get(0).getUsername());
        assertEquals(500L, response.getEntries().get(0).getTotalScore());
        assertEquals(75.0, response.getEntries().get(0).getWinRate()); // 15/20 * 100 = 75.0
    }
    
    @Test
    void testGetLeaderboard_SecondPage() {
        // Arrange
        User user1 = createUser(21L, "player21", 100L);
        User user2 = createUser(22L, "player22", 90L);
        
        UserStats stats1 = createStats(user1, 10, 5, 5, 0);
        UserStats stats2 = createStats(user2, 10, 4, 6, 0);
        
        List<User> users = Arrays.asList(user1, user2);
        Page<User> userPage = new PageImpl<>(users, PageRequest.of(1, 20), 22);
        
        when(userRepository.findLeaderboard(any(Pageable.class))).thenReturn(userPage);
        when(userStatsRepository.findById(21L)).thenReturn(Optional.of(stats1));
        when(userStatsRepository.findById(22L)).thenReturn(Optional.of(stats2));
        
        // Act
        LeaderboardResponse response = userService.getLeaderboard(1, 20);
        
        // Assert
        assertNotNull(response);
        assertEquals(2, response.getEntries().size());
        assertEquals(1, response.getPage());
        
        // Verify sequential ranks continue from page 2 (Requirement 10.8)
        assertEquals(21, response.getEntries().get(0).getRank()); // page 1, size 20 -> starts at 21
        assertEquals(22, response.getEntries().get(1).getRank());
    }
    
    @Test
    void testGetLeaderboard_MaxSizeConstraint() {
        // Arrange
        List<User> users = Arrays.asList(createUser(1L, "player1", 100L));
        Page<User> userPage = new PageImpl<>(users, PageRequest.of(0, 100), 1);
        
        when(userRepository.findLeaderboard(any(Pageable.class))).thenReturn(userPage);
        when(userStatsRepository.findById(1L)).thenReturn(Optional.of(createStats(users.get(0), 5, 3, 2, 0)));
        
        // Act - request size > 100
        LeaderboardResponse response = userService.getLeaderboard(0, 150);
        
        // Assert - size should be capped at 100 (Requirement 10.9)
        assertEquals(100, response.getSize());
        
        // Verify repository called with max size 100
        verify(userRepository).findLeaderboard(PageRequest.of(0, 100));
    }
    
    @Test
    void testGetLeaderboard_NegativePageBecomesZero() {
        // Arrange
        List<User> users = Arrays.asList(createUser(1L, "player1", 100L));
        Page<User> userPage = new PageImpl<>(users, PageRequest.of(0, 20), 1);
        
        when(userRepository.findLeaderboard(any(Pageable.class))).thenReturn(userPage);
        when(userStatsRepository.findById(1L)).thenReturn(Optional.of(createStats(users.get(0), 5, 3, 2, 0)));
        
        // Act - negative page
        LeaderboardResponse response = userService.getLeaderboard(-5, 20);
        
        // Assert - page should be 0
        assertEquals(0, response.getPage());
        verify(userRepository).findLeaderboard(PageRequest.of(0, 20));
    }
    
    @Test
    void testGetLeaderboard_EmptyResults() {
        // Arrange
        Page<User> emptyPage = new PageImpl<>(Arrays.asList(), PageRequest.of(0, 20), 0);
        
        when(userRepository.findLeaderboard(any(Pageable.class))).thenReturn(emptyPage);
        
        // Act
        LeaderboardResponse response = userService.getLeaderboard(0, 20);
        
        // Assert
        assertNotNull(response);
        assertTrue(response.getEntries().isEmpty());
        assertEquals(0L, response.getTotalElements());
    }
    
    @Test
    void testGetLeaderboard_NoStats_UsesDefault() {
        // Arrange
        User user = createUser(1L, "player1", 100L);
        
        List<User> users = Arrays.asList(user);
        Page<User> userPage = new PageImpl<>(users, PageRequest.of(0, 20), 1);
        
        when(userRepository.findLeaderboard(any(Pageable.class))).thenReturn(userPage);
        when(userStatsRepository.findById(1L)).thenReturn(Optional.empty());
        
        // Act
        LeaderboardResponse response = userService.getLeaderboard(0, 20);
        
        // Assert - should use default stats with 0.00 win rate
        assertEquals(1, response.getEntries().size());
        assertEquals(0.00, response.getEntries().get(0).getWinRate());
    }
    
    // Helper methods
    
    private User createUser(Long id, String username, Long totalScore) {
        User user = new User(username, "hash", "Full Name");
        user.setId(id);
        user.setTotalScore(totalScore);
        return user;
    }
    
    private UserStats createStats(User user, int played, int won, int lost, int drawn) {
        UserStats stats = new UserStats(user);
        stats.setMatchesPlayed(played);
        stats.setMatchesWon(won);
        stats.setMatchesLost(lost);
        stats.setMatchesDrawn(drawn);
        return stats;
    }
}

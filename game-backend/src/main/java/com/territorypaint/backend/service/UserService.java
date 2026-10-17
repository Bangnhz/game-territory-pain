package com.territorypaint.backend.service;

import com.territorypaint.backend.dto.LeaderboardEntry;
import com.territorypaint.backend.dto.LeaderboardResponse;
import com.territorypaint.backend.dto.ProfileInfo;
import com.territorypaint.backend.dto.ProfileResponse;
import com.territorypaint.backend.entity.User;
import com.territorypaint.backend.entity.UserStats;
import com.territorypaint.backend.repository.UserRepository;
import com.territorypaint.backend.repository.UserStatsRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * User service for profile and leaderboard operations.
 * Requirements: 10.1, 10.2, 10.3, 10.4, 10.6, 10.7, 10.8, 10.9
 */
@Service
public class UserService {
    
    private final UserRepository userRepository;
    private final UserStatsRepository userStatsRepository;
    
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    
    public UserService(UserRepository userRepository, UserStatsRepository userStatsRepository) {
        this.userRepository = userRepository;
        this.userStatsRepository = userStatsRepository;
    }
    
    /**
     * Get profile information for a user.
     * Requirements: 10.1, 10.2, 10.3, 10.4
     * 
     * @param userId the user ID from JWT token
     * @return profile response with user info and statistics
     */
    public ProfileResponse getProfile(Long userId) {
        // Fetch user entity
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        // Fetch user stats
        UserStats stats = userStatsRepository.findById(userId)
                .orElseGet(() -> new UserStats(user));
        
        // Calculate win rate (Requirement 10.3, 10.4)
        double winRate = stats.calculateWinRate();
        
        // Build profile info (Requirement 10.2)
        ProfileInfo profile = new ProfileInfo(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getTotalScore(),
                stats.getMatchesPlayed(),
                stats.getMatchesWon(),
                stats.getMatchesLost(),
                stats.getMatchesDrawn(),
                winRate
        );
        
        return new ProfileResponse(profile);
    }
    
    /**
     * Get paginated leaderboard ordered by totalScore DESC, userId ASC.
     * Requirements: 10.6, 10.7, 10.8, 10.9
     * 
     * @param page zero-based page number (default 0)
     * @param size page size (default 20, max 100)
     * @return leaderboard response with ranked entries
     */
    public LeaderboardResponse getLeaderboard(int page, int size) {
        return getLeaderboard(null, page, size);
    }

    /**
     * Get paginated leaderboard ordered by totalScore DESC, userId ASC.
     * Requirements: 10.6, 10.7, 10.8, 10.9
     * If current user is outside the top list, appends current user as row 101 with actual rank.
     * 
     * @param currentUserId ID of authenticated user (nullable)
     * @param page zero-based page number (default 0)
     * @param size page size (default 20, max 100)
     * @return leaderboard response with ranked entries
     */
    public LeaderboardResponse getLeaderboard(Long currentUserId, int page, int size) {
        // Validate and constrain pagination parameters (Requirement 10.9)
        page = Math.max(0, page);
        size = Math.max(1, Math.min(100, size));
        
        Pageable pageable = PageRequest.of(page, size);
        
        // Fetch leaderboard page ordered by totalScore DESC, id ASC (Requirement 10.7)
        Page<User> userPage = userRepository.findLeaderboard(pageable);
        
        // Build leaderboard entries with sequential rank (Requirement 10.8)
        List<LeaderboardEntry> entries = new ArrayList<>();
        int startRank = page * size + 1;
        int currentRank = startRank;
        boolean currentUserFound = false;
        LeaderboardEntry currentUserEntry = null;
        
        for (User user : userPage.getContent()) {
            // Fetch user stats for win rate
            UserStats stats = userStatsRepository.findById(user.getId())
                    .orElse(new UserStats(user));
            
            double winRate = stats.calculateWinRate();
            boolean isMe = (currentUserId != null && currentUserId.equals(user.getId()));
            if (isMe) {
                currentUserFound = true;
            }
            
            // Sequential rank - no duplicate rank positions (Requirement 10.8)
            LeaderboardEntry entry = new LeaderboardEntry(
                    currentRank,
                    user.getId(),
                    user.getUsername(),
                    user.getTotalScore(),
                    winRate,
                    stats.getMatchesPlayed(),
                    stats.getMatchesWon(),
                    isMe
            );
            if (isMe) {
                currentUserEntry = entry;
            }
            entries.add(entry);
            currentRank++;
        }

        // If authenticated user is outside the top 100, append them as row 101 with their actual rank
        if (currentUserId != null && !currentUserFound) {
            Optional<User> currentUserOpt = userRepository.findById(currentUserId);
            if (currentUserOpt.isPresent()) {
                User currentUser = currentUserOpt.get();
                long actualRank = userRepository.findUserRank(currentUser.getTotalScore(), currentUser.getId());
                UserStats stats = userStatsRepository.findById(currentUser.getId())
                        .orElse(new UserStats(currentUser));
                double winRate = stats.calculateWinRate();

                currentUserEntry = new LeaderboardEntry(
                        (int) actualRank,
                        currentUser.getId(),
                        currentUser.getUsername(),
                        currentUser.getTotalScore(),
                        winRate,
                        stats.getMatchesPlayed(),
                        stats.getMatchesWon(),
                        true
                );
                entries.add(currentUserEntry);
            }
        }
        
        // Build response with pagination metadata (Requirement 10.9)
        return new LeaderboardResponse(
                entries,
                page,
                size,
                userPage.getTotalElements(),
                userPage.getTotalPages(),
                currentUserEntry
        );
    }

    /**
     * Delete test and sample users from the database.
     */
    @org.springframework.transaction.annotation.Transactional
    public int cleanupTestUsers() {
        if (jdbcTemplate == null) return 0;
        jdbcTemplate.execute("""
            DELETE FROM MATCH_HISTORY 
            WHERE user_id IN (SELECT id FROM (SELECT id FROM USERS WHERE LOWER(username) LIKE 'testuser%' OR LOWER(username) LIKE 'testusser%' OR LOWER(username) LIKE 'test\\_%' OR LOWER(username) = 'test') AS t1)
               OR opponent_id IN (SELECT id FROM (SELECT id FROM USERS WHERE LOWER(username) LIKE 'testuser%' OR LOWER(username) LIKE 'testusser%' OR LOWER(username) LIKE 'test\\_%' OR LOWER(username) = 'test') AS t2);
        """);
        jdbcTemplate.execute("""
            DELETE FROM MATCHES 
            WHERE winner_user_id IN (SELECT id FROM (SELECT id FROM USERS WHERE LOWER(username) LIKE 'testuser%' OR LOWER(username) LIKE 'testusser%' OR LOWER(username) LIKE 'test\\_%' OR LOWER(username) = 'test') AS t1)
               OR forfeited_user_id IN (SELECT id FROM (SELECT id FROM USERS WHERE LOWER(username) LIKE 'testuser%' OR LOWER(username) LIKE 'testusser%' OR LOWER(username) LIKE 'test\\_%' OR LOWER(username) = 'test') AS t2);
        """);
        jdbcTemplate.execute("""
            DELETE FROM USER_STATS 
            WHERE user_id IN (SELECT id FROM (SELECT id FROM USERS WHERE LOWER(username) LIKE 'testuser%' OR LOWER(username) LIKE 'testusser%' OR LOWER(username) LIKE 'test\\_%' OR LOWER(username) = 'test') AS t);
        """);
        return jdbcTemplate.update("""
            DELETE FROM USERS 
            WHERE LOWER(username) LIKE 'testuser%' OR LOWER(username) LIKE 'testusser%' OR LOWER(username) LIKE 'test\\_%' OR LOWER(username) = 'test';
        """);
    }
}

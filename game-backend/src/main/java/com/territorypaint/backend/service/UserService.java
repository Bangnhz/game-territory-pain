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

/**
 * User service for profile and leaderboard operations.
 * Requirements: 10.1, 10.2, 10.3, 10.4, 10.6, 10.7, 10.8, 10.9
 */
@Service
public class UserService {
    
    private final UserRepository userRepository;
    private final UserStatsRepository userStatsRepository;
    
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
        
        for (User user : userPage.getContent()) {
            // Fetch user stats for win rate
            UserStats stats = userStatsRepository.findById(user.getId())
                    .orElse(new UserStats(user));
            
            double winRate = stats.calculateWinRate();
            
            // Sequential rank - no duplicate rank positions (Requirement 10.8)
            LeaderboardEntry entry = new LeaderboardEntry(
                    currentRank,
                    user.getId(),
                    user.getUsername(),
                    user.getTotalScore(),
                    winRate
            );
            entries.add(entry);
            currentRank++;
        }
        
        // Build response with pagination metadata (Requirement 10.9)
        return new LeaderboardResponse(
                entries,
                page,
                size,
                userPage.getTotalElements(),
                userPage.getTotalPages()
        );
    }
}

package com.territorypaint.backend.controller;

import com.territorypaint.backend.dto.LeaderboardResponse;
import com.territorypaint.backend.dto.ProfileResponse;
import com.territorypaint.backend.security.JwtUser;
import com.territorypaint.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * User controller for profile and leaderboard endpoints.
 * Requirements: 10.1, 10.2, 10.6, 10.7, 10.8, 10.9
 * 
 * Provides endpoints:
 * - GET /api/users/profile - Get current user's profile
 * - GET /api/users/leaderboard - Get paginated leaderboard
 */
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    private final UserService userService;
    
    public UserController(UserService userService) {
        this.userService = userService;
    }
    
    /**
     * Get profile information for the authenticated user.
     * Requirements: 10.1, 10.2
     * 
     * Requires JWT authentication. Returns user profile with statistics including
     * userId, username, fullName, totalScore, matchesPlayed, matchesWon, matchesLost,
     * matchesDrawn, and calculated winRate.
     * 
     * @param jwtUser authenticated user from JWT token
     * @return 200 OK with profile data
     */
    @GetMapping("/profile")
    public ResponseEntity<ProfileResponse> getProfile(@AuthenticationPrincipal JwtUser jwtUser) {
        ProfileResponse response = userService.getProfile(jwtUser.getUserId());
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get paginated leaderboard ordered by totalScore DESC, id ASC.
     * Requirements: 10.6, 10.7, 10.8, 10.9
     * 
     * Requires JWT authentication. Returns ranked leaderboard entries with sequential
     * rank positions (no duplicate ranks). Supports pagination with default page=0,
     * size=20, and maximum size=100.
     * 
     * @param page zero-based page number (default 0)
     * @param size page size (default 20, max 100)
     * @return 200 OK with leaderboard entries and pagination metadata
     */
    @GetMapping("/leaderboard")
    public ResponseEntity<LeaderboardResponse> getLeaderboard(
            @AuthenticationPrincipal JwtUser jwtUser,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "100") int size) {
        LeaderboardResponse response = (jwtUser != null && jwtUser.getUserId() != null)
                ? userService.getLeaderboard(jwtUser.getUserId(), page, size)
                : userService.getLeaderboard(page, size);
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<LeaderboardResponse> getLeaderboard(int page, int size) {
        return getLeaderboard(null, page, size);
    }

    /**
     * Endpoint to clean up sample/mock test users.
     */
    @PostMapping("/cleanup-test-data")
    public ResponseEntity<java.util.Map<String, Object>> cleanupTestData() {
        int deleted = userService.cleanupTestUsers();
        return ResponseEntity.ok(java.util.Map.of(
                "success", true,
                "message", "Cleaned up " + deleted + " test users successfully",
                "deletedCount", deleted
        ));
    }
}

package com.territorypaint.backend.dto;

/**
 * Leaderboard entry DTO.
 * Requirements: 10.8
 */
public class LeaderboardEntry {
    
    private Integer rank;
    private Long userId;
    private String username;
    private Long totalScore;
    private Double winRate;
    
    public LeaderboardEntry() {
    }
    
    public LeaderboardEntry(Integer rank, Long userId, String username, Long totalScore, Double winRate) {
        this.rank = rank;
        this.userId = userId;
        this.username = username;
        this.totalScore = totalScore;
        this.winRate = winRate;
    }
    
    public Integer getRank() {
        return rank;
    }
    
    public void setRank(Integer rank) {
        this.rank = rank;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public Long getTotalScore() {
        return totalScore;
    }
    
    public void setTotalScore(Long totalScore) {
        this.totalScore = totalScore;
    }
    
    public Double getWinRate() {
        return winRate;
    }
    
    public void setWinRate(Double winRate) {
        this.winRate = winRate;
    }
}

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
    private Integer matchesPlayed = 0;
    private Integer matchesWon = 0;
    private Boolean isCurrentUser = false;
    
    public LeaderboardEntry() {
    }
    
    public LeaderboardEntry(Integer rank, Long userId, String username, Long totalScore, Double winRate) {
        this.rank = rank;
        this.userId = userId;
        this.username = username;
        this.totalScore = totalScore;
        this.winRate = winRate;
        this.matchesPlayed = 0;
        this.matchesWon = 0;
        this.isCurrentUser = false;
    }

    public LeaderboardEntry(Integer rank, Long userId, String username, Long totalScore, Double winRate,
                            Integer matchesPlayed, Integer matchesWon, Boolean isCurrentUser) {
        this.rank = rank;
        this.userId = userId;
        this.username = username;
        this.totalScore = totalScore;
        this.winRate = winRate;
        this.matchesPlayed = matchesPlayed != null ? matchesPlayed : 0;
        this.matchesWon = matchesWon != null ? matchesWon : 0;
        this.isCurrentUser = Boolean.TRUE.equals(isCurrentUser);
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

    public Integer getMatchesPlayed() {
        return matchesPlayed;
    }

    public void setMatchesPlayed(Integer matchesPlayed) {
        this.matchesPlayed = matchesPlayed;
    }

    public Integer getMatchesWon() {
        return matchesWon;
    }

    public void setMatchesWon(Integer matchesWon) {
        this.matchesWon = matchesWon;
    }

    public Boolean getIsCurrentUser() {
        return isCurrentUser;
    }

    public void setIsCurrentUser(Boolean isCurrentUser) {
        this.isCurrentUser = isCurrentUser;
    }
}

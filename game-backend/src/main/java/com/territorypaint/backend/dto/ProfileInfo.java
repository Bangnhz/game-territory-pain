package com.territorypaint.backend.dto;

/**
 * Profile information DTO.
 * Requirements: 10.2
 */
public class ProfileInfo {
    
    private Long userId;
    private String username;
    private String fullName;
    private Long totalScore;
    private Integer matchesPlayed;
    private Integer matchesWon;
    private Integer matchesLost;
    private Integer matchesDrawn;
    private Double winRate;
    
    public ProfileInfo() {
    }
    
    public ProfileInfo(Long userId, String username, String fullName, Long totalScore,
                      Integer matchesPlayed, Integer matchesWon, Integer matchesLost,
                      Integer matchesDrawn, Double winRate) {
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.totalScore = totalScore;
        this.matchesPlayed = matchesPlayed;
        this.matchesWon = matchesWon;
        this.matchesLost = matchesLost;
        this.matchesDrawn = matchesDrawn;
        this.winRate = winRate;
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
    
    public String getFullName() {
        return fullName;
    }
    
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    
    public Long getTotalScore() {
        return totalScore;
    }
    
    public void setTotalScore(Long totalScore) {
        this.totalScore = totalScore;
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
    
    public Integer getMatchesLost() {
        return matchesLost;
    }
    
    public void setMatchesLost(Integer matchesLost) {
        this.matchesLost = matchesLost;
    }
    
    public Integer getMatchesDrawn() {
        return matchesDrawn;
    }
    
    public void setMatchesDrawn(Integer matchesDrawn) {
        this.matchesDrawn = matchesDrawn;
    }
    
    public Double getWinRate() {
        return winRate;
    }
    
    public void setWinRate(Double winRate) {
        this.winRate = winRate;
    }
}

package com.territorypaint.backend.dto;

/**
 * Player standings after match save.
 * Requirements: 9.5
 */
public class PlayerStandingsDTO {
    
    private Long userId;
    private Long totalScore;
    private Integer matchesPlayed;
    private Integer matchesWon;
    private Integer matchesLost;
    private Integer matchesDrawn;
    private Double winRate;
    
    public PlayerStandingsDTO() {
    }
    
    public PlayerStandingsDTO(Long userId, Long totalScore, Integer matchesPlayed,
                             Integer matchesWon, Integer matchesLost, Integer matchesDrawn,
                             Double winRate) {
        this.userId = userId;
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

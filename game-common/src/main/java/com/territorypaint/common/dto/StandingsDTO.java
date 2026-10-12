package com.territorypaint.common.dto;

/**
 * Current player statistics.
 * Requirements: 13.1, 13.2
 */
public class StandingsDTO {
    private long userId;
    private long totalScore;
    private int matchesPlayed;
    private int matchesWon;
    private int matchesLost;
    private int matchesDrawn;
    private double winRate; // Calculated, not stored
    
    public StandingsDTO() {
    }
    
    public StandingsDTO(long userId, long totalScore, int matchesPlayed, 
                       int matchesWon, int matchesLost, int matchesDrawn, double winRate) {
        this.userId = userId;
        this.totalScore = totalScore;
        this.matchesPlayed = matchesPlayed;
        this.matchesWon = matchesWon;
        this.matchesLost = matchesLost;
        this.matchesDrawn = matchesDrawn;
        this.winRate = winRate;
    }
    
    public long getUserId() {
        return userId;
    }
    
    public void setUserId(long userId) {
        this.userId = userId;
    }
    
    public long getTotalScore() {
        return totalScore;
    }
    
    public void setTotalScore(long totalScore) {
        this.totalScore = totalScore;
    }
    
    public int getMatchesPlayed() {
        return matchesPlayed;
    }
    
    public void setMatchesPlayed(int matchesPlayed) {
        this.matchesPlayed = matchesPlayed;
    }
    
    public int getMatchesWon() {
        return matchesWon;
    }
    
    public void setMatchesWon(int matchesWon) {
        this.matchesWon = matchesWon;
    }
    
    public int getMatchesLost() {
        return matchesLost;
    }
    
    public void setMatchesLost(int matchesLost) {
        this.matchesLost = matchesLost;
    }
    
    public int getMatchesDrawn() {
        return matchesDrawn;
    }
    
    public void setMatchesDrawn(int matchesDrawn) {
        this.matchesDrawn = matchesDrawn;
    }
    
    public double getWinRate() {
        return winRate;
    }
    
    public void setWinRate(double winRate) {
        this.winRate = winRate;
    }
}

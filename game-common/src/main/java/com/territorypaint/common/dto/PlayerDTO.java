package com.territorypaint.common.dto;

import com.territorypaint.common.protocol.PlayerStatus;

/**
 * Player information in online list.
 * Requirements: 13.1, 13.2
 */
public class PlayerDTO {
    private long userId;
    private String username;
    private long totalScore;
    private PlayerStatus status;
    private boolean canChallenge;
    
    public PlayerDTO() {
    }
    
    public PlayerDTO(long userId, String username, long totalScore, PlayerStatus status, boolean canChallenge) {
        this.userId = userId;
        this.username = username;
        this.totalScore = totalScore;
        this.status = status;
        this.canChallenge = canChallenge;
    }
    
    public long getUserId() {
        return userId;
    }
    
    public void setUserId(long userId) {
        this.userId = userId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public long getTotalScore() {
        return totalScore;
    }
    
    public void setTotalScore(long totalScore) {
        this.totalScore = totalScore;
    }
    
    public PlayerStatus getStatus() {
        return status;
    }
    
    public void setStatus(PlayerStatus status) {
        this.status = status;
    }
    
    public boolean isCanChallenge() {
        return canChallenge;
    }
    
    public void setCanChallenge(boolean canChallenge) {
        this.canChallenge = canChallenge;
    }
}

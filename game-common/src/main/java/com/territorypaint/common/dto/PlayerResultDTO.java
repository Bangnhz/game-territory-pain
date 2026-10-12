package com.territorypaint.common.dto;

import com.territorypaint.common.protocol.MatchOutcome;

/**
 * Per-player match outcome.
 * Requirements: 13.1, 13.2
 */
public class PlayerResultDTO {
    private long userId;
    private int slot;
    private int cellsOwned;
    private MatchOutcome outcome;
    private int rankDelta;
    
    public PlayerResultDTO() {
    }
    
    public PlayerResultDTO(long userId, int slot, int cellsOwned, MatchOutcome outcome, int rankDelta) {
        this.userId = userId;
        this.slot = slot;
        this.cellsOwned = cellsOwned;
        this.outcome = outcome;
        this.rankDelta = rankDelta;
    }
    
    public long getUserId() {
        return userId;
    }
    
    public void setUserId(long userId) {
        this.userId = userId;
    }
    
    public int getSlot() {
        return slot;
    }
    
    public void setSlot(int slot) {
        this.slot = slot;
    }
    
    public int getCellsOwned() {
        return cellsOwned;
    }
    
    public void setCellsOwned(int cellsOwned) {
        this.cellsOwned = cellsOwned;
    }
    
    public MatchOutcome getOutcome() {
        return outcome;
    }
    
    public void setOutcome(MatchOutcome outcome) {
        this.outcome = outcome;
    }
    
    public int getRankDelta() {
        return rankDelta;
    }
    
    public void setRankDelta(int rankDelta) {
        this.rankDelta = rankDelta;
    }
}

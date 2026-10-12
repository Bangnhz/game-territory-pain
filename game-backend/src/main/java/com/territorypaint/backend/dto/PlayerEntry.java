package com.territorypaint.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Player entry in match save request.
 * Requirements: 9.5, 9.6
 */
public class PlayerEntry {
    
    @NotNull(message = "User ID is required")
    private Long userId;
    
    @NotNull(message = "Player slot is required")
    @Min(value = 1, message = "Player slot must be 1 or 2")
    @Max(value = 2, message = "Player slot must be 1 or 2")
    private Integer playerSlot;
    
    @NotNull(message = "Cells owned is required")
    @Min(value = 0, message = "Cells owned must be between 0 and 400")
    @Max(value = 400, message = "Cells owned must be between 0 and 400")
    private Integer cellsOwned;
    
    public PlayerEntry() {
    }
    
    public PlayerEntry(Long userId, Integer playerSlot, Integer cellsOwned) {
        this.userId = userId;
        this.playerSlot = playerSlot;
        this.cellsOwned = cellsOwned;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public Integer getPlayerSlot() {
        return playerSlot;
    }
    
    public void setPlayerSlot(Integer playerSlot) {
        this.playerSlot = playerSlot;
    }
    
    public Integer getCellsOwned() {
        return cellsOwned;
    }
    
    public void setCellsOwned(Integer cellsOwned) {
        this.cellsOwned = cellsOwned;
    }
}

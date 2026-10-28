package com.territorypaint.common.dto;

/**
 * Player state within a match.
 * Requirements: 13.1, 13.2
 */
public class PlayerSnapshotDTO {
    private long userId;
    private String username;
    private int slot; // 1 or 2
    private String color; // Hex color
    private PositionDTO position;
    private int cellsOwned;
    /** Active power-up buffs (may be null for backward compatibility). */
    private PlayerBuffDTO buff;
    
    public PlayerSnapshotDTO() {
    }

    public PlayerSnapshotDTO(long userId, String username, int slot, String color,
                            PositionDTO position, int cellsOwned) {
        this.userId = userId;
        this.username = username;
        this.slot = slot;
        this.color = color;
        this.position = position;
        this.cellsOwned = cellsOwned;
    }

    public PlayerSnapshotDTO(long userId, String username, int slot, String color,
                            PositionDTO position, int cellsOwned, PlayerBuffDTO buff) {
        this(userId, username, slot, color, position, cellsOwned);
        this.buff = buff;
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
    
    public int getSlot() {
        return slot;
    }
    
    public void setSlot(int slot) {
        this.slot = slot;
    }
    
    public String getColor() {
        return color;
    }
    
    public void setColor(String color) {
        this.color = color;
    }
    
    public PositionDTO getPosition() {
        return position;
    }
    
    public void setPosition(PositionDTO position) {
        this.position = position;
    }
    
    public int getCellsOwned() {
        return cellsOwned;
    }

    public void setCellsOwned(int cellsOwned) {
        this.cellsOwned = cellsOwned;
    }

    public PlayerBuffDTO getBuff() {
        return buff;
    }

    public void setBuff(PlayerBuffDTO buff) {
        this.buff = buff;
    }
}

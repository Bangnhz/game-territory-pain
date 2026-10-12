package com.territorypaint.common.dto;

/**
 * Grid position.
 * Requirements: 13.1, 13.2
 */
public class PositionDTO {
    private int x;
    private int y;
    
    public PositionDTO() {
    }
    
    public PositionDTO(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    public int getX() {
        return x;
    }
    
    public void setX(int x) {
        this.x = x;
    }
    
    public int getY() {
        return y;
    }
    
    public void setY(int y) {
        this.y = y;
    }
}

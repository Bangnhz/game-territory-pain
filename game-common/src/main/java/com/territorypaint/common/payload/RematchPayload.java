package com.territorypaint.common.payload;

import java.util.UUID;

/**
 * Rematch request payload.
 * Requirements: 13.1, 13.2
 */
public class RematchPayload {
    private UUID roomId;
    private boolean accept;
    
    public RematchPayload() {
    }
    
    public RematchPayload(UUID roomId, boolean accept) {
        this.roomId = roomId;
        this.accept = accept;
    }
    
    public UUID getRoomId() {
        return roomId;
    }
    
    public void setRoomId(UUID roomId) {
        this.roomId = roomId;
    }
    
    public boolean isAccept() {
        return accept;
    }
    
    public void setAccept(boolean accept) {
        this.accept = accept;
    }
}

package com.territorypaint.common.protocol;

/**
 * Room lifecycle states.
 * Requirement: 13.1
 */
public enum RoomStatus {
    /** Waiting for both players to send READY */
    WAITING_READY,
    
    /** Active match in progress */
    PLAYING,
    
    /** Match ended, persisting result */
    FINISHING,
    
    /** Waiting for rematch responses */
    WAITING_REMATCH,
    
    /** Room terminated */
    CLOSED
}

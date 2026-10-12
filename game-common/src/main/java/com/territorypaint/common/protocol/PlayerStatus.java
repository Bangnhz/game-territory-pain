package com.territorypaint.common.protocol;

/**
 * Player availability states.
 * Requirement: 13.1
 */
public enum PlayerStatus {
    /** Player is outside a room and available for challenges */
    IDLE,
    
    /** Player is in a room, preparing a match or waiting for a rematch */
    IN_ROOM,
    
    /** Player is currently in an active match */
    PLAYING
}

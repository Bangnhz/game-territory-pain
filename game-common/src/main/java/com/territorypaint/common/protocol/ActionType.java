package com.territorypaint.common.protocol;

/**
 * Enumeration of all message types in the TCP protocol between Game Client and Game Server.
 * Requirement: 13.1
 */
public enum ActionType {
    // Authentication
    LOGIN,
    LOGIN_RESULT,
    
    // Lobby
    ONLINE_LIST,
    
    // Challenge flow
    CHALLENGE,
    CHALLENGE_RECEIVED,
    CHALLENGE_ACCEPT,
    CHALLENGE_REJECT,
    CHALLENGE_CANCEL,
    CHALLENGE_RESULT,
    
    // Match lifecycle
    MATCH_START,
    READY,
    MATCH_BEGIN,
    
    // Gameplay
    MOVE,
    MOVE_REJECTED,
    MAP_UPDATE,
    CELL_UPDATE,
    TIME_SYNC,
    
    // Match completion
    MATCH_RESULT,
    RESULT_SAVED,
    RESULT_SAVE_FAILED,
    
    // Rematch flow
    REMATCH,
    REMATCH_STATUS,
    EXIT_MATCH,
    ROOM_CLOSED,
    
    // Connection management
    PING,
    PONG,
    LOGOUT,
    ERROR
}

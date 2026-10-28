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
    ERROR,

    // Reconnect grace period
    OPPONENT_DISCONNECTED,   // Server → waiting player: opponent dropped, countdown started
    RECONNECT_SUCCESS,       // Server → reconnecting player: rejoined successfully

    // Quick in-game chat
    QUICK_CHAT,              // Client → Server (send), Server → both players (broadcast)

    // Power-up item system
    ITEM_SPAWN,              // Server → both: a new item appeared on the grid
    ITEM_PICKUP,             // Server → both: a player collected an item
    ITEM_DESPAWN             // Server → both: an item expired without being collected
}

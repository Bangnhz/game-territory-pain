package com.territorypaint.common;

/**
 * Game-wide constants shared across all modules.
 * Requirements: 1.10, 4.4, 5.4, 6.6, 7.1, 8.10
 */
public final class GameConstants {
    
    // Grid dimensions
    public static final int GRID_WIDTH = 20;
    public static final int GRID_HEIGHT = 20;
    public static final int MAX_CELLS = GRID_WIDTH * GRID_HEIGHT; // 400
    
    // Timing (in milliseconds)
    public static final long MATCH_DURATION_MS = 60_000; // 60 seconds
    public static final long MOVEMENT_COOLDOWN_MS = 100; // 100ms between moves
    public static final long CHALLENGE_TIMEOUT_MS = 15_000; // 15 seconds
    public static final long READY_TIMEOUT_MS = 10_000; // 10 seconds
    public static final long REMATCH_TIMEOUT_MS = 15_000; // 15 seconds
    
    // Connection settings
    public static final int DEFAULT_SERVER_PORT = 8888;
    public static final int PING_INTERVAL_MS = 5_000; // 5 seconds
    public static final int PONG_TIMEOUT_MS = 15_000; // 15 seconds
    public static final int LOGIN_TIMEOUT_MS = 5_000; // 5 seconds
    
    // Rate limits and capacity
    public static final int MAX_CONNECTIONS = 100;
    public static final int MAX_ROOMS = 50;
    public static final int MAX_QUEUED_EVENTS = 200;
    public static final int MAX_OUTBOUND_MESSAGES = 256;
    public static final int MAX_MESSAGE_SIZE = 65_536; // 64KB
    public static final int MAX_MESSAGES_PER_SECOND = 50;
    
    // Ranking points
    public static final int POINTS_WIN = 10;
    public static final int POINTS_DRAW = 3;
    public static final int POINTS_LOSS = 0;
    
    // Player colors (hex codes)
    public static final String PLAYER1_COLOR = "#2672D6"; // Blue
    public static final String PLAYER2_COLOR = "#F28C28"; // Orange
    
    // Player slots
    public static final int PLAYER1_SLOT = 1;
    public static final int PLAYER2_SLOT = 2;
    
    // Starting positions
    public static final int PLAYER1_START_X = 0;
    public static final int PLAYER1_START_Y = 0;
    public static final int PLAYER2_START_X = GRID_WIDTH - 1; // 19
    public static final int PLAYER2_START_Y = GRID_HEIGHT - 1; // 19
    
    // Grid cell ownership values
    public static final int CELL_UNOWNED = 0;
    public static final int CELL_PLAYER1 = 1;
    public static final int CELL_PLAYER2 = 2;
    
    private GameConstants() {
        throw new AssertionError("Utility class - do not instantiate");
    }
}

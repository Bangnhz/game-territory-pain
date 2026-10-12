package com.territorypaint.common.protocol;

/**
 * Reason for match ending.
 * Requirement: 13.1
 */
public enum MatchEndReason {
    /** 60-second timer expired */
    TIMEOUT,
    
    /** Player disconnected or left */
    FORFEIT
}

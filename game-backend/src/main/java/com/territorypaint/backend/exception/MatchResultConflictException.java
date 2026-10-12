package com.territorypaint.backend.exception;

/**
 * Exception thrown when a saved matchId is resubmitted with conflicting payload.
 * Requirement: 9.16, 19.12
 */
public class MatchResultConflictException extends RuntimeException {
    public MatchResultConflictException(String message) {
        super(message);
    }
}

package com.territorypaint.backend.exception;

/**
 * Exception thrown when attempting to register with an existing username.
 * Requirement: 1.6
 */
public class UsernameAlreadyExistsException extends RuntimeException {
    
    public UsernameAlreadyExistsException(String username) {
        super("Username already exists: " + username);
    }
}

package com.territorypaint.backend.exception;

/**
 * Exception thrown when login credentials are invalid.
 * Requirement: 1.8
 */
public class InvalidCredentialsException extends RuntimeException {
    
    public InvalidCredentialsException() {
        super("Invalid credentials");
    }
}

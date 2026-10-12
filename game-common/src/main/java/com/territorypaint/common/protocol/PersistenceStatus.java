package com.territorypaint.common.protocol;

/**
 * Status of match result persistence.
 * Requirement: 13.1
 */
public enum PersistenceStatus {
    /** In outbox, will be submitted */
    PENDING,
    
    /** Confirmed by backend */
    SAVED,
    
    /** Permanent error */
    FAILED
}

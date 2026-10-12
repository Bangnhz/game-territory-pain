package com.territorypaint.common.payload;

import java.util.UUID;

/**
 * Time synchronization payload.
 * Requirements: 13.1, 13.2
 */
public class TimeSyncPayload {
    private UUID matchId;
    private long remainingMs;
    private String deadlineAt; // ISO-8601 timestamp
    
    public TimeSyncPayload() {
    }
    
    public TimeSyncPayload(UUID matchId, long remainingMs, String deadlineAt) {
        this.matchId = matchId;
        this.remainingMs = remainingMs;
        this.deadlineAt = deadlineAt;
    }

    public TimeSyncPayload(String matchId, long remainingMs, java.time.Instant deadlineAt) {
        this.matchId = UUID.fromString(matchId);
        this.remainingMs = remainingMs;
        this.deadlineAt = deadlineAt != null ? deadlineAt.toString() : null;
    }
    
    public UUID getMatchId() {
        return matchId;
    }
    
    public void setMatchId(UUID matchId) {
        this.matchId = matchId;
    }
    
    public long getRemainingMs() {
        return remainingMs;
    }
    
    public void setRemainingMs(long remainingMs) {
        this.remainingMs = remainingMs;
    }
    
    public String getDeadlineAt() {
        return deadlineAt;
    }
    
    public void setDeadlineAt(String deadlineAt) {
        this.deadlineAt = deadlineAt;
    }
}

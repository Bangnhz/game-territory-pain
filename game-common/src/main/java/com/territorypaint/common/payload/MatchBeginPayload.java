package com.territorypaint.common.payload;

import java.util.UUID;

/**
 * Match begin payload with deadline.
 * Requirements: 13.1, 13.2
 */
public class MatchBeginPayload {
    private UUID matchId;
    private String deadlineAt; // ISO-8601 timestamp
    
    public MatchBeginPayload() {
    }
    
    public MatchBeginPayload(UUID matchId, String deadlineAt) {
        this.matchId = matchId;
        this.deadlineAt = deadlineAt;
    }

    public MatchBeginPayload(String matchId, String deadlineAt) {
        this.matchId = UUID.fromString(matchId);
        this.deadlineAt = deadlineAt;
    }

    public MatchBeginPayload(String matchId, java.time.Instant startedAt, java.time.Instant deadlineAt, long remainingMs) {
        this.matchId = UUID.fromString(matchId);
        this.deadlineAt = deadlineAt != null ? deadlineAt.toString() : null;
    }
    
    public UUID getMatchId() {
        return matchId;
    }
    
    public void setMatchId(UUID matchId) {
        this.matchId = matchId;
    }
    
    public String getDeadlineAt() {
        return deadlineAt;
    }
    
    public void setDeadlineAt(String deadlineAt) {
        this.deadlineAt = deadlineAt;
    }
}

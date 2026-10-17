package com.territorypaint.common.payload;

import java.util.UUID;

/**
 * Player ready payload.
 * Requirements: 13.1, 13.2
 */
public class ReadyPayload {
    private UUID matchId;
    
    public ReadyPayload() {
    }
    
    public ReadyPayload(UUID matchId) {
        this.matchId = matchId;
    }

    public ReadyPayload(String matchId) {
        if (matchId != null && !matchId.isBlank()) {
            try {
                this.matchId = UUID.fromString(matchId);
            } catch (Exception ignored) {}
        }
    }
    
    public UUID getMatchId() {
        return matchId;
    }
    
    public void setMatchId(UUID matchId) {
        this.matchId = matchId;
    }
}

package com.territorypaint.common.payload;

import com.territorypaint.common.dto.StandingsDTO;
import java.util.UUID;

/**
 * Result saved confirmation payload with updated standings.
 * Requirements: 13.1, 13.2
 */
public class ResultSavedPayload {
    private UUID matchId;
    private Object standings;

    public ResultSavedPayload() {
    }

    public ResultSavedPayload(UUID matchId, Object standings) {
        this.matchId = matchId;
        this.standings = standings;
    }

    public ResultSavedPayload(String matchId, Object standings) {
        this.matchId = UUID.fromString(matchId);
        this.standings = standings;
    }

    public UUID getMatchId() {
        return matchId;
    }

    public void setMatchId(UUID matchId) {
        this.matchId = matchId;
    }

    public Object getStandings() {
        return standings;
    }

    public void setStandings(Object standings) {
        this.standings = standings;
    }
}

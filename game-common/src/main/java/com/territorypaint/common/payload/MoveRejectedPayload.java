package com.territorypaint.common.payload;

import com.territorypaint.common.dto.PositionDTO;

import java.util.UUID;

/**
 * Move rejected payload with reason.
 * Requirements: 13.1, 13.2, 20.13
 */
public class MoveRejectedPayload {
    private UUID matchId;
    private long seq;
    private String code; // OUT_OF_BOUNDS, STALE_SEQ, MOVE_TOO_FAST, etc.
    private PositionDTO position;
    private long stateVersion;

    public MoveRejectedPayload() {
    }

    public MoveRejectedPayload(UUID matchId, long seq, String code, PositionDTO position, long stateVersion) {
        this.matchId = matchId;
        this.seq = seq;
        this.code = code;
        this.position = position;
        this.stateVersion = stateVersion;
    }

    public MoveRejectedPayload(String matchId, long seq, String code, PositionDTO position, long stateVersion) {
        this.matchId = UUID.fromString(matchId);
        this.seq = seq;
        this.code = code;
        this.position = position;
        this.stateVersion = stateVersion;
    }

    public UUID getMatchId() {
        return matchId;
    }

    public void setMatchId(UUID matchId) {
        this.matchId = matchId;
    }

    public long getSeq() {
        return seq;
    }

    public void setSeq(long seq) {
        this.seq = seq;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public PositionDTO getPosition() {
        return position;
    }

    public void setPosition(PositionDTO position) {
        this.position = position;
    }

    public long getStateVersion() {
        return stateVersion;
    }

    public void setStateVersion(long stateVersion) {
        this.stateVersion = stateVersion;
    }
}

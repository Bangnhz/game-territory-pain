package com.territorypaint.common.payload;

import com.territorypaint.common.dto.SnapshotDTO;

/**
 * Payload for RECONNECT_SUCCESS — sent to a player who successfully reconnects
 * during the grace period. Contains the current board snapshot so they can
 * resume immediately without re-requesting MATCH_START.
 */
public class ReconnectSuccessPayload {

    private String roomId;
    private String matchId;
    private SnapshotDTO snapshot;    // current board state
    private long remainingMatchMs;   // remaining match time in ms

    public ReconnectSuccessPayload() {}

    public ReconnectSuccessPayload(String roomId, String matchId,
                                   SnapshotDTO snapshot, long remainingMatchMs) {
        this.roomId = roomId;
        this.matchId = matchId;
        this.snapshot = snapshot;
        this.remainingMatchMs = remainingMatchMs;
    }

    public String getRoomId() { return roomId; }
    public String getMatchId() { return matchId; }
    public SnapshotDTO getSnapshot() { return snapshot; }
    public long getRemainingMatchMs() { return remainingMatchMs; }
}

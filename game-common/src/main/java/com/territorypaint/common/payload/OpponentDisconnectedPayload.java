package com.territorypaint.common.payload;

import com.territorypaint.common.dto.SnapshotDTO;

/**
 * Payload for OPPONENT_DISCONNECTED — sent to the waiting player when their
 * opponent drops connection. Includes the grace period countdown in seconds.
 */
public class OpponentDisconnectedPayload {

    private String roomId;
    private String matchId;
    private String disconnectedUsername;
    private int graceSeconds;        // total grace period (10)
    private long graceDeadlineEpochMs;  // absolute epoch ms when grace expires

    public OpponentDisconnectedPayload() {}

    public OpponentDisconnectedPayload(String roomId, String matchId,
                                       String disconnectedUsername,
                                       int graceSeconds, long graceDeadlineEpochMs) {
        this.roomId = roomId;
        this.matchId = matchId;
        this.disconnectedUsername = disconnectedUsername;
        this.graceSeconds = graceSeconds;
        this.graceDeadlineEpochMs = graceDeadlineEpochMs;
    }

    public String getRoomId() { return roomId; }
    public String getMatchId() { return matchId; }
    public String getDisconnectedUsername() { return disconnectedUsername; }
    public int getGraceSeconds() { return graceSeconds; }
    public long getGraceDeadlineEpochMs() { return graceDeadlineEpochMs; }
}

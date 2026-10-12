package com.territorypaint.common.payload;

import com.territorypaint.common.dto.SnapshotDTO;

/**
 * Match start payload with initial snapshot.
 * Requirements: 13.1, 13.2
 */
public class MatchStartPayload {
    private SnapshotDTO snapshot;
    private String readyDeadline; // ISO-8601 timestamp
    
    public MatchStartPayload() {
    }
    
    public MatchStartPayload(SnapshotDTO snapshot, String readyDeadline) {
        this.snapshot = snapshot;
        this.readyDeadline = readyDeadline;
    }

    public MatchStartPayload(SnapshotDTO snapshot, java.time.Instant readyDeadline) {
        this.snapshot = snapshot;
        this.readyDeadline = readyDeadline != null ? readyDeadline.toString() : null;
    }
    
    public SnapshotDTO getSnapshot() {
        return snapshot;
    }
    
    public void setSnapshot(SnapshotDTO snapshot) {
        this.snapshot = snapshot;
    }
    
    public String getReadyDeadline() {
        return readyDeadline;
    }
    
    public void setReadyDeadline(String readyDeadline) {
        this.readyDeadline = readyDeadline;
    }
}

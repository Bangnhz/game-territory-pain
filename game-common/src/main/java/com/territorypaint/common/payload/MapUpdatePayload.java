package com.territorypaint.common.payload;

import com.territorypaint.common.dto.SnapshotDTO;

/**
 * Complete map update payload.
 * Requirements: 13.1, 13.2, 20.12
 */
public class MapUpdatePayload {
    private SnapshotDTO snapshot;
    private Long actorUserId;
    private long acceptedSeq;

    public MapUpdatePayload() {
    }

    public MapUpdatePayload(SnapshotDTO snapshot) {
        this.snapshot = snapshot;
    }

    public MapUpdatePayload(SnapshotDTO snapshot, Long actorUserId, long acceptedSeq) {
        this.snapshot = snapshot;
        this.actorUserId = actorUserId;
        this.acceptedSeq = acceptedSeq;
    }

    public SnapshotDTO getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(SnapshotDTO snapshot) {
        this.snapshot = snapshot;
    }

    public Long getActorUserId() {
        return actorUserId;
    }

    public void setActorUserId(Long actorUserId) {
        this.actorUserId = actorUserId;
    }

    public long getAcceptedSeq() {
        return acceptedSeq;
    }

    public void setAcceptedSeq(long acceptedSeq) {
        this.acceptedSeq = acceptedSeq;
    }
}

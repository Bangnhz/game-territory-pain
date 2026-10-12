package com.territorypaint.common.payload;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Rematch status notification payload.
 * Requirements: 13.1, 13.2, 20.18
 */
public class RematchStatusPayload {
    private UUID roomId;
    private UUID previousMatchId;
    private List<Long> acceptedUserIds;
    private String expiresAt; // ISO-8601 timestamp

    public RematchStatusPayload() {
    }

    public RematchStatusPayload(UUID roomId, UUID previousMatchId, List<Long> acceptedUserIds, String expiresAt) {
        this.roomId = roomId;
        this.previousMatchId = previousMatchId;
        this.acceptedUserIds = acceptedUserIds;
        this.expiresAt = expiresAt;
    }

    public RematchStatusPayload(String roomId, String previousMatchId, List<Long> acceptedUserIds, Instant expiresAt) {
        this.roomId = UUID.fromString(roomId);
        this.previousMatchId = UUID.fromString(previousMatchId);
        this.acceptedUserIds = acceptedUserIds;
        this.expiresAt = expiresAt != null ? expiresAt.toString() : null;
    }

    public UUID getRoomId() {
        return roomId;
    }

    public void setRoomId(UUID roomId) {
        this.roomId = roomId;
    }

    public UUID getPreviousMatchId() {
        return previousMatchId;
    }

    public void setPreviousMatchId(UUID previousMatchId) {
        this.previousMatchId = previousMatchId;
    }

    public List<Long> getAcceptedUserIds() {
        return acceptedUserIds;
    }

    public void setAcceptedUserIds(List<Long> acceptedUserIds) {
        this.acceptedUserIds = acceptedUserIds;
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(String expiresAt) {
        this.expiresAt = expiresAt;
    }
}

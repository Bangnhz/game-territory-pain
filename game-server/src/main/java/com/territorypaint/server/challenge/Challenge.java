package com.territorypaint.server.challenge;

import com.territorypaint.common.dto.PlayerDTO;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a time-limited match invitation between two players.
 * Requirements: 4.3, 4.4, 4.11
 */
public class Challenge {

    public enum Status {
        PENDING,
        ACCEPTED,
        REJECTED,
        CANCELLED,
        EXPIRED
    }

    private final UUID challengeId;
    private final Long challengerUserId;
    private final Long targetUserId;
    private final Instant createdAt;
    private final Instant expiresAt;
    private volatile Status status;

    public Challenge(Long challengerUserId, Long targetUserId, int durationSeconds) {
        this.challengeId = UUID.randomUUID();
        this.challengerUserId = challengerUserId;
        this.targetUserId = targetUserId;
        this.createdAt = Instant.now();
        this.expiresAt = createdAt.plusSeconds(durationSeconds);
        this.status = Status.PENDING;
    }

    public UUID getChallengeId() {
        return challengeId;
    }

    public Long getChallengerUserId() {
        return challengerUserId;
    }

    public Long getTargetUserId() {
        return targetUserId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}

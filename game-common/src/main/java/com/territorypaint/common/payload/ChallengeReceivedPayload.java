package com.territorypaint.common.payload;

import com.territorypaint.common.dto.PlayerDTO;

import java.time.Instant;
import java.util.UUID;

/**
 * Challenge received notification payload.
 * Requirements: 13.1, 13.2, 20.4
 */
public class ChallengeReceivedPayload {
    private UUID challengeId;
    private PlayerDTO challenger;
    private long challengerUserId;
    private String challengerUsername;
    private String expiresAt; // ISO-8601 timestamp

    public ChallengeReceivedPayload() {
    }

    public ChallengeReceivedPayload(UUID challengeId, PlayerDTO challenger, String expiresAt) {
        this.challengeId = challengeId;
        this.challenger = challenger;
        if (challenger != null) {
            this.challengerUserId = challenger.getUserId();
            this.challengerUsername = challenger.getUsername();
        }
        this.expiresAt = expiresAt;
    }

    public ChallengeReceivedPayload(UUID challengeId, PlayerDTO challenger, Instant expiresAt) {
        this(challengeId, challenger, expiresAt != null ? expiresAt.toString() : null);
    }

    public ChallengeReceivedPayload(UUID challengeId, long challengerUserId, String challengerUsername, String expiresAt) {
        this.challengeId = challengeId;
        this.challengerUserId = challengerUserId;
        this.challengerUsername = challengerUsername;
        this.expiresAt = expiresAt;
    }

    public UUID getChallengeId() {
        return challengeId;
    }

    public void setChallengeId(UUID challengeId) {
        this.challengeId = challengeId;
    }

    public PlayerDTO getChallenger() {
        return challenger;
    }

    public void setChallenger(PlayerDTO challenger) {
        this.challenger = challenger;
        if (challenger != null) {
            this.challengerUserId = challenger.getUserId();
            this.challengerUsername = challenger.getUsername();
        }
    }

    public long getChallengerUserId() {
        return challengerUserId;
    }

    public void setChallengerUserId(long challengerUserId) {
        this.challengerUserId = challengerUserId;
    }

    public String getChallengerUsername() {
        return challengerUsername;
    }

    public void setChallengerUsername(String challengerUsername) {
        this.challengerUsername = challengerUsername;
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(String expiresAt) {
        this.expiresAt = expiresAt;
    }
}

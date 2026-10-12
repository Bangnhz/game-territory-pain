package com.territorypaint.common.payload;

import java.util.UUID;

/**
 * Challenge accept payload.
 * Requirements: 13.1, 13.2, 20.5
 */
public class ChallengeAcceptPayload {
    private UUID challengeId;

    public ChallengeAcceptPayload() {
    }

    public ChallengeAcceptPayload(UUID challengeId) {
        this.challengeId = challengeId;
    }

    public ChallengeAcceptPayload(String challengeId) {
        this.challengeId = UUID.fromString(challengeId);
    }

    public UUID getChallengeId() {
        return challengeId;
    }

    public void setChallengeId(UUID challengeId) {
        this.challengeId = challengeId;
    }
}

package com.territorypaint.common.payload;

import java.util.UUID;

/**
 * Challenge reject payload.
 * Requirements: 13.1, 13.2, 20.5
 */
public class ChallengeRejectPayload {
    private UUID challengeId;

    public ChallengeRejectPayload() {
    }

    public ChallengeRejectPayload(UUID challengeId) {
        this.challengeId = challengeId;
    }

    public ChallengeRejectPayload(String challengeId) {
        this.challengeId = UUID.fromString(challengeId);
    }

    public UUID getChallengeId() {
        return challengeId;
    }

    public void setChallengeId(UUID challengeId) {
        this.challengeId = challengeId;
    }
}

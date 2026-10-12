package com.territorypaint.common.payload;

import java.util.UUID;

/**
 * Challenge cancel payload.
 * Requirements: 13.1, 13.2, 20.5
 */
public class ChallengeCancelPayload {
    private UUID challengeId;

    public ChallengeCancelPayload() {
    }

    public ChallengeCancelPayload(UUID challengeId) {
        this.challengeId = challengeId;
    }

    public ChallengeCancelPayload(String challengeId) {
        this.challengeId = UUID.fromString(challengeId);
    }

    public UUID getChallengeId() {
        return challengeId;
    }

    public void setChallengeId(UUID challengeId) {
        this.challengeId = challengeId;
    }
}

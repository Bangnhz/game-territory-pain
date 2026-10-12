package com.territorypaint.common.payload;

import java.util.UUID;

/**
 * Challenge result payload (accepted, rejected, cancelled, expired).
 * Requirements: 13.1, 13.2, 20.6
 */
public class ChallengeResultPayload {

    public enum ChallengeStatus {
        PENDING,
        ACCEPTED,
        REJECTED,
        CANCELLED,
        EXPIRED
    }

    private UUID challengeId;
    private String status; // PENDING, ACCEPTED, REJECTED, CANCELLED, EXPIRED
    private String reason;
    private String message;

    public ChallengeResultPayload() {
    }

    public ChallengeResultPayload(UUID challengeId, String status, String reason) {
        this.challengeId = challengeId;
        this.status = status;
        this.reason = reason;
        this.message = reason;
    }

    public ChallengeResultPayload(UUID challengeId, ChallengeStatus status, String reason) {
        this.challengeId = challengeId;
        this.status = status != null ? status.name() : null;
        this.reason = reason;
        this.message = reason;
    }

    public UUID getChallengeId() {
        return challengeId;
    }

    public void setChallengeId(UUID challengeId) {
        this.challengeId = challengeId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReason() {
        return reason != null ? reason : message;
    }

    public void setReason(String reason) {
        this.reason = reason;
        this.message = reason;
    }

    public String getMessage() {
        return message != null ? message : reason;
    }

    public void setMessage(String message) {
        this.message = message;
        this.reason = message;
    }
}

package com.territorypaint.common.payload;

/**
 * Challenge request payload.
 * Requirements: 13.1, 13.2
 */
public class ChallengePayload {
    private long targetUserId;
    
    public ChallengePayload() {
    }
    
    public ChallengePayload(long targetUserId) {
        this.targetUserId = targetUserId;
    }
    
    public long getTargetUserId() {
        return targetUserId;
    }
    
    public void setTargetUserId(long targetUserId) {
        this.targetUserId = targetUserId;
    }
}

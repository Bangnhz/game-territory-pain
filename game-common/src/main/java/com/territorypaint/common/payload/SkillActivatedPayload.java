package com.territorypaint.common.payload;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.territorypaint.common.protocol.SkillType;

/**
 * Payload for SKILL_ACTIVATED message.
 * Server confirms skill activation and notifies both players.
 */
public class SkillActivatedPayload {
    
    private final Long userId;
    private final SkillType skillType;
    private final Long durationMs;
    private final Long expiresAtMs;
    
    @JsonCreator
    public SkillActivatedPayload(
            @JsonProperty("userId") Long userId,
            @JsonProperty("skillType") SkillType skillType,
            @JsonProperty("durationMs") Long durationMs,
            @JsonProperty("expiresAtMs") Long expiresAtMs) {
        this.userId = userId;
        this.skillType = skillType;
        this.durationMs = durationMs;
        this.expiresAtMs = expiresAtMs;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public SkillType getSkillType() {
        return skillType;
    }
    
    public Long getDurationMs() {
        return durationMs;
    }
    
    public Long getExpiresAtMs() {
        return expiresAtMs;
    }
}

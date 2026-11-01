package com.territorypaint.common.payload;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.territorypaint.common.protocol.SkillType;

/**
 * Payload for SKILL_REJECTED message.
 * Server rejects skill usage (already used, invalid state, etc.)
 */
public class SkillRejectedPayload {
    
    private final SkillType skillType;
    private final String reason;
    
    @JsonCreator
    public SkillRejectedPayload(
            @JsonProperty("skillType") SkillType skillType,
            @JsonProperty("reason") String reason) {
        this.skillType = skillType;
        this.reason = reason;
    }
    
    public SkillType getSkillType() {
        return skillType;
    }
    
    public String getReason() {
        return reason;
    }
}

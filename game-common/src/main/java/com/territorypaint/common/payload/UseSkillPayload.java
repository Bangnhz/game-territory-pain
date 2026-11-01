package com.territorypaint.common.payload;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.territorypaint.common.protocol.SkillType;

/**
 * Payload for USE_SKILL message.
 * Player uses a skill during match.
 */
public class UseSkillPayload {
    
    private final SkillType skillType;
    
    @JsonCreator
    public UseSkillPayload(@JsonProperty("skillType") SkillType skillType) {
        this.skillType = skillType;
    }
    
    public SkillType getSkillType() {
        return skillType;
    }
}

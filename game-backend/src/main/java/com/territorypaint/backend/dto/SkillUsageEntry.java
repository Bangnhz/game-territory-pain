package com.territorypaint.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;

/**
 * Represents a skill usage during a match.
 */
public class SkillUsageEntry {
    
    @NotNull(message = "User ID is required")
    private Long userId;
    
    @NotBlank(message = "Skill type is required")
    @Pattern(regexp = "^(FREEZE|SHIELD)$", message = "Skill type must be FREEZE or SHIELD")
    private String skillType;
    
    @NotNull(message = "Usage time is required")
    private Instant usedAt;
    
    public SkillUsageEntry() {
    }
    
    public SkillUsageEntry(Long userId, String skillType, Instant usedAt) {
        this.userId = userId;
        this.skillType = skillType;
        this.usedAt = usedAt;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getSkillType() {
        return skillType;
    }
    
    public void setSkillType(String skillType) {
        this.skillType = skillType;
    }
    
    public Instant getUsedAt() {
        return usedAt;
    }
    
    public void setUsedAt(Instant usedAt) {
        this.usedAt = usedAt;
    }
}

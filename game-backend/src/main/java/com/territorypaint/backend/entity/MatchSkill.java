package com.territorypaint.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Match skill entity - stores skills used during matches.
 */
@Entity
@Table(name = "MATCH_SKILLS")
public class MatchSkill {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "match_id", nullable = false, length = 36)
    private String matchId;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "skill_type", nullable = false, length = 20)
    private String skillType;
    
    @Column(name = "used_at", nullable = false)
    private Instant usedAt;
    
    // Constructors
    public MatchSkill() {
    }
    
    public MatchSkill(String matchId, Long userId, String skillType, Instant usedAt) {
        this.matchId = matchId;
        this.userId = userId;
        this.skillType = skillType;
        this.usedAt = usedAt;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getMatchId() {
        return matchId;
    }
    
    public void setMatchId(String matchId) {
        this.matchId = matchId;
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

package com.territorypaint.common.protocol;

/**
 * Types of skills available in the game.
 * Each skill can be used once per match.
 */
public enum SkillType {
    /**
     * FREEZE: Prevents opponent from moving for 5 seconds
     */
    FREEZE,
    
    /**
     * SHIELD: Prevents opponent from capturing player's cells for 5 seconds
     */
    SHIELD
}

package com.territorypaint.common.protocol;

/**
 * Types of power-up items that can appear on the game grid.
 */
public enum PowerUpType {
    /** 🚀 Speed Boost: -33% move cooldown for 4 seconds. */
    SPEED_BOOST,
    /** 🧊 Freeze: opponent cannot move for 2 seconds. */
    FREEZE,
    /** 🛡️ Shield: immune to Freeze + owned cells are write-protected for 5 seconds. */
    SHIELD
}

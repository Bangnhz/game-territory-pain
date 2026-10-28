package com.territorypaint.common.dto;

import com.territorypaint.common.protocol.PowerUpType;

/**
 * Buff state of a single player, included in PlayerSnapshotDTO.
 * All times are epoch-milliseconds; 0 means no active buff.
 */
public class PlayerBuffDTO {

    /** Epoch-ms when Speed Boost expires (0 = not active). */
    private long speedBoostEndMs;

    /** Epoch-ms when Shield expires (0 = not active). */
    private long shieldEndMs;

    /** Epoch-ms when this player's Freeze expires (0 = not frozen). */
    private long frozenEndMs;

    public PlayerBuffDTO() {
    }

    public PlayerBuffDTO(long speedBoostEndMs, long shieldEndMs, long frozenEndMs) {
        this.speedBoostEndMs = speedBoostEndMs;
        this.shieldEndMs = shieldEndMs;
        this.frozenEndMs = frozenEndMs;
    }

    public long getSpeedBoostEndMs() { return speedBoostEndMs; }
    public void setSpeedBoostEndMs(long speedBoostEndMs) { this.speedBoostEndMs = speedBoostEndMs; }

    public long getShieldEndMs() { return shieldEndMs; }
    public void setShieldEndMs(long shieldEndMs) { this.shieldEndMs = shieldEndMs; }

    public long getFrozenEndMs() { return frozenEndMs; }
    public void setFrozenEndMs(long frozenEndMs) { this.frozenEndMs = frozenEndMs; }

    /** @return true if the player is currently speed-boosted at the given time. */
    public boolean isSpeedBoosted(long nowMs) { return speedBoostEndMs > nowMs; }

    /** @return true if the player's shield is currently active. */
    public boolean isShielded(long nowMs) { return shieldEndMs > nowMs; }

    /** @return true if the player is currently frozen. */
    public boolean isFrozen(long nowMs) { return frozenEndMs > nowMs; }
}

package com.territorypaint.common.dto;

import com.territorypaint.common.protocol.PowerUpType;

/**
 * A power-up item currently present on the game grid.
 * Included in SnapshotDTO so clients can render items on every MAP_UPDATE.
 */
public class ActiveItemDTO {

    /** Unique identifier (sequential long from server). */
    private long itemId;

    /** Type of power-up. */
    private PowerUpType type;

    /** Grid X coordinate (0-based). */
    private int x;

    /** Grid Y coordinate (0-based). */
    private int y;

    /** Epoch-ms when the item will auto-despawn if not collected. */
    private long despawnAtMs;

    public ActiveItemDTO() {
    }

    public ActiveItemDTO(long itemId, PowerUpType type, int x, int y, long despawnAtMs) {
        this.itemId = itemId;
        this.type = type;
        this.x = x;
        this.y = y;
        this.despawnAtMs = despawnAtMs;
    }

    public long getItemId() { return itemId; }
    public void setItemId(long itemId) { this.itemId = itemId; }

    public PowerUpType getType() { return type; }
    public void setType(PowerUpType type) { this.type = type; }

    public int getX() { return x; }
    public void setX(int x) { this.x = x; }

    public int getY() { return y; }
    public void setY(int y) { this.y = y; }

    public long getDespawnAtMs() { return despawnAtMs; }
    public void setDespawnAtMs(long despawnAtMs) { this.despawnAtMs = despawnAtMs; }
}

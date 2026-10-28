package com.territorypaint.common.payload;

import com.territorypaint.common.protocol.PowerUpType;

/**
 * Broadcast by the server when a new power-up item spawns on the grid.
 * Server → both players.
 */
public class ItemSpawnPayload {

    private long itemId;
    private PowerUpType type;
    private int x;
    private int y;
    /** Epoch-ms when this item will auto-despawn if not collected. */
    private long despawnAtMs;

    public ItemSpawnPayload() {}

    public ItemSpawnPayload(long itemId, PowerUpType type, int x, int y, long despawnAtMs) {
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

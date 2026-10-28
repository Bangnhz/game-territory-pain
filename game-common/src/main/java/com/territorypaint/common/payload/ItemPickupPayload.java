package com.territorypaint.common.payload;

import com.territorypaint.common.protocol.PowerUpType;

/**
 * Broadcast by the server when a player collects a power-up item.
 * Server → both players.
 */
public class ItemPickupPayload {

    private long itemId;
    private PowerUpType type;
    /** Player slot (1 or 2) who collected the item. */
    private int collectorSlot;
    private long collectorUserId;
    private String collectorUsername;
    /**
     * For FREEZE only: the slot of the player who gets frozen.
     * -1 for other power-up types.
     */
    private int targetSlot;
    /** Epoch-ms when the buff effect expires on the collector (or target for FREEZE). */
    private long effectEndMs;

    public ItemPickupPayload() {}

    public ItemPickupPayload(long itemId, PowerUpType type,
                             int collectorSlot, long collectorUserId, String collectorUsername,
                             int targetSlot, long effectEndMs) {
        this.itemId = itemId;
        this.type = type;
        this.collectorSlot = collectorSlot;
        this.collectorUserId = collectorUserId;
        this.collectorUsername = collectorUsername;
        this.targetSlot = targetSlot;
        this.effectEndMs = effectEndMs;
    }

    public long getItemId() { return itemId; }
    public void setItemId(long itemId) { this.itemId = itemId; }

    public PowerUpType getType() { return type; }
    public void setType(PowerUpType type) { this.type = type; }

    public int getCollectorSlot() { return collectorSlot; }
    public void setCollectorSlot(int collectorSlot) { this.collectorSlot = collectorSlot; }

    public long getCollectorUserId() { return collectorUserId; }
    public void setCollectorUserId(long collectorUserId) { this.collectorUserId = collectorUserId; }

    public String getCollectorUsername() { return collectorUsername; }
    public void setCollectorUsername(String collectorUsername) { this.collectorUsername = collectorUsername; }

    public int getTargetSlot() { return targetSlot; }
    public void setTargetSlot(int targetSlot) { this.targetSlot = targetSlot; }

    public long getEffectEndMs() { return effectEndMs; }
    public void setEffectEndMs(long effectEndMs) { this.effectEndMs = effectEndMs; }
}

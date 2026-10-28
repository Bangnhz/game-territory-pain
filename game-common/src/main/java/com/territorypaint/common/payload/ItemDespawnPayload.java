package com.territorypaint.common.payload;

/**
 * Broadcast by the server when a power-up item expires without being collected.
 * Server → both players.
 */
public class ItemDespawnPayload {

    private long itemId;

    public ItemDespawnPayload() {}

    public ItemDespawnPayload(long itemId) {
        this.itemId = itemId;
    }

    public long getItemId() { return itemId; }
    public void setItemId(long itemId) { this.itemId = itemId; }
}

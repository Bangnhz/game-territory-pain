package com.territorypaint.server.game;

import com.territorypaint.common.protocol.PowerUpType;
import java.util.concurrent.ScheduledFuture;

/**
 * Represents a single power-up item currently active on the game grid (server-side).
 */
public class PowerUpItem {

    private final long itemId;
    private final PowerUpType type;
    private final int x;
    private final int y;
    private final long despawnAtMs;

    /** Handle to the scheduled despawn task; cancelled on pickup. */
    private ScheduledFuture<?> despawnTask;

    public PowerUpItem(long itemId, PowerUpType type, int x, int y, long despawnAtMs) {
        this.itemId = itemId;
        this.type = type;
        this.x = x;
        this.y = y;
        this.despawnAtMs = despawnAtMs;
    }

    public long getItemId()       { return itemId; }
    public PowerUpType getType()  { return type; }
    public int getX()             { return x; }
    public int getY()             { return y; }
    public long getDespawnAtMs()  { return despawnAtMs; }

    public ScheduledFuture<?> getDespawnTask()                    { return despawnTask; }
    public void setDespawnTask(ScheduledFuture<?> despawnTask)    { this.despawnTask = despawnTask; }
}

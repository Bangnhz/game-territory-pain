package com.territorypaint.server.game;

import com.territorypaint.common.dto.*;
import com.territorypaint.common.dto.ActiveItemDTO;
import com.territorypaint.common.dto.PlayerBuffDTO;
import com.territorypaint.common.dto.PlayerSnapshotDTO;
import com.territorypaint.common.dto.PositionDTO;
import com.territorypaint.common.dto.SnapshotDTO;

import java.util.*;

/**
 * Authoritative match state maintained by the Game Server.
 * Requirements: 5.4, 5.5, 5.6, 5.7, 5.8, 6.6, 6.15, 6.16, 6.17
 * Extended: Power-up buff state and grid protection.
 */
public class GameState {

    private final String roomId;
    private final String matchId;
    private final int[][] grid = new int[20][20]; // 20x20 grid, grid[y][x]

    private final Long user1Id;
    private final String user1Username;
    private PositionDTO pos1;

    private final Long user2Id;
    private final String user2Username;
    private PositionDTO pos2;

    private final int[] territoryScores = new int[]{1, 1}; // Requirement 5.7: initial scores = 1
    private long stateVersion = 0; // Requirement 6.15

    // Per-player sequence tracking (Requirement 6.16)
    private long lastSeq1 = -1;
    private long lastSeq2 = -1;

    // Per-player 100ms cooldown tracking (Requirement 6.6)
    private long lastMoveTime1 = 0;
    private long lastMoveTime2 = 0;

    // ── Power-up Buff State ────────────────────────────────────────────────
    /** Epoch-ms when player 1's Speed Boost expires (0 = inactive). */
    private long speedBoostEnd1 = 0;
    /** Epoch-ms when player 2's Speed Boost expires (0 = inactive). */
    private long speedBoostEnd2 = 0;
    /** Epoch-ms when player 1's Shield expires (0 = inactive). */
    private long shieldEnd1 = 0;
    /** Epoch-ms when player 2's Shield expires (0 = inactive). */
    private long shieldEnd2 = 0;
    /** Epoch-ms when player 1 is frozen until (0 = not frozen). */
    private long frozenEnd1 = 0;
    /** Epoch-ms when player 2 is frozen until (0 = not frozen). */
    private long frozenEnd2 = 0;

    /**
     * Grid cells that are currently Shield-protected.
     * Key = y*20+x, Value = owning slot (1 or 2).
     * While a cell is in this map, opponents cannot overwrite it.
     */
    private final Map<Integer, Integer> protectedCells = new HashMap<>();
    // ──────────────────────────────────────────────────────────────────────

    public GameState(String roomId, String matchId, Long user1Id, String user1Username, Long user2Id, String user2Username) {
        this.roomId = roomId;
        this.matchId = matchId;
        this.user1Id = user1Id;
        this.user1Username = user1Username;
        this.user2Id = user2Id;
        this.user2Username = user2Username;

        // Player 1 at (0,0) (Requirement 5.6, 5.7)
        this.pos1 = new PositionDTO(0, 0);
        this.grid[0][0] = 1;

        // Player 2 at (19,19) (Requirement 5.6, 5.7)
        this.pos2 = new PositionDTO(19, 19);
        this.grid[19][19] = 2;
    }

    public String getRoomId()  { return roomId; }
    public String getMatchId() { return matchId; }

    public int[][] getGridCopy() {
        int[][] copy = new int[20][20];
        for (int i = 0; i < 20; i++) {
            System.arraycopy(grid[i], 0, copy[i], 0, 20);
        }
        return copy;
    }

    public int[][] getGrid() { return grid; }

    public PositionDTO getPlayerPosition(int slot) {
        return slot == 1 ? pos1 : pos2;
    }

    public void setPlayerPosition(int slot, PositionDTO pos) {
        if (slot == 1) this.pos1 = pos;
        else           this.pos2 = pos;
    }

    public int[] getTerritoryScores() { return territoryScores; }

    public long getStateVersion()    { return stateVersion; }
    public void incrementStateVersion() { this.stateVersion++; }

    public boolean checkAndRecordSeq(int slot, long seq) {
        if (slot == 1) {
            if (seq <= lastSeq1) return false;
            lastSeq1 = seq;
            return true;
        } else {
            if (seq <= lastSeq2) return false;
            lastSeq2 = seq;
            return true;
        }
    }

    /**
     * Checks the per-player movement cooldown.
     * Speed Boost reduces the effective cooldown to 67 ms (≈ 33% less than 100 ms).
     */
    public boolean checkCooldown(int slot, long nowMs) {
        long cooldownMs = isSpeedBoosted(slot, nowMs) ? 67L : 100L; // Requirement 6.6 + SpeedBoost
        long last = (slot == 1) ? lastMoveTime1 : lastMoveTime2;
        if (nowMs - last < cooldownMs) return false;
        if (slot == 1) lastMoveTime1 = nowMs;
        else           lastMoveTime2 = nowMs;
        return true;
    }

    // ── Buff accessors ─────────────────────────────────────────────────────

    public boolean isFrozen(int slot, long nowMs) {
        return slot == 1 ? frozenEnd1 > nowMs : frozenEnd2 > nowMs;
    }

    public boolean isSpeedBoosted(int slot, long nowMs) {
        return slot == 1 ? speedBoostEnd1 > nowMs : speedBoostEnd2 > nowMs;
    }

    public boolean isShielded(int slot, long nowMs) {
        return slot == 1 ? shieldEnd1 > nowMs : shieldEnd2 > nowMs;
    }

    public void applySpeedBoost(int slot, long endMs) {
        if (slot == 1) speedBoostEnd1 = endMs; else speedBoostEnd2 = endMs;
    }

    public void applyShield(int slot, long endMs) {
        if (slot == 1) shieldEnd1 = endMs; else shieldEnd2 = endMs;
    }

    /**
     * Apply Freeze to a player unless they are currently Shielded.
     * @return true if freeze was applied, false if blocked by Shield.
     */
    public boolean applyFreeze(int slot, long endMs, long nowMs) {
        if (isShielded(slot, nowMs)) return false;
        if (slot == 1) frozenEnd1 = endMs; else frozenEnd2 = endMs;
        return true;
    }

    // ── Protected cells ────────────────────────────────────────────────────

    /** Mark a grid cell as Shield-protected by the given slot. */
    public void protectCell(int x, int y, int ownerSlot) {
        protectedCells.put(y * 20 + x, ownerSlot);
    }

    /** @return the slot that has this cell protected, or 0 if unprotected. */
    public int getProtectedOwner(int x, int y) {
        return protectedCells.getOrDefault(y * 20 + x, 0);
    }

    /** Remove shield protection from all cells owned by the given slot (called when shield expires). */
    public void clearProtectedCells(int slot) {
        protectedCells.values().removeIf(v -> v == slot);
    }

    // ── Snapshot ───────────────────────────────────────────────────────────

    public SnapshotDTO toSnapshotDTO(long remainingMs) {
        return toSnapshotDTO(remainingMs, Collections.emptyList());
    }

    public SnapshotDTO toSnapshotDTO(long remainingMs, List<ActiveItemDTO> activeItems) {
        long nowMs = System.currentTimeMillis();
        PlayerBuffDTO buff1 = new PlayerBuffDTO(speedBoostEnd1, shieldEnd1, frozenEnd1);
        PlayerBuffDTO buff2 = new PlayerBuffDTO(speedBoostEnd2, shieldEnd2, frozenEnd2);

        PlayerSnapshotDTO p1 = new PlayerSnapshotDTO(
                user1Id, user1Username, 1, "#2672D6", pos1, territoryScores[0], buff1);
        PlayerSnapshotDTO p2 = new PlayerSnapshotDTO(
                user2Id, user2Username, 2, "#F28C28", pos2, territoryScores[1], buff2);

        return new SnapshotDTO(roomId, matchId, stateVersion, getGridCopy(),
                List.of(p1, p2), remainingMs, activeItems);
    }

    // ── Getters for raw buff end-times (used by GameRoom) ─────────────────
    public long getSpeedBoostEnd(int slot) { return slot == 1 ? speedBoostEnd1 : speedBoostEnd2; }
    public long getShieldEnd(int slot)     { return slot == 1 ? shieldEnd1 : shieldEnd2; }
    public long getFrozenEnd(int slot)     { return slot == 1 ? frozenEnd1 : frozenEnd2; }
}

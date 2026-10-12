package com.territorypaint.server.game;

import com.territorypaint.common.dto.PlayerSnapshotDTO;
import com.territorypaint.common.dto.PositionDTO;
import com.territorypaint.common.dto.SnapshotDTO;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/**
 * Authoritative match state maintained by the Game Server.
 * Requirements: 5.4, 5.5, 5.6, 5.7, 5.8, 6.6, 6.15, 6.16, 6.17
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

    public String getRoomId() {
        return roomId;
    }

    public String getMatchId() {
        return matchId;
    }

    public int[][] getGridCopy() {
        int[][] copy = new int[20][20];
        for (int i = 0; i < 20; i++) {
            System.arraycopy(grid[i], 0, copy[i], 0, 20);
        }
        return copy;
    }

    public int[][] getGrid() {
        return grid;
    }

    public PositionDTO getPlayerPosition(int slot) {
        return slot == 1 ? pos1 : pos2;
    }

    public void setPlayerPosition(int slot, PositionDTO pos) {
        if (slot == 1) {
            this.pos1 = pos;
        } else {
            this.pos2 = pos;
        }
    }

    public int[] getTerritoryScores() {
        return territoryScores;
    }

    public long getStateVersion() {
        return stateVersion;
    }

    public void incrementStateVersion() {
        this.stateVersion++;
    }

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

    public boolean checkCooldown(int slot, long nowMs) {
        long last = (slot == 1) ? lastMoveTime1 : lastMoveTime2;
        if (nowMs - last < 100) { // Requirement 6.6: 100ms cooldown
            return false;
        }
        if (slot == 1) {
            lastMoveTime1 = nowMs;
        } else {
            lastMoveTime2 = nowMs;
        }
        return true;
    }

    public SnapshotDTO toSnapshotDTO(long remainingMs) {
        PlayerSnapshotDTO p1 = new PlayerSnapshotDTO(user1Id, user1Username, 1, "#2672D6", pos1, territoryScores[0]);
        PlayerSnapshotDTO p2 = new PlayerSnapshotDTO(user2Id, user2Username, 2, "#F28C28", pos2, territoryScores[1]);
        return new SnapshotDTO(roomId, matchId, stateVersion, getGridCopy(), List.of(p1, p2), remainingMs);
    }
}

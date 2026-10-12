package com.territorypaint.common.dto;

import java.util.List;
import java.util.UUID;

/**
 * Complete game state snapshot.
 * Requirements: 13.1, 13.2
 */
public class SnapshotDTO {
    private UUID roomId;
    private UUID matchId;
    private long stateVersion; // Server authoritative version
    private int[][] grid; // [y][x], 0=unowned, 1=player1, 2=player2
    private List<PlayerSnapshotDTO> players; // Ordered by slot
    private long remainingMs;
    
    public SnapshotDTO() {
    }
    
    public SnapshotDTO(UUID roomId, UUID matchId, long stateVersion, int[][] grid, 
                      List<PlayerSnapshotDTO> players, long remainingMs) {
        this.roomId = roomId;
        this.matchId = matchId;
        this.stateVersion = stateVersion;
        this.grid = grid;
        this.players = players;
        this.remainingMs = remainingMs;
    }

    public SnapshotDTO(String roomId, String matchId, long stateVersion, int[][] grid, 
                      List<PlayerSnapshotDTO> players, long remainingMs) {
        this(UUID.fromString(roomId), UUID.fromString(matchId), stateVersion, grid, players, remainingMs);
    }
    
    public UUID getRoomId() {
        return roomId;
    }
    
    public void setRoomId(UUID roomId) {
        this.roomId = roomId;
    }
    
    public UUID getMatchId() {
        return matchId;
    }
    
    public void setMatchId(UUID matchId) {
        this.matchId = matchId;
    }
    
    public long getStateVersion() {
        return stateVersion;
    }
    
    public void setStateVersion(long stateVersion) {
        this.stateVersion = stateVersion;
    }
    
    public int[][] getGrid() {
        return grid;
    }
    
    public void setGrid(int[][] grid) {
        this.grid = grid;
    }
    
    public List<PlayerSnapshotDTO> getPlayers() {
        return players;
    }
    
    public void setPlayers(List<PlayerSnapshotDTO> players) {
        this.players = players;
    }
    
    public long getRemainingMs() {
        return remainingMs;
    }
    
    public void setRemainingMs(long remainingMs) {
        this.remainingMs = remainingMs;
    }
}

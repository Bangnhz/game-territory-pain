package com.territorypaint.common.payload;

import com.territorypaint.common.dto.PositionDTO;
import java.util.List;
import java.util.UUID;

/**
 * Incremental cell update payload.
 * Requirements: 13.1, 13.2
 */
public class CellUpdatePayload {
    private UUID matchId;
    private long stateVersion;
    private List<CellChange> changes;
    private List<PlayerPosition> playerPositions;
    
    public CellUpdatePayload() {
    }
    
    public CellUpdatePayload(UUID matchId, long stateVersion, 
                           List<CellChange> changes, List<PlayerPosition> playerPositions) {
        this.matchId = matchId;
        this.stateVersion = stateVersion;
        this.changes = changes;
        this.playerPositions = playerPositions;
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
    
    public List<CellChange> getChanges() {
        return changes;
    }
    
    public void setChanges(List<CellChange> changes) {
        this.changes = changes;
    }
    
    public List<PlayerPosition> getPlayerPositions() {
        return playerPositions;
    }
    
    public void setPlayerPositions(List<PlayerPosition> playerPositions) {
        this.playerPositions = playerPositions;
    }
    
    public static class CellChange {
        private int x;
        private int y;
        private int owner; // 0=unowned, 1=player1, 2=player2
        
        public CellChange() {
        }
        
        public CellChange(int x, int y, int owner) {
            this.x = x;
            this.y = y;
            this.owner = owner;
        }
        
        public int getX() {
            return x;
        }
        
        public void setX(int x) {
            this.x = x;
        }
        
        public int getY() {
            return y;
        }
        
        public void setY(int y) {
            this.y = y;
        }
        
        public int getOwner() {
            return owner;
        }
        
        public void setOwner(int owner) {
            this.owner = owner;
        }
    }
    
    public static class PlayerPosition {
        private int slot;
        private PositionDTO position;
        private int cellsOwned;
        
        public PlayerPosition() {
        }
        
        public PlayerPosition(int slot, PositionDTO position, int cellsOwned) {
            this.slot = slot;
            this.position = position;
            this.cellsOwned = cellsOwned;
        }
        
        public int getSlot() {
            return slot;
        }
        
        public void setSlot(int slot) {
            this.slot = slot;
        }
        
        public PositionDTO getPosition() {
            return position;
        }
        
        public void setPosition(PositionDTO position) {
            this.position = position;
        }
        
        public int getCellsOwned() {
            return cellsOwned;
        }
        
        public void setCellsOwned(int cellsOwned) {
            this.cellsOwned = cellsOwned;
        }
    }
}

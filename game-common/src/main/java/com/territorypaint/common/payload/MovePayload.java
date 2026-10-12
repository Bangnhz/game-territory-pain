package com.territorypaint.common.payload;

import com.territorypaint.common.protocol.Direction;
import java.util.UUID;

/**
 * Player movement payload.
 * Requirements: 13.1, 13.2
 */
public class MovePayload {
    private UUID matchId;
    private long seq; // Client sequence number
    private Direction direction;
    
    public MovePayload() {
    }
    
    public MovePayload(UUID matchId, long seq, Direction direction) {
        this.matchId = matchId;
        this.seq = seq;
        this.direction = direction;
    }

    public MovePayload(String matchId, long seq, Direction direction) {
        this.matchId = UUID.fromString(matchId);
        this.seq = seq;
        this.direction = direction;
    }
    
    public UUID getMatchId() {
        return matchId;
    }
    
    public void setMatchId(UUID matchId) {
        this.matchId = matchId;
    }
    
    public long getSeq() {
        return seq;
    }
    
    public void setSeq(long seq) {
        this.seq = seq;
    }
    
    public Direction getDirection() {
        return direction;
    }
    
    public void setDirection(Direction direction) {
        this.direction = direction;
    }
}

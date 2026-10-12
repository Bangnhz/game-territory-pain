package com.territorypaint.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Match entity.
 * Requirements: 12.2, 12.3, 12.5, 12.6, 12.7, 12.9
 */
@Entity
@Table(name = "MATCHES")
public class Match {
    
    @Id
    @Column(name = "match_id", length = 36)
    private String matchId;
    
    @Column(name = "room_id", nullable = false, length = 36)
    private String roomId;
    
    @Column(name = "started_at", nullable = false)
    private Instant startedAt;
    
    @Column(name = "ended_at", nullable = false)
    private Instant endedAt;
    
    @Column(name = "end_reason", nullable = false, length = 16)
    private String endReason;
    
    @Column(name = "winner_user_id")
    private Long winnerUserId;
    
    @Column(name = "forfeited_user_id")
    private Long forfeitedUserId;
    
    @Column(name = "payload_hash", nullable = false, length = 64)
    private String payloadHash;
    
    // Constructors
    public Match() {
    }
    
    public Match(String matchId, String roomId, Instant startedAt, Instant endedAt,
                String endReason, Long winnerUserId, Long forfeitedUserId, String payloadHash) {
        this.matchId = matchId;
        this.roomId = roomId;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.endReason = endReason;
        this.winnerUserId = winnerUserId;
        this.forfeitedUserId = forfeitedUserId;
        this.payloadHash = payloadHash;
    }
    
    // Getters and Setters
    public String getMatchId() {
        return matchId;
    }
    
    public void setMatchId(String matchId) {
        this.matchId = matchId;
    }
    
    public String getRoomId() {
        return roomId;
    }
    
    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }
    
    public Instant getStartedAt() {
        return startedAt;
    }
    
    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }
    
    public Instant getEndedAt() {
        return endedAt;
    }
    
    public void setEndedAt(Instant endedAt) {
        this.endedAt = endedAt;
    }
    
    public String getEndReason() {
        return endReason;
    }
    
    public void setEndReason(String endReason) {
        this.endReason = endReason;
    }
    
    public Long getWinnerUserId() {
        return winnerUserId;
    }
    
    public void setWinnerUserId(Long winnerUserId) {
        this.winnerUserId = winnerUserId;
    }
    
    public Long getForfeitedUserId() {
        return forfeitedUserId;
    }
    
    public void setForfeitedUserId(Long forfeitedUserId) {
        this.forfeitedUserId = forfeitedUserId;
    }
    
    public String getPayloadHash() {
        return payloadHash;
    }
    
    public void setPayloadHash(String payloadHash) {
        this.payloadHash = payloadHash;
    }
}

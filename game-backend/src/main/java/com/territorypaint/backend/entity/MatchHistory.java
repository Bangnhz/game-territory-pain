package com.territorypaint.backend.entity;

import jakarta.persistence.*;

/**
 * Match history entity.
 * Requirements: 12.2, 12.3, 12.5, 12.6, 12.7, 12.9
 */
@Entity
@Table(name = "MATCH_HISTORY")
public class MatchHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "match_id", nullable = false, length = 36)
    private String matchId;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "opponent_id", nullable = false)
    private Long opponentId;
    
    @Column(name = "player_slot", nullable = false)
    private Integer playerSlot;
    
    @Column(name = "cells_owned", nullable = false)
    private Integer cellsOwned;
    
    @Column(nullable = false, length = 8)
    private String outcome;
    
    @Column(name = "rank_delta", nullable = false)
    private Integer rankDelta;
    
    // Constructors
    public MatchHistory() {
    }
    
    public MatchHistory(String matchId, Long userId, Long opponentId, Integer playerSlot,
                       Integer cellsOwned, String outcome, Integer rankDelta) {
        this.matchId = matchId;
        this.userId = userId;
        this.opponentId = opponentId;
        this.playerSlot = playerSlot;
        this.cellsOwned = cellsOwned;
        this.outcome = outcome;
        this.rankDelta = rankDelta;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getMatchId() {
        return matchId;
    }
    
    public void setMatchId(String matchId) {
        this.matchId = matchId;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public Long getOpponentId() {
        return opponentId;
    }
    
    public void setOpponentId(Long opponentId) {
        this.opponentId = opponentId;
    }
    
    public Integer getPlayerSlot() {
        return playerSlot;
    }
    
    public void setPlayerSlot(Integer playerSlot) {
        this.playerSlot = playerSlot;
    }
    
    public Integer getCellsOwned() {
        return cellsOwned;
    }
    
    public void setCellsOwned(Integer cellsOwned) {
        this.cellsOwned = cellsOwned;
    }
    
    public String getOutcome() {
        return outcome;
    }
    
    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }
    
    public Integer getRankDelta() {
        return rankDelta;
    }
    
    public void setRankDelta(Integer rankDelta) {
        this.rankDelta = rankDelta;
    }
}

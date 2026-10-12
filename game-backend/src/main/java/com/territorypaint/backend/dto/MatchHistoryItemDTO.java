package com.territorypaint.backend.dto;

import java.time.Instant;

/**
 * Match history entry DTO for GET /api/matches/history.
 * Requirements: 11.5, 11.6
 */
public class MatchHistoryItemDTO {

    private String matchId;
    private Instant playedAt;
    private Long opponentUserId;
    private String opponentUsername;
    private Integer myCells;
    private Integer opponentCells;
    private String outcome;
    private Integer rankDelta;
    private String endReason;

    public MatchHistoryItemDTO() {
    }

    public MatchHistoryItemDTO(String matchId, Instant playedAt, Long opponentUserId,
                               String opponentUsername, Integer myCells, Integer opponentCells,
                               String outcome, Integer rankDelta, String endReason) {
        this.matchId = matchId;
        this.playedAt = playedAt;
        this.opponentUserId = opponentUserId;
        this.opponentUsername = opponentUsername;
        this.myCells = myCells;
        this.opponentCells = opponentCells;
        this.outcome = outcome;
        this.rankDelta = rankDelta;
        this.endReason = endReason;
    }

    public String getMatchId() {
        return matchId;
    }

    public void setMatchId(String matchId) {
        this.matchId = matchId;
    }

    public Instant getPlayedAt() {
        return playedAt;
    }

    public void setPlayedAt(Instant playedAt) {
        this.playedAt = playedAt;
    }

    public Long getOpponentUserId() {
        return opponentUserId;
    }

    public void setOpponentUserId(Long opponentUserId) {
        this.opponentUserId = opponentUserId;
    }

    public String getOpponentUsername() {
        return opponentUsername;
    }

    public void setOpponentUsername(String opponentUsername) {
        this.opponentUsername = opponentUsername;
    }

    public Integer getMyCells() {
        return myCells;
    }

    public void setMyCells(Integer myCells) {
        this.myCells = myCells;
    }

    public Integer getOpponentCells() {
        return opponentCells;
    }

    public void setOpponentCells(Integer opponentCells) {
        this.opponentCells = opponentCells;
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

    public String getEndReason() {
        return endReason;
    }

    public void setEndReason(String endReason) {
        this.endReason = endReason;
    }
}

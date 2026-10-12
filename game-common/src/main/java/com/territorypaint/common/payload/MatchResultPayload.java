package com.territorypaint.common.payload;

import com.territorypaint.common.dto.PlayerResultDTO;
import com.territorypaint.common.protocol.MatchEndReason;
import com.territorypaint.common.protocol.PersistenceStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Match result payload.
 * Requirements: 13.1, 13.2, 20.15, 20.16
 */
public class MatchResultPayload {
    private UUID roomId;
    private UUID matchId;
    private Long winnerUserId; // Null for draw
    private MatchEndReason endReason;
    private List<PlayerResultDTO> playerResults;
    private PersistenceStatus persistenceStatus;
    private String rematchDeadline; // ISO-8601, null if no rematch available

    public MatchResultPayload() {
    }

    public MatchResultPayload(UUID roomId, UUID matchId, Long winnerUserId,
                              List<PlayerResultDTO> playerResults, MatchEndReason endReason,
                              PersistenceStatus persistenceStatus, String rematchDeadline) {
        this.roomId = roomId;
        this.matchId = matchId;
        this.winnerUserId = winnerUserId;
        this.playerResults = playerResults;
        this.endReason = endReason;
        this.persistenceStatus = persistenceStatus;
        this.rematchDeadline = rematchDeadline;
    }

    public MatchResultPayload(UUID roomId, UUID matchId, Long winnerUserId, MatchEndReason endReason,
                              List<PlayerResultDTO> playerResults, PersistenceStatus persistenceStatus,
                              String rematchDeadline) {
        this.roomId = roomId;
        this.matchId = matchId;
        this.winnerUserId = winnerUserId;
        this.endReason = endReason;
        this.playerResults = playerResults;
        this.persistenceStatus = persistenceStatus;
        this.rematchDeadline = rematchDeadline;
    }

    public MatchResultPayload(String roomId, String matchId, Long winnerUserId, MatchEndReason endReason,
                              List<PlayerResultDTO> playerResults, PersistenceStatus persistenceStatus,
                              Instant rematchDeadline) {
        this.roomId = UUID.fromString(roomId);
        this.matchId = UUID.fromString(matchId);
        this.winnerUserId = winnerUserId;
        this.endReason = endReason;
        this.playerResults = playerResults;
        this.persistenceStatus = persistenceStatus;
        this.rematchDeadline = rematchDeadline != null ? rematchDeadline.toString() : null;
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

    public Long getWinnerUserId() {
        return winnerUserId;
    }

    public void setWinnerUserId(Long winnerUserId) {
        this.winnerUserId = winnerUserId;
    }

    public MatchEndReason getEndReason() {
        return endReason;
    }

    public MatchEndReason getReason() {
        return endReason;
    }

    public void setEndReason(MatchEndReason endReason) {
        this.endReason = endReason;
    }

    public void setReason(MatchEndReason reason) {
        this.endReason = reason;
    }

    public List<PlayerResultDTO> getPlayerResults() {
        return playerResults;
    }

    public void setPlayerResults(List<PlayerResultDTO> playerResults) {
        this.playerResults = playerResults;
    }

    public PersistenceStatus getPersistenceStatus() {
        return persistenceStatus;
    }

    public PersistenceStatus getPersistence() {
        return persistenceStatus;
    }

    public void setPersistenceStatus(PersistenceStatus persistenceStatus) {
        this.persistenceStatus = persistenceStatus;
    }

    public void setPersistence(PersistenceStatus persistence) {
        this.persistenceStatus = persistence;
    }

    public String getRematchDeadline() {
        return rematchDeadline;
    }

    public String getRematchDeadlineAt() {
        return rematchDeadline;
    }

    public void setRematchDeadline(String rematchDeadline) {
        this.rematchDeadline = rematchDeadline;
    }

    public void setRematchDeadlineAt(String rematchDeadlineAt) {
        this.rematchDeadline = rematchDeadlineAt;
    }
}

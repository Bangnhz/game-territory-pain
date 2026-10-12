package com.territorypaint.server.session;

import com.territorypaint.common.dto.PlayerDTO;
import com.territorypaint.common.protocol.PlayerStatus;
import com.territorypaint.server.net.ClientHandler;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents an authenticated client session on the Game Server.
 * Requirements: 2.5, 2.8, 3.4, 3.5
 */
public class Session {

    private final String sessionId;
    private final Long userId;
    private final String username;
    private Long rankingScore;
    private final Instant tokenExpiry;
    private final ClientHandler clientHandler;
    private volatile PlayerStatus status;
    private volatile String currentRoomId;
    private volatile UUID pendingChallengeId;

    public Session(Long userId, String username, Long rankingScore, Instant tokenExpiry, ClientHandler clientHandler) {
        this.sessionId = UUID.randomUUID().toString();
        this.userId = userId;
        this.username = username;
        this.rankingScore = rankingScore;
        this.tokenExpiry = tokenExpiry;
        this.clientHandler = clientHandler;
        this.status = PlayerStatus.IDLE;
    }

    public String getSessionId() {
        return sessionId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public Long getRankingScore() {
        return rankingScore;
    }

    public void setRankingScore(Long rankingScore) {
        this.rankingScore = rankingScore;
    }

    public Instant getTokenExpiry() {
        return tokenExpiry;
    }

    public ClientHandler getClientHandler() {
        return clientHandler;
    }

    public PlayerStatus getStatus() {
        return status;
    }

    public void setStatus(PlayerStatus status) {
        this.status = status;
    }

    public String getCurrentRoomId() {
        return currentRoomId;
    }

    public void setCurrentRoomId(String currentRoomId) {
        this.currentRoomId = currentRoomId;
    }

    public UUID getPendingChallengeId() {
        return pendingChallengeId;
    }

    public void setPendingChallengeId(UUID pendingChallengeId) {
        this.pendingChallengeId = pendingChallengeId;
    }

    public boolean canChallenge() {
        return status == PlayerStatus.IDLE && pendingChallengeId == null;
    }

    public PlayerDTO toPlayerDTO() {
        return new PlayerDTO(userId, username, rankingScore, status, canChallenge());
    }
}

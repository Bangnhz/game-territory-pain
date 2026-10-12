package com.territorypaint.client.state;

import com.territorypaint.common.dto.PlayerDTO;
import com.territorypaint.common.dto.SnapshotDTO;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Shared client application state.
 */
public class ClientState {

    private static final ClientState instance = new ClientState();

    private Long userId;
    private String username;
    private Long rankingScore = 1500L;
    private String jwtToken;

    private List<PlayerDTO> onlinePlayers = new CopyOnWriteArrayList<>();

    private String currentRoomId;
    private String currentMatchId;
    private int playerSlot = 1; // 1 or 2
    private final AtomicLong moveSequence = new AtomicLong(0);

    private SnapshotDTO currentSnapshot;
    private String matchWinnerId;

    private ClientState() {}

    public static ClientState getInstance() {
        return instance;
    }

    public void resetMatch() {
        this.currentMatchId = null;
        this.moveSequence.set(0);
        this.currentSnapshot = null;
        this.matchWinnerId = null;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getRankingScore() {
        return rankingScore;
    }

    public void setRankingScore(Long rankingScore) {
        this.rankingScore = rankingScore;
    }

    public String getJwtToken() {
        return jwtToken;
    }

    public void setJwtToken(String jwtToken) {
        this.jwtToken = jwtToken;
    }

    public List<PlayerDTO> getOnlinePlayers() {
        return onlinePlayers;
    }

    public void setOnlinePlayers(List<PlayerDTO> onlinePlayers) {
        this.onlinePlayers = new CopyOnWriteArrayList<>(onlinePlayers);
    }

    public String getCurrentRoomId() {
        return currentRoomId;
    }

    public void setCurrentRoomId(String currentRoomId) {
        this.currentRoomId = currentRoomId;
    }

    public String getCurrentMatchId() {
        return currentMatchId;
    }

    public void setCurrentMatchId(String currentMatchId) {
        this.currentMatchId = currentMatchId;
    }

    public int getPlayerSlot() {
        return playerSlot;
    }

    public void setPlayerSlot(int playerSlot) {
        this.playerSlot = playerSlot;
    }

    public long nextMoveSeq() {
        return moveSequence.incrementAndGet();
    }

    public SnapshotDTO getCurrentSnapshot() {
        return currentSnapshot;
    }

    public void setCurrentSnapshot(SnapshotDTO currentSnapshot) {
        this.currentSnapshot = currentSnapshot;
    }

    public String getMatchWinnerId() {
        return matchWinnerId;
    }

    public void setMatchWinnerId(String matchWinnerId) {
        this.matchWinnerId = matchWinnerId;
    }
}

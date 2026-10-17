package com.territorypaint.common.payload;

import java.util.UUID;

/**
 * Payload broadcast when a player clicks READY in match preparation.
 */
public class ReadyStatusPayload {
    private String roomId;
    private String matchId;
    private Long userId;
    private int slot;
    private int readyCount;
    private boolean allReady;
    private boolean ready1;
    private boolean ready2;

    public ReadyStatusPayload() {
    }

    public ReadyStatusPayload(String roomId, String matchId, Long userId, int slot, int readyCount, boolean allReady, boolean ready1, boolean ready2) {
        this.roomId = roomId;
        this.matchId = matchId;
        this.userId = userId;
        this.slot = slot;
        this.readyCount = readyCount;
        this.allReady = allReady;
        this.ready1 = ready1;
        this.ready2 = ready2;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
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

    public int getSlot() {
        return slot;
    }

    public void setSlot(int slot) {
        this.slot = slot;
    }

    public int getReadyCount() {
        return readyCount;
    }

    public void setReadyCount(int readyCount) {
        this.readyCount = readyCount;
    }

    public boolean isAllReady() {
        return allReady;
    }

    public void setAllReady(boolean allReady) {
        this.allReady = allReady;
    }

    public boolean isReady1() {
        return ready1;
    }

    public void setReady1(boolean ready1) {
        this.ready1 = ready1;
    }

    public boolean isReady2() {
        return ready2;
    }

    public void setReady2(boolean ready2) {
        this.ready2 = ready2;
    }
}

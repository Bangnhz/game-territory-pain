package com.territorypaint.common.payload;

import com.territorypaint.common.dto.PlayerDTO;

import java.time.Instant;

/**
 * Login result payload.
 * Requirements: 13.1, 13.2, 20.1
 */
public class LoginResultPayload {
    private boolean success;
    private String message;
    private long userId;
    private String username;
    private PlayerDTO player;
    private String expiresAt;

    public LoginResultPayload() {
    }

    public LoginResultPayload(boolean success, String message, long userId, String username) {
        this.success = success;
        this.message = message;
        this.userId = userId;
        this.username = username;
    }

    public LoginResultPayload(boolean success, PlayerDTO player, String expiresAt) {
        this.success = success;
        this.player = player;
        if (player != null) {
            this.userId = player.getUserId();
            this.username = player.getUsername();
        }
        this.expiresAt = expiresAt;
    }

    public LoginResultPayload(boolean success, PlayerDTO player, Instant expiresAt) {
        this(success, player, expiresAt != null ? expiresAt.toString() : null);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public PlayerDTO getPlayer() {
        return player;
    }

    public void setPlayer(PlayerDTO player) {
        this.player = player;
        if (player != null) {
            this.userId = player.getUserId();
            this.username = player.getUsername();
        }
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(String expiresAt) {
        this.expiresAt = expiresAt;
    }
}

package com.territorypaint.backend.dto;

import com.territorypaint.backend.validation.ValidMatchRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

/**
 * Request to save a match result.
 * Requirements: 9.5, 9.6, 9.7
 */
@ValidMatchRequest
public class SaveMatchRequest {
    
    @NotBlank(message = "Match ID is required")
    @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
             message = "Match ID must be a valid UUID")
    private String matchId;
    
    @NotBlank(message = "Room ID is required")
    @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
             message = "Room ID must be a valid UUID")
    private String roomId;
    
    @NotNull(message = "Start time is required")
    private Instant startedAt;
    
    @NotNull(message = "End time is required")
    private Instant endedAt;
    
    @NotBlank(message = "End reason is required")
    @Pattern(regexp = "^(TIMEOUT|FORFEIT)$", message = "End reason must be TIMEOUT or FORFEIT")
    private String endReason;
    
    @NotEmpty(message = "Players list is required")
    @Size(min = 2, max = 2, message = "Exactly two players are required")
    @Valid
    private List<PlayerEntry> players;
    
    private Long forfeitedUserId;
    
    public SaveMatchRequest() {
    }
    
    public SaveMatchRequest(String matchId, String roomId, Instant startedAt, Instant endedAt,
                           String endReason, List<PlayerEntry> players, Long forfeitedUserId) {
        this.matchId = matchId;
        this.roomId = roomId;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.endReason = endReason;
        this.players = players;
        this.forfeitedUserId = forfeitedUserId;
    }
    
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
    
    public List<PlayerEntry> getPlayers() {
        return players;
    }
    
    public void setPlayers(List<PlayerEntry> players) {
        this.players = players;
    }
    
    public Long getForfeitedUserId() {
        return forfeitedUserId;
    }
    
    public void setForfeitedUserId(Long forfeitedUserId) {
        this.forfeitedUserId = forfeitedUserId;
    }

    @com.fasterxml.jackson.annotation.JsonSetter("forfeitedUserId")
    public void setForfeitedUserIdFromObject(Object value) {
        if (value instanceof Number n) {
            this.forfeitedUserId = n.longValue();
        } else if (value instanceof String s && !s.isBlank()) {
            try {
                this.forfeitedUserId = Long.parseLong(s);
            } catch (NumberFormatException e) {
                this.forfeitedUserId = null;
            }
        } else {
            this.forfeitedUserId = null;
        }
    }
}

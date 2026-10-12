package com.territorypaint.backend.dto;

import java.util.List;

/**
 * Response after saving a match result.
 * Requirements: 9.5
 */
public class SaveMatchResponse {
    
    private boolean success;
    private String matchId;
    private boolean alreadyExists;
    private List<PlayerStandingsDTO> players;
    
    public SaveMatchResponse() {
    }
    
    public SaveMatchResponse(boolean success, String matchId, boolean alreadyExists,
                            List<PlayerStandingsDTO> players) {
        this.success = success;
        this.matchId = matchId;
        this.alreadyExists = alreadyExists;
        this.players = players;
    }
    
    public static SaveMatchResponse newlySaved(String matchId, List<PlayerStandingsDTO> players) {
        return new SaveMatchResponse(true, matchId, false, players);
    }
    
    public static SaveMatchResponse alreadySaved(String matchId, List<PlayerStandingsDTO> players) {
        return new SaveMatchResponse(true, matchId, true, players);
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMatchId() {
        return matchId;
    }
    
    public void setMatchId(String matchId) {
        this.matchId = matchId;
    }
    
    public boolean isAlreadyExists() {
        return alreadyExists;
    }
    
    public void setAlreadyExists(boolean alreadyExists) {
        this.alreadyExists = alreadyExists;
    }
    
    public List<PlayerStandingsDTO> getPlayers() {
        return players;
    }
    
    public void setPlayers(List<PlayerStandingsDTO> players) {
        this.players = players;
    }
}

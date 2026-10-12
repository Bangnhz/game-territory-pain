package com.territorypaint.common.payload;

import com.territorypaint.common.dto.PlayerDTO;
import java.util.List;

/**
 * Online player list payload.
 * Requirements: 13.1, 13.2
 */
public class OnlineListPayload {
    private List<PlayerDTO> players;
    
    public OnlineListPayload() {
    }
    
    public OnlineListPayload(List<PlayerDTO> players) {
        this.players = players;
    }
    
    public List<PlayerDTO> getPlayers() {
        return players;
    }
    
    public void setPlayers(List<PlayerDTO> players) {
        this.players = players;
    }
}

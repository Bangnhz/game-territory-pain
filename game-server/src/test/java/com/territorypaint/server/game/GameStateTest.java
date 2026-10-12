package com.territorypaint.server.game;

import com.territorypaint.common.dto.PositionDTO;
import com.territorypaint.common.dto.SnapshotDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class GameStateTest {

    private GameState gameState;
    private String roomId;
    private String matchId;

    @BeforeEach
    public void setUp() {
        roomId = UUID.randomUUID().toString();
        matchId = UUID.randomUUID().toString();
        gameState = new GameState(roomId, matchId, 100L, "player1", 200L, "player2");
    }

    @Test
    public void testInitialState() {
        assertEquals(roomId, gameState.getRoomId());
        assertEquals(matchId, gameState.getMatchId());
        assertEquals(0, gameState.getStateVersion());

        PositionDTO p1 = gameState.getPlayerPosition(1);
        PositionDTO p2 = gameState.getPlayerPosition(2);

        assertEquals(0, p1.getX());
        assertEquals(0, p1.getY());
        assertEquals(19, p2.getX());
        assertEquals(19, p2.getY());

        int[] scores = gameState.getTerritoryScores();
        assertEquals(1, scores[0]);
        assertEquals(1, scores[1]);
    }

    @Test
    public void testCooldownCheck() {
        long now = System.currentTimeMillis();

        // First move allowed
        assertTrue(gameState.checkCooldown(1, now));

        // Move 50ms later rejected (cooldown is 100ms)
        assertFalse(gameState.checkCooldown(1, now + 50));

        // Move 105ms later allowed
        assertTrue(gameState.checkCooldown(1, now + 105));
    }

    @Test
    public void testSequenceNumberValidation() {
        // Monotonically increasing sequence allowed
        assertTrue(gameState.checkAndRecordSeq(1, 1));
        assertTrue(gameState.checkAndRecordSeq(1, 2));

        // Stale or duplicate sequence rejected
        assertFalse(gameState.checkAndRecordSeq(1, 2));
        assertFalse(gameState.checkAndRecordSeq(1, 1));

        // Higher sequence allowed
        assertTrue(gameState.checkAndRecordSeq(1, 10));
    }

    @Test
    public void testSnapshotGeneration() {
        SnapshotDTO snapshot = gameState.toSnapshotDTO(45000L);

        assertEquals(roomId, snapshot.getRoomId().toString());
        assertEquals(matchId, snapshot.getMatchId().toString());
        assertEquals(45000L, snapshot.getRemainingMs());
        assertEquals(2, snapshot.getPlayers().size());
        assertEquals(20, snapshot.getGrid().length);
    }
}

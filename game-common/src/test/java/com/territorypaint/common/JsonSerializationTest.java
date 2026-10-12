package com.territorypaint.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.territorypaint.common.dto.*;
import com.territorypaint.common.payload.*;
import com.territorypaint.common.protocol.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test JSON serialization and deserialization of all protocol messages.
 * Requirement: Task 6 - Verify game-common
 */
class JsonSerializationTest {
    
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }
    
    @Test
    void testMessageEnvelopeSerialization() throws Exception {
        Message message = new Message(ActionType.LOGIN, UUID.randomUUID(), 
                                     "2024-01-01T10:00:00Z", new LoginPayload("test-jwt"));
        
        String json = objectMapper.writeValueAsString(message);
        assertNotNull(json);
        assertTrue(json.contains("LOGIN"));
        
        Message deserialized = objectMapper.readValue(json, Message.class);
        assertEquals(ActionType.LOGIN, deserialized.getActionType());
    }
    
    @Test
    void testPlayerDTOSerialization() throws Exception {
        PlayerDTO player = new PlayerDTO(1L, "testuser", 100L, PlayerStatus.IDLE, true);
        
        String json = objectMapper.writeValueAsString(player);
        PlayerDTO deserialized = objectMapper.readValue(json, PlayerDTO.class);
        
        assertEquals(1L, deserialized.getUserId());
        assertEquals("testuser", deserialized.getUsername());
        assertEquals(PlayerStatus.IDLE, deserialized.getStatus());
        assertTrue(deserialized.isCanChallenge());
    }
    
    @Test
    void testSnapshotDTOSerialization() throws Exception {
        int[][] grid = new int[GameConstants.GRID_HEIGHT][GameConstants.GRID_WIDTH];
        grid[0][0] = 1;
        grid[19][19] = 2;
        
        PlayerSnapshotDTO p1 = new PlayerSnapshotDTO(1L, "player1", 1, 
                                                     GameConstants.PLAYER1_COLOR, 
                                                     new PositionDTO(0, 0), 1);
        PlayerSnapshotDTO p2 = new PlayerSnapshotDTO(2L, "player2", 2, 
                                                     GameConstants.PLAYER2_COLOR, 
                                                     new PositionDTO(19, 19), 1);
        
        SnapshotDTO snapshot = new SnapshotDTO(UUID.randomUUID(), UUID.randomUUID(), 
                                               0L, grid, List.of(p1, p2), 60000L);
        
        String json = objectMapper.writeValueAsString(snapshot);
        SnapshotDTO deserialized = objectMapper.readValue(json, SnapshotDTO.class);
        
        assertEquals(0L, deserialized.getStateVersion());
        assertEquals(2, deserialized.getPlayers().size());
        assertEquals(1, deserialized.getGrid()[0][0]);
        assertEquals(2, deserialized.getGrid()[19][19]);
    }
    
    @Test
    void testMovePayloadSerialization() throws Exception {
        MovePayload move = new MovePayload(UUID.randomUUID(), 1, Direction.UP);
        
        String json = objectMapper.writeValueAsString(move);
        MovePayload deserialized = objectMapper.readValue(json, MovePayload.class);
        
        assertEquals(1, deserialized.getSeq());
        assertEquals(Direction.UP, deserialized.getDirection());
    }
    
    @Test
    void testChallengePayloadsSerialization() throws Exception {
        // Challenge request
        ChallengePayload challenge = new ChallengePayload(123L);
        String json = objectMapper.writeValueAsString(challenge);
        ChallengePayload deserialized = objectMapper.readValue(json, ChallengePayload.class);
        assertEquals(123L, deserialized.getTargetUserId());
        
        // Challenge received
        ChallengeReceivedPayload received = new ChallengeReceivedPayload(
            UUID.randomUUID(), 456L, "challenger", "2024-01-01T10:00:15Z");
        json = objectMapper.writeValueAsString(received);
        ChallengeReceivedPayload deserializedReceived = objectMapper.readValue(json, ChallengeReceivedPayload.class);
        assertEquals(456L, deserializedReceived.getChallengerUserId());
    }
    
    @Test
    void testMatchResultPayloadSerialization() throws Exception {
        PlayerResultDTO r1 = new PlayerResultDTO(1L, 1, 200, MatchOutcome.WIN, 10);
        PlayerResultDTO r2 = new PlayerResultDTO(2L, 2, 200, MatchOutcome.LOSS, 0);
        
        MatchResultPayload result = new MatchResultPayload(
            UUID.randomUUID(), UUID.randomUUID(), 1L, List.of(r1, r2),
            MatchEndReason.TIMEOUT, PersistenceStatus.PENDING, "2024-01-01T10:01:00Z");
        
        String json = objectMapper.writeValueAsString(result);
        MatchResultPayload deserialized = objectMapper.readValue(json, MatchResultPayload.class);
        
        assertEquals(MatchEndReason.TIMEOUT, deserialized.getEndReason());
        assertEquals(PersistenceStatus.PENDING, deserialized.getPersistenceStatus());
        assertEquals(2, deserialized.getPlayerResults().size());
    }
    
    @Test
    void testAllEnumsSerialization() throws Exception {
        // ActionType
        String json = objectMapper.writeValueAsString(ActionType.MOVE);
        assertEquals("\"MOVE\"", json);
        
        // Direction
        json = objectMapper.writeValueAsString(Direction.UP);
        Direction dir = objectMapper.readValue(json, Direction.class);
        assertEquals(Direction.UP, dir);
        assertEquals(0, dir.getDeltaX());
        assertEquals(-1, dir.getDeltaY());
        
        // MatchOutcome
        json = objectMapper.writeValueAsString(MatchOutcome.WIN);
        assertEquals("\"WIN\"", json);
    }
    
    @Test
    void testErrorPayloadSerialization() throws Exception {
        ErrorPayload error = new ErrorPayload("INVALID_MOVE", "Move out of bounds");
        
        String json = objectMapper.writeValueAsString(error);
        ErrorPayload deserialized = objectMapper.readValue(json, ErrorPayload.class);
        
        assertEquals("INVALID_MOVE", deserialized.getCode());
        assertEquals("Move out of bounds", deserialized.getMessage());
    }
}

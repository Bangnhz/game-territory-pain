package com.territorypaint.backend.repository;

import com.territorypaint.backend.entity.Match;
import com.territorypaint.backend.entity.MatchHistory;
import com.territorypaint.backend.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MatchHistoryRepository.
 * Tests history query joining MATCHES and MATCH_HISTORY, ordered by ended_at DESC.
 */
@DataJpaTest
class MatchHistoryRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private MatchHistoryRepository matchHistoryRepository;
    
    private User user1;
    private User user2;
    private User user3;
    
    @BeforeEach
    void setUp() {
        // Clear any existing data
        matchHistoryRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();
        
        // Create test users
        user1 = new User("player1", "hash1", "Player One");
        user1.setCreatedAt(Instant.now());
        entityManager.persist(user1);
        
        user2 = new User("player2", "hash2", "Player Two");
        user2.setCreatedAt(Instant.now());
        entityManager.persist(user2);
        
        user3 = new User("player3", "hash3", "Player Three");
        user3.setCreatedAt(Instant.now());
        entityManager.persist(user3);
        
        entityManager.flush();
    }
    
    @Test
    void findHistoryByUserId_shouldReturnUserMatchHistory() {
        // Given
        createMatchWithHistory(user1, user2, Instant.now().minusSeconds(3600), "WIN", "LOSS");
        createMatchWithHistory(user1, user3, Instant.now().minusSeconds(1800), "LOSS", "WIN");
        
        // When
        Pageable pageable = PageRequest.of(0, 10);
        Page<MatchHistory> result = matchHistoryRepository.findHistoryByUserId(user1.getId(), pageable);
        
        // Then
        assertEquals(2, result.getTotalElements());
        assertTrue(result.getContent().stream().allMatch(h -> h.getUserId().equals(user1.getId())));
    }
    
    @Test
    void findHistoryByUserId_shouldOrderByEndedAtDesc() {
        // Given - Create matches at different times
        Instant time1 = Instant.parse("2024-01-15T10:00:00Z");
        Instant time2 = Instant.parse("2024-01-15T11:00:00Z");
        Instant time3 = Instant.parse("2024-01-15T12:00:00Z");
        
        createMatchWithHistory(user1, user2, time1, "WIN", "LOSS");
        createMatchWithHistory(user1, user3, time3, "LOSS", "WIN");
        createMatchWithHistory(user1, user2, time2, "DRAW", "DRAW");
        
        // When
        Pageable pageable = PageRequest.of(0, 10);
        Page<MatchHistory> result = matchHistoryRepository.findHistoryByUserId(user1.getId(), pageable);
        
        // Then
        assertEquals(3, result.getTotalElements());
        // Should be ordered by most recent first (time3, time2, time1)
        // We verify by checking the opponent IDs in the expected order
        assertEquals(user3.getId(), result.getContent().get(0).getOpponentId()); // Most recent (time3)
        assertEquals(user2.getId(), result.getContent().get(1).getOpponentId()); // Middle (time2)
        assertEquals(user2.getId(), result.getContent().get(2).getOpponentId()); // Oldest (time1)
    }
    
    @Test
    void findHistoryByUserId_shouldBreakTiesByMatchIdDesc() {
        // Given - Create two matches with same end time
        Instant sameTime = Instant.parse("2024-01-15T10:00:00Z");
        
        String matchId1 = "11111111-1111-1111-1111-111111111111";
        String matchId2 = "22222222-2222-2222-2222-222222222222";
        
        createMatchWithHistory(matchId1, user1, user2, sameTime, "WIN", "LOSS");
        createMatchWithHistory(matchId2, user1, user3, sameTime, "LOSS", "WIN");
        
        // When
        Pageable pageable = PageRequest.of(0, 10);
        Page<MatchHistory> result = matchHistoryRepository.findHistoryByUserId(user1.getId(), pageable);
        
        // Then
        assertEquals(2, result.getTotalElements());
        // Should be ordered by matchId descending (matchId2 before matchId1)
        assertEquals(matchId2, result.getContent().get(0).getMatchId());
        assertEquals(matchId1, result.getContent().get(1).getMatchId());
    }
    
    @Test
    void findHistoryByUserId_shouldReturnEmptyWhenNoHistory() {
        // Given
        createMatchWithHistory(user2, user3, Instant.now(), "WIN", "LOSS");
        
        // When - Request history for user1 who has no matches
        Pageable pageable = PageRequest.of(0, 10);
        Page<MatchHistory> result = matchHistoryRepository.findHistoryByUserId(user1.getId(), pageable);
        
        // Then
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
    }
    
    @Test
    void findHistoryByUserId_shouldSupportPagination() {
        // Given - Create 5 matches
        for (int i = 0; i < 5; i++) {
            Instant time = Instant.now().minusSeconds(i * 600);
            createMatchWithHistory(user1, user2, time, "WIN", "LOSS");
        }
        
        // When - Request page 0 with size 2
        Pageable pageable = PageRequest.of(0, 2);
        Page<MatchHistory> result = matchHistoryRepository.findHistoryByUserId(user1.getId(), pageable);
        
        // Then
        assertEquals(5, result.getTotalElements());
        assertEquals(3, result.getTotalPages());
        assertEquals(2, result.getContent().size());
    }
    
    @Test
    void findHistoryByUserId_shouldIncludeAllHistoryFields() {
        // Given
        String matchId = UUID.randomUUID().toString();
        String roomId = UUID.randomUUID().toString();
        Instant startedAt = Instant.now().minusSeconds(60);
        Instant endedAt = Instant.now();
        
        Match match = new Match(matchId, roomId, startedAt, endedAt, 
                               "TIMEOUT", user1.getId(), null, "hash123");
        entityManager.persist(match);
        
        MatchHistory history = new MatchHistory(matchId, user1.getId(), user2.getId(), 
                                                1, 250, "WIN", 10);
        entityManager.persist(history);
        entityManager.flush();
        
        // When
        Pageable pageable = PageRequest.of(0, 10);
        Page<MatchHistory> result = matchHistoryRepository.findHistoryByUserId(user1.getId(), pageable);
        
        // Then
        assertEquals(1, result.getTotalElements());
        MatchHistory retrieved = result.getContent().get(0);
        assertEquals(matchId, retrieved.getMatchId());
        assertEquals(user1.getId(), retrieved.getUserId());
        assertEquals(user2.getId(), retrieved.getOpponentId());
        assertEquals(1, retrieved.getPlayerSlot());
        assertEquals(250, retrieved.getCellsOwned());
        assertEquals("WIN", retrieved.getOutcome());
        assertEquals(10, retrieved.getRankDelta());
    }
    
    @Test
    void findHistoryByUserId_shouldNotReturnOtherUsersMatches() {
        // Given
        createMatchWithHistory(user1, user2, Instant.now().minusSeconds(3600), "WIN", "LOSS");
        createMatchWithHistory(user2, user3, Instant.now().minusSeconds(1800), "WIN", "LOSS");
        
        // When - Request history for user1
        Pageable pageable = PageRequest.of(0, 10);
        Page<MatchHistory> result = matchHistoryRepository.findHistoryByUserId(user1.getId(), pageable);
        
        // Then - Should only return user1's match, not user2 vs user3
        assertEquals(1, result.getTotalElements());
        assertEquals(user1.getId(), result.getContent().get(0).getUserId());
    }
    
    @Test
    void findHistoryByUserId_shouldHandleBothWinAndLoss() {
        // Given
        createMatchWithHistory(user1, user2, Instant.now().minusSeconds(3600), "WIN", "LOSS");
        createMatchWithHistory(user1, user3, Instant.now().minusSeconds(1800), "LOSS", "WIN");
        
        // When
        Pageable pageable = PageRequest.of(0, 10);
        Page<MatchHistory> result = matchHistoryRepository.findHistoryByUserId(user1.getId(), pageable);
        
        // Then
        assertEquals(2, result.getTotalElements());
        // Most recent should be LOSS
        assertEquals("LOSS", result.getContent().get(0).getOutcome());
        // Oldest should be WIN
        assertEquals("WIN", result.getContent().get(1).getOutcome());
    }
    
    /**
     * Helper method to create a match with history for both players.
     */
    private void createMatchWithHistory(User player1, User player2, Instant endedAt, 
                                        String outcome1, String outcome2) {
        String matchId = UUID.randomUUID().toString();
        createMatchWithHistory(matchId, player1, player2, endedAt, outcome1, outcome2);
    }
    
    /**
     * Helper method to create a match with history for both players with specific matchId.
     */
    private void createMatchWithHistory(String matchId, User player1, User player2, 
                                        Instant endedAt, String outcome1, String outcome2) {
        String roomId = UUID.randomUUID().toString();
        Instant startedAt = endedAt.minusSeconds(60);
        
        Long winnerId = null;
        if ("WIN".equals(outcome1)) winnerId = player1.getId();
        else if ("WIN".equals(outcome2)) winnerId = player2.getId();
        
        Match match = new Match(matchId, roomId, startedAt, endedAt, 
                               "TIMEOUT", winnerId, null, "hash_" + matchId);
        entityManager.persist(match);
        
        MatchHistory history1 = new MatchHistory(matchId, player1.getId(), player2.getId(), 
                                                 1, 200, outcome1, getRankDelta(outcome1));
        entityManager.persist(history1);
        
        MatchHistory history2 = new MatchHistory(matchId, player2.getId(), player1.getId(), 
                                                 2, 200, outcome2, getRankDelta(outcome2));
        entityManager.persist(history2);
        
        entityManager.flush();
    }
    
    private int getRankDelta(String outcome) {
        return switch (outcome) {
            case "WIN" -> 10;
            case "DRAW" -> 3;
            case "LOSS" -> 0;
            default -> 0;
        };
    }
}

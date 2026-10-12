package com.territorypaint.backend.repository;

import com.territorypaint.backend.entity.Match;
import com.territorypaint.backend.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MatchRepository.
 * Tests basic CRUD operations for Match entity.
 */
@DataJpaTest
class MatchRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private MatchRepository matchRepository;
    
    private User user1;
    private User user2;
    
    @BeforeEach
    void setUp() {
        // Clear any existing data
        matchRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();
        
        // Create test users
        user1 = new User("player1", "hash1", "Player One");
        user1.setCreatedAt(Instant.now());
        entityManager.persist(user1);
        
        user2 = new User("player2", "hash2", "Player Two");
        user2.setCreatedAt(Instant.now());
        entityManager.persist(user2);
        
        entityManager.flush();
    }
    
    @Test
    void findById_shouldReturnMatchWhenExists() {
        // Given
        String matchId = UUID.randomUUID().toString();
        String roomId = UUID.randomUUID().toString();
        Instant startedAt = Instant.now().minusSeconds(60);
        Instant endedAt = Instant.now();
        
        Match match = new Match(matchId, roomId, startedAt, endedAt, 
                               "TIMEOUT", user1.getId(), null, "hash123");
        entityManager.persist(match);
        entityManager.flush();
        
        // When
        Optional<Match> result = matchRepository.findById(matchId);
        
        // Then
        assertTrue(result.isPresent());
        assertEquals(matchId, result.get().getMatchId());
        assertEquals(roomId, result.get().getRoomId());
        assertEquals("TIMEOUT", result.get().getEndReason());
        assertEquals(user1.getId(), result.get().getWinnerUserId());
        assertNull(result.get().getForfeitedUserId());
    }
    
    @Test
    void findById_shouldReturnEmptyWhenNotExists() {
        // When
        String nonExistentId = UUID.randomUUID().toString();
        Optional<Match> result = matchRepository.findById(nonExistentId);
        
        // Then
        assertFalse(result.isPresent());
    }
    
    @Test
    void save_shouldPersistNewMatch() {
        // Given
        String matchId = UUID.randomUUID().toString();
        String roomId = UUID.randomUUID().toString();
        Instant startedAt = Instant.now().minusSeconds(60);
        Instant endedAt = Instant.now();
        
        Match match = new Match(matchId, roomId, startedAt, endedAt, 
                               "TIMEOUT", user1.getId(), null, "hash123");
        
        // When
        Match saved = matchRepository.save(match);
        entityManager.flush();
        entityManager.clear();
        
        // Then
        assertNotNull(saved);
        Match retrieved = entityManager.find(Match.class, matchId);
        assertNotNull(retrieved);
        assertEquals(matchId, retrieved.getMatchId());
    }
    
    @Test
    void save_shouldHandleForfeitMatch() {
        // Given
        String matchId = UUID.randomUUID().toString();
        String roomId = UUID.randomUUID().toString();
        Instant startedAt = Instant.now().minusSeconds(30);
        Instant endedAt = Instant.now();
        
        Match match = new Match(matchId, roomId, startedAt, endedAt, 
                               "FORFEIT", user1.getId(), user2.getId(), "hash456");
        
        // When
        matchRepository.save(match);
        entityManager.flush();
        
        // Then
        Optional<Match> result = matchRepository.findById(matchId);
        assertTrue(result.isPresent());
        assertEquals("FORFEIT", result.get().getEndReason());
        assertEquals(user1.getId(), result.get().getWinnerUserId());
        assertEquals(user2.getId(), result.get().getForfeitedUserId());
    }
    
    @Test
    void save_shouldHandleDrawMatch() {
        // Given
        String matchId = UUID.randomUUID().toString();
        String roomId = UUID.randomUUID().toString();
        Instant startedAt = Instant.now().minusSeconds(60);
        Instant endedAt = Instant.now();
        
        Match match = new Match(matchId, roomId, startedAt, endedAt, 
                               "TIMEOUT", null, null, "hash789");
        
        // When
        matchRepository.save(match);
        entityManager.flush();
        
        // Then
        Optional<Match> result = matchRepository.findById(matchId);
        assertTrue(result.isPresent());
        assertNull(result.get().getWinnerUserId()); // Draw has no winner
        assertNull(result.get().getForfeitedUserId());
    }
    
    @Test
    void save_shouldPreserveAllFields() {
        // Given
        String matchId = UUID.randomUUID().toString();
        String roomId = UUID.randomUUID().toString();
        Instant startedAt = Instant.parse("2024-01-15T10:00:00Z");
        Instant endedAt = Instant.parse("2024-01-15T10:01:00Z");
        String payloadHash = "a".repeat(64);
        
        Match match = new Match(matchId, roomId, startedAt, endedAt, 
                               "TIMEOUT", user1.getId(), null, payloadHash);
        
        // When
        matchRepository.save(match);
        entityManager.flush();
        entityManager.clear();
        
        // Then
        Optional<Match> result = matchRepository.findById(matchId);
        assertTrue(result.isPresent());
        Match retrieved = result.get();
        assertEquals(matchId, retrieved.getMatchId());
        assertEquals(roomId, retrieved.getRoomId());
        assertEquals(startedAt, retrieved.getStartedAt());
        assertEquals(endedAt, retrieved.getEndedAt());
        assertEquals("TIMEOUT", retrieved.getEndReason());
        assertEquals(user1.getId(), retrieved.getWinnerUserId());
        assertEquals(payloadHash, retrieved.getPayloadHash());
    }
    
    @Test
    void delete_shouldRemoveMatch() {
        // Given
        String matchId = UUID.randomUUID().toString();
        String roomId = UUID.randomUUID().toString();
        Instant startedAt = Instant.now().minusSeconds(60);
        Instant endedAt = Instant.now();
        
        Match match = new Match(matchId, roomId, startedAt, endedAt, 
                               "TIMEOUT", user1.getId(), null, "hash123");
        entityManager.persist(match);
        entityManager.flush();
        
        // When
        matchRepository.deleteById(matchId);
        entityManager.flush();
        
        // Then
        Optional<Match> result = matchRepository.findById(matchId);
        assertFalse(result.isPresent());
    }
    
    @Test
    void count_shouldReturnCorrectNumber() {
        // Given
        for (int i = 0; i < 3; i++) {
            String matchId = UUID.randomUUID().toString();
            String roomId = UUID.randomUUID().toString();
            Instant startedAt = Instant.now().minusSeconds(60);
            Instant endedAt = Instant.now();
            
            Match match = new Match(matchId, roomId, startedAt, endedAt, 
                                   "TIMEOUT", user1.getId(), null, "hash" + i);
            entityManager.persist(match);
        }
        entityManager.flush();
        
        // When
        long count = matchRepository.count();
        
        // Then
        assertEquals(3, count);
    }
}

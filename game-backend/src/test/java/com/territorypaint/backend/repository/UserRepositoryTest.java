package com.territorypaint.backend.repository;

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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for UserRepository.
 * Tests findByUsername and leaderboard query functionality.
 */
@DataJpaTest
class UserRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private UserRepository userRepository;
    
    @BeforeEach
    void setUp() {
        // Clear any existing data
        userRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();
    }
    
    @Test
    void findByUsername_shouldReturnUserWhenExists() {
        // Given
        User user = new User("testuser", "hashedPassword123", "Test User");
        user.setCreatedAt(Instant.now());
        entityManager.persist(user);
        entityManager.flush();
        
        // When
        Optional<User> result = userRepository.findByUsername("testuser");
        
        // Then
        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
        assertEquals("hashedPassword123", result.get().getPasswordHash());
        assertEquals("Test User", result.get().getFullName());
    }
    
    @Test
    void findByUsername_shouldReturnEmptyWhenNotExists() {
        // When
        Optional<User> result = userRepository.findByUsername("nonexistent");
        
        // Then
        assertFalse(result.isPresent());
    }
    
    @Test
    void findByUsername_shouldBeCaseExact() {
        // Given
        User user = new User("testuser", "hashedPassword123", "Test User");
        user.setCreatedAt(Instant.now());
        entityManager.persist(user);
        entityManager.flush();
        
        // When
        Optional<User> result = userRepository.findByUsername("TESTUSER");
        
        // Then
        assertFalse(result.isPresent());
    }
    
    @Test
    void findLeaderboard_shouldOrderByTotalScoreDescThenIdAsc() {
        // Given - Create users with different scores
        User user1 = new User("user1", "hash1", "User One");
        user1.setTotalScore(100L);
        user1.setCreatedAt(Instant.now());
        
        User user2 = new User("user2", "hash2", "User Two");
        user2.setTotalScore(200L);
        user2.setCreatedAt(Instant.now());
        
        User user3 = new User("user3", "hash3", "User Three");
        user3.setTotalScore(150L);
        user3.setCreatedAt(Instant.now());
        
        entityManager.persist(user1);
        entityManager.persist(user2);
        entityManager.persist(user3);
        entityManager.flush();
        
        // When
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> result = userRepository.findLeaderboard(pageable);
        
        // Then
        assertEquals(3, result.getTotalElements());
        assertEquals("user2", result.getContent().get(0).getUsername()); // 200 points
        assertEquals("user3", result.getContent().get(1).getUsername()); // 150 points
        assertEquals("user1", result.getContent().get(2).getUsername()); // 100 points
    }
    
    @Test
    void findLeaderboard_shouldBreakTiesByIdAscending() {
        // Given - Create users with same score (tie scenario)
        User user1 = new User("user1", "hash1", "User One");
        user1.setTotalScore(100L);
        user1.setCreatedAt(Instant.now());
        
        User user2 = new User("user2", "hash2", "User Two");
        user2.setTotalScore(100L);
        user2.setCreatedAt(Instant.now());
        
        User user3 = new User("user3", "hash3", "User Three");
        user3.setTotalScore(100L);
        user3.setCreatedAt(Instant.now());
        
        entityManager.persist(user1);
        entityManager.persist(user2);
        entityManager.persist(user3);
        entityManager.flush();
        
        // When
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> result = userRepository.findLeaderboard(pageable);
        
        // Then
        assertEquals(3, result.getTotalElements());
        // All have same score, so should be ordered by ID ascending
        assertTrue(result.getContent().get(0).getId() < result.getContent().get(1).getId());
        assertTrue(result.getContent().get(1).getId() < result.getContent().get(2).getId());
    }
    
    @Test
    void findLeaderboard_shouldSupportPagination() {
        // Given - Create 5 users
        for (int i = 1; i <= 5; i++) {
            User user = new User("user" + i, "hash" + i, "User " + i);
            user.setTotalScore((long) (i * 10));
            user.setCreatedAt(Instant.now());
            entityManager.persist(user);
        }
        entityManager.flush();
        
        // When - Request page 0 with size 2
        Pageable pageable = PageRequest.of(0, 2);
        Page<User> result = userRepository.findLeaderboard(pageable);
        
        // Then
        assertEquals(5, result.getTotalElements());
        assertEquals(3, result.getTotalPages());
        assertEquals(2, result.getContent().size());
        assertEquals("user5", result.getContent().get(0).getUsername()); // Highest score (50)
        assertEquals("user4", result.getContent().get(1).getUsername()); // Second highest (40)
    }
    
    @Test
    void findLeaderboard_shouldReturnEmptyPageWhenNoUsers() {
        // When
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> result = userRepository.findLeaderboard(pageable);
        
        // Then
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
    }
    
    @Test
    void findLeaderboard_shouldIncludeZeroScoreUsers() {
        // Given
        User user1 = new User("user1", "hash1", "User One");
        user1.setTotalScore(0L);
        user1.setCreatedAt(Instant.now());
        
        User user2 = new User("user2", "hash2", "User Two");
        user2.setTotalScore(10L);
        user2.setCreatedAt(Instant.now());
        
        entityManager.persist(user1);
        entityManager.persist(user2);
        entityManager.flush();
        
        // When
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> result = userRepository.findLeaderboard(pageable);
        
        // Then
        assertEquals(2, result.getTotalElements());
        assertEquals("user2", result.getContent().get(0).getUsername());
        assertEquals("user1", result.getContent().get(1).getUsername());
    }
}

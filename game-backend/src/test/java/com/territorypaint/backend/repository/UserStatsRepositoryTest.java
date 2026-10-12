package com.territorypaint.backend.repository;

import com.territorypaint.backend.entity.User;
import com.territorypaint.backend.entity.UserStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for UserStatsRepository.
 * Tests custom locking query for deterministic row locking.
 */
@DataJpaTest
class UserStatsRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserStatsRepository userStatsRepository;

    @BeforeEach
    void setUp() {
        // Clear any existing data
        userStatsRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void save_shouldPersistNewUserStats() {
        User user = new User("testuser", "hash", "Test User");
        user.setCreatedAt(Instant.now());
        user = entityManager.persist(user);
        entityManager.flush();

        UserStats stats = new UserStats(user);
        userStatsRepository.save(stats);
        entityManager.flush();
        entityManager.clear();

        UserStats found = entityManager.find(UserStats.class, user.getId());
        assertNotNull(found);
    }

    @Test
    void findByUserIdInForUpdate_shouldReturnUsersInIdOrder() {
        // Given - Create users with stats
        User user1 = createUserWithStats("user1", "hash1", "User One");
        User user2 = createUserWithStats("user2", "hash2", "User Two");
        User user3 = createUserWithStats("user3", "hash3", "User Three");

        Long id1 = user1.getId();
        Long id2 = user2.getId();
        Long id3 = user3.getId();

        // When - Request in arbitrary order
        List<Long> userIds = Arrays.asList(id3, id1, id2);
        List<UserStats> result = userStatsRepository.findByUserIdInForUpdate(userIds);

        // Then - Should be ordered by ID ascending
        assertEquals(3, result.size());
        assertTrue(result.get(0).getUser().getId() < result.get(1).getUser().getId());
        assertTrue(result.get(1).getUser().getId() < result.get(2).getUser().getId());
    }

    @Test
    void findByUserIdInForUpdate_shouldReturnOnlyRequestedUsers() {
        // Given
        User user1 = createUserWithStats("user1", "hash1", "User One");
        User user2 = createUserWithStats("user2", "hash2", "User Two");
        User user3 = createUserWithStats("user3", "hash3", "User Three");

        // When - Request only 2 of 3 users
        List<Long> userIds = Arrays.asList(user1.getId(), user3.getId());
        List<UserStats> result = userStatsRepository.findByUserIdInForUpdate(userIds);

        // Then
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(us -> us.getUser().getId().equals(user1.getId())));
        assertTrue(result.stream().anyMatch(us -> us.getUser().getId().equals(user3.getId())));
        assertFalse(result.stream().anyMatch(us -> us.getUser().getId().equals(user2.getId())));
    }

    @Test
    void findByUserIdInForUpdate_shouldReturnEmptyListWhenNoMatches() {
        // Given
        createUserWithStats("user1", "hash1", "User One");

        // When - Request non-existent user ID
        List<Long> userIds = Arrays.asList(999L, 1000L);
        List<UserStats> result = userStatsRepository.findByUserIdInForUpdate(userIds);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void findByUserIdInForUpdate_shouldHandleSingleUser() {
        // Given
        User user1 = createUserWithStats("user1", "hash1", "User One");

        // When
        List<Long> userIds = Arrays.asList(user1.getId());
        List<UserStats> result = userStatsRepository.findByUserIdInForUpdate(userIds);

        // Then
        assertEquals(1, result.size());
        assertEquals(user1.getId(), result.get(0).getUser().getId());
    }

    @Test
    void findByUserIdInForUpdate_shouldHandleEmptyList() {
        // Given
        createUserWithStats("user1", "hash1", "User One");

        // When
        List<Long> userIds = Arrays.asList();
        List<UserStats> result = userStatsRepository.findByUserIdInForUpdate(userIds);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void findByUserIdInForUpdate_shouldLockRowsForUpdate() {
        // Given
        User user1 = createUserWithStats("user1", "hash1", "User One");
        User user2 = createUserWithStats("user2", "hash2", "User Two");

        // When - Lock the rows
        List<Long> userIds = Arrays.asList(user1.getId(), user2.getId());
        List<UserStats> result = userStatsRepository.findByUserIdInForUpdate(userIds);

        // Then - Rows should be returned (locking happens at DB level)
        assertEquals(2, result.size());
        // The actual locking behavior is tested at the integration level
        // This test verifies the query executes without error
    }

    @Test
    void findByUserIdInForUpdate_shouldPreserveStatsData() {
        // Given
        User user1 = createUserWithStats("user1", "hash1", "User One");
        UserStats stats = entityManager.find(UserStats.class, user1.getId());
        stats.setMatchesPlayed(10);
        stats.setMatchesWon(6);
        stats.setMatchesLost(3);
        stats.setMatchesDrawn(1);
        entityManager.persist(stats);
        entityManager.flush();

        // When
        List<Long> userIds = Arrays.asList(user1.getId());
        List<UserStats> result = userStatsRepository.findByUserIdInForUpdate(userIds);

        // Then
        assertEquals(1, result.size());
        UserStats retrieved = result.get(0);
        assertEquals(10, retrieved.getMatchesPlayed());
        assertEquals(6, retrieved.getMatchesWon());
        assertEquals(3, retrieved.getMatchesLost());
        assertEquals(1, retrieved.getMatchesDrawn());
    }

    /**
     * Helper method to create a user with associated stats.
     */
    private User createUserWithStats(String username, String hash, String fullName) {
        User user = new User(username, hash, fullName);
        user.setCreatedAt(Instant.now());
        entityManager.persist(user);
        entityManager.flush();

        UserStats stats = new UserStats(user);
        entityManager.persist(stats);
        entityManager.flush();

        return user;
    }
}

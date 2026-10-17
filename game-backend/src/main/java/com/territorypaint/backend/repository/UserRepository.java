package com.territorypaint.backend.repository;

import com.territorypaint.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for User entity.
 * Requirements: 10.6, 10.7
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by username.
     * Requirement: 10.6
     */
    Optional<User> findByUsername(String username);

    /**
     * Paginated leaderboard query ordered by totalScore DESC, id ASC.
     * Requirement: 10.7
     */
    @Query("SELECT u FROM User u ORDER BY u.totalScore DESC, u.id ASC")
    Page<User> findLeaderboard(Pageable pageable);

    /**
     * Calculate 1-based rank for a user based on totalScore DESC, id ASC.
     * Count users with higher totalScore, or same totalScore and lower id.
     */
    @Query("SELECT COUNT(u) + 1 FROM User u WHERE u.totalScore > :score OR (u.totalScore = :score AND u.id < :id)")
    long findUserRank(@org.springframework.data.repository.query.Param("score") Long score,
            @org.springframework.data.repository.query.Param("id") Long id);
}

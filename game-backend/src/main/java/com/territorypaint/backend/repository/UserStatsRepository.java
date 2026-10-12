package com.territorypaint.backend.repository;

import com.territorypaint.backend.entity.UserStats;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for UserStats entity.
 * Requirements: 11.2, 12.12
 */
@Repository
public interface UserStatsRepository extends JpaRepository<UserStats, Long> {
    
    /**
     * Custom locking query for deterministic row locking.
     * Locks rows in user ID order to prevent deadlocks.
     * Requirement: 11.2, 12.12
     * 
     * @param userIds List of user IDs to lock, will be ordered by ID for deterministic locking
     * @return List of UserStats in ID order
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT us FROM UserStats us WHERE us.user.id IN :userIds ORDER BY us.user.id ASC")
    List<UserStats> findByUserIdInForUpdate(@Param("userIds") List<Long> userIds);
}

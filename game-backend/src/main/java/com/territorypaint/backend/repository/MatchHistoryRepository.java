package com.territorypaint.backend.repository;

import com.territorypaint.backend.entity.MatchHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for MatchHistory entity.
 * Requirements: 11.4, 12.12
 */
@Repository
public interface MatchHistoryRepository extends JpaRepository<MatchHistory, Long> {
    
    /**
     * History query joining MATCHES and MATCH_HISTORY, ordered by MATCHES.ended_at DESC.
     * Requirement: 11.4, 12.12
     * 
     * @param userId The user ID to fetch history for
     * @param pageable Pagination parameters
     * @return Page of MatchHistory with joined Match data, ordered by match end time DESC
     */
    @Query("""
        SELECT mh FROM MatchHistory mh
        JOIN Match m ON mh.matchId = m.matchId
        WHERE mh.userId = :userId
        ORDER BY m.endedAt DESC, mh.matchId DESC
        """)
    Page<MatchHistory> findHistoryByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * Find opponent's MatchHistory entry for a specific match.
     */
    java.util.Optional<MatchHistory> findByMatchIdAndUserIdNot(String matchId, Long userId);
}

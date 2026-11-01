package com.territorypaint.backend.repository;

import com.territorypaint.backend.entity.MatchSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for MatchSkill entity.
 */
@Repository
public interface MatchSkillRepository extends JpaRepository<MatchSkill, Long> {
    
    /**
     * Find all skills used in a specific match.
     */
    List<MatchSkill> findByMatchId(String matchId);
    
    /**
     * Find all skills used by a specific user in a match.
     */
    @Query("SELECT ms FROM MatchSkill ms WHERE ms.matchId = :matchId AND ms.userId = :userId")
    List<MatchSkill> findByMatchIdAndUserId(@Param("matchId") String matchId, @Param("userId") Long userId);
    
    /**
     * Check if a user has already used a specific skill in a match.
     */
    @Query("SELECT COUNT(ms) > 0 FROM MatchSkill ms WHERE ms.matchId = :matchId AND ms.userId = :userId AND ms.skillType = :skillType")
    boolean hasUsedSkill(@Param("matchId") String matchId, @Param("userId") Long userId, @Param("skillType") String skillType);
}

package com.territorypaint.backend.repository;

import com.territorypaint.backend.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for Match entity.
 * Requirements: 11.3
 */
@Repository
public interface MatchRepository extends JpaRepository<Match, String> {
    // findById inherited from JpaRepository<Match, String> provides matchId lookup
}

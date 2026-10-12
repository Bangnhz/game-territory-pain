package com.territorypaint.backend.entity;

import com.territorypaint.common.protocol.MatchOutcome;
import jakarta.persistence.*;

import org.springframework.data.domain.Persistable;

/**
 * User statistics entity.
 * Requirements: 12.2, 12.3, 12.5, 12.6, 12.7, 12.9
 */
@Entity
@Table(name = "USER_STATS")
public class UserStats implements Persistable<Long> {
    @Id
    @Column(name = "user_id")
    private Long id;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Transient
    private boolean isNew = true;

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostPersist
    @PostLoad
    void markNotNew() {
        this.isNew = false;
    }

    @Column(name = "matches_played", nullable = false)
    private Integer matchesPlayed = 0;

    @Column(name = "matches_won", nullable = false)
    private Integer matchesWon = 0;

    @Column(name = "matches_lost", nullable = false)
    private Integer matchesLost = 0;

    @Column(name = "matches_drawn", nullable = false)
    private Integer matchesDrawn = 0;

    // Constructors
    public UserStats() {
    }

    public UserStats(User user) {
        this.user = user;
        if (user != null) {
            this.id = user.getId();
        }
        this.matchesPlayed = 0;
        this.matchesWon = 0;
        this.matchesLost = 0;
        this.matchesDrawn = 0;
    }

    /**
     * Apply match outcome to increment counters.
     * Requirement: 12.9
     */
    public void applyOutcome(MatchOutcome outcome) {
        this.matchesPlayed++;
        switch (outcome) {
            case WIN -> this.matchesWon++;
            case LOSS -> this.matchesLost++;
            case DRAW -> this.matchesDrawn++;
        }
    }

    /**
     * Calculate win rate, returning 0.00 when matchesPlayed is zero.
     * Requirement: 12.9
     */
    public double calculateWinRate() {
        if (matchesPlayed == 0) {
            return 0.00;
        }
        return Math.round((matchesWon * 100.0 / matchesPlayed) * 100.0) / 100.0;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
        if (user != null) {
            this.id = user.getId();
        }
    }

    public Integer getMatchesPlayed() {
        return matchesPlayed;
    }

    public void setMatchesPlayed(Integer matchesPlayed) {
        this.matchesPlayed = matchesPlayed;
    }

    public Integer getMatchesWon() {
        return matchesWon;
    }

    public void setMatchesWon(Integer matchesWon) {
        this.matchesWon = matchesWon;
    }

    public Integer getMatchesLost() {
        return matchesLost;
    }

    public void setMatchesLost(Integer matchesLost) {
        this.matchesLost = matchesLost;
    }

    public Integer getMatchesDrawn() {
        return matchesDrawn;
    }

    public void setMatchesDrawn(Integer matchesDrawn) {
        this.matchesDrawn = matchesDrawn;
    }
}

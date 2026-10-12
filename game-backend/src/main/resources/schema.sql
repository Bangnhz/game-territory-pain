-- Database schema for Territorial Paint Game Backend
-- Requirements: 12.1, 12.2, 12.3, 12.5, 12.6, 12.7, 12.8, 12.12

-- Drop tables if they exist (for development)
DROP TABLE IF EXISTS MATCH_HISTORY;
DROP TABLE IF EXISTS MATCHES;
DROP TABLE IF EXISTS USER_STATS;
DROP TABLE IF EXISTS USERS;

-- USERS table
CREATE TABLE USERS (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(30) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    total_score BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    
    CONSTRAINT chk_total_score_nonnegative CHECK (total_score >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- USER_STATS table
CREATE TABLE USER_STATS (
    user_id BIGINT PRIMARY KEY,
    matches_played INT NOT NULL DEFAULT 0,
    matches_won INT NOT NULL DEFAULT 0,
    matches_lost INT NOT NULL DEFAULT 0,
    matches_drawn INT NOT NULL DEFAULT 0,
    
    CONSTRAINT fk_user_stats_user FOREIGN KEY (user_id) REFERENCES USERS(id) ON DELETE RESTRICT,
    CONSTRAINT chk_matches_played_nonnegative CHECK (matches_played >= 0),
    CONSTRAINT chk_matches_won_nonnegative CHECK (matches_won >= 0),
    CONSTRAINT chk_matches_lost_nonnegative CHECK (matches_lost >= 0),
    CONSTRAINT chk_matches_drawn_nonnegative CHECK (matches_drawn >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- MATCHES table
CREATE TABLE MATCHES (
    match_id CHAR(36) PRIMARY KEY,
    room_id CHAR(36) NOT NULL,
    started_at DATETIME(3) NOT NULL,
    ended_at DATETIME(3) NOT NULL,
    end_reason VARCHAR(16) NOT NULL,
    winner_user_id BIGINT NULL,
    forfeited_user_id BIGINT NULL,
    payload_hash CHAR(64) NOT NULL,
    
    CONSTRAINT fk_matches_winner FOREIGN KEY (winner_user_id) REFERENCES USERS(id) ON DELETE RESTRICT,
    CONSTRAINT fk_matches_forfeited FOREIGN KEY (forfeited_user_id) REFERENCES USERS(id) ON DELETE RESTRICT,
    CONSTRAINT chk_end_reason CHECK (end_reason IN ('TIMEOUT', 'FORFEIT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- MATCH_HISTORY table
CREATE TABLE MATCH_HISTORY (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    match_id CHAR(36) NOT NULL,
    user_id BIGINT NOT NULL,
    opponent_id BIGINT NOT NULL,
    player_slot TINYINT NOT NULL,
    cells_owned SMALLINT NOT NULL,
    outcome VARCHAR(8) NOT NULL,
    rank_delta INT NOT NULL,
    
    CONSTRAINT fk_match_history_match FOREIGN KEY (match_id) REFERENCES MATCHES(match_id) ON DELETE RESTRICT,
    CONSTRAINT fk_match_history_user FOREIGN KEY (user_id) REFERENCES USERS(id) ON DELETE RESTRICT,
    CONSTRAINT fk_match_history_opponent FOREIGN KEY (opponent_id) REFERENCES USERS(id) ON DELETE RESTRICT,
    CONSTRAINT uq_match_user UNIQUE (match_id, user_id),
    CONSTRAINT uq_match_slot UNIQUE (match_id, player_slot),
    CONSTRAINT chk_player_slot CHECK (player_slot IN (1, 2)),
    CONSTRAINT chk_cells_owned CHECK (cells_owned >= 0 AND cells_owned <= 400),
    CONSTRAINT chk_outcome CHECK (outcome IN ('WIN', 'LOSS', 'DRAW'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Indexes for performance

-- Username lookup for login
CREATE INDEX idx_users_username ON USERS(username);

-- Leaderboard ordering: total_score DESC, id ASC
CREATE INDEX idx_users_leaderboard ON USERS(total_score DESC, id ASC);

-- Match history lookup by user and match end time
CREATE INDEX idx_match_history_user ON MATCH_HISTORY(user_id, match_id);
CREATE INDEX idx_matches_ended_at ON MATCHES(ended_at DESC);

-- Composite index for efficient history queries
CREATE INDEX idx_match_history_lookup ON MATCH_HISTORY(user_id, match_id);

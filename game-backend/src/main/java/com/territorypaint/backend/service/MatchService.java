package com.territorypaint.backend.service;

import com.territorypaint.backend.dto.*;
import com.territorypaint.backend.entity.*;
import com.territorypaint.backend.exception.MatchResultConflictException;
import com.territorypaint.backend.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;

/**
 * Service for match persistence, idempotency, and history lookup.
 * Requirements: 8.1, 8.2, 8.3, 8.4, 8.10, 9.7, 9.8, 9.9, 9.10, 9.11, 9.12, 9.13, 9.14, 9.15, 9.16, 11.1, 11.2, 11.3, 11.4, 11.5, 11.6
 */
@Service
public class MatchService {

    private final MatchRepository matchRepository;
    private final MatchHistoryRepository matchHistoryRepository;
    private final UserRepository userRepository;
    private final UserStatsRepository userStatsRepository;

    public MatchService(MatchRepository matchRepository,
                        MatchHistoryRepository matchHistoryRepository,
                        UserRepository userRepository,
                        UserStatsRepository userStatsRepository) {
        this.matchRepository = matchRepository;
        this.matchHistoryRepository = matchHistoryRepository;
        this.userRepository = userRepository;
        this.userStatsRepository = userStatsRepository;
    }

    /**
     * Save match result with payload hash idempotency and statistics updating.
     * Requirements: 9.5-9.16
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public SaveMatchResponse saveMatch(SaveMatchRequest request) {
        // Calculate payload hash for idempotency check (Requirement 9.14)
        String payloadHash = calculatePayloadHash(request);

        // Check if matchId was previously saved (Requirements 9.14, 9.15, 9.16)
        Optional<Match> existingMatchOpt = matchRepository.findById(request.getMatchId());
        if (existingMatchOpt.isPresent()) {
            Match existingMatch = existingMatchOpt.get();
            if (existingMatch.getPayloadHash().equals(payloadHash)) {
                // Idempotent resubmission with same payload (Requirement 9.15)
                List<PlayerStandingsDTO> currentStandings = getCurrentStandings(request.getPlayers());
                return SaveMatchResponse.alreadySaved(request.getMatchId(), currentStandings);
            } else {
                // Idempotent resubmission with different payload (Requirement 9.16)
                throw new MatchResultConflictException("Match result conflict for matchId: " + request.getMatchId());
            }
        }

        // Validate distinct existing users
        PlayerEntry player1 = request.getPlayers().get(0);
        PlayerEntry player2 = request.getPlayers().get(1);

        User user1 = userRepository.findById(player1.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + player1.getUserId()));
        User user2 = userRepository.findById(player2.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + player2.getUserId()));

        // Determine outcomes and rank deltas based on rules (Requirements 8.1-8.4, 8.10, 9.8)
        String outcome1;
        String outcome2;
        Long winnerUserId = null;

        if ("FORFEIT".equals(request.getEndReason())) {
            // Forfeit rules (Requirement 8.7)
            if (player1.getUserId().equals(request.getForfeitedUserId())) {
                outcome1 = "LOSS";
                outcome2 = "WIN";
                winnerUserId = player2.getUserId();
            } else {
                outcome1 = "WIN";
                outcome2 = "LOSS";
                winnerUserId = player1.getUserId();
            }
        } else {
            // Timeout rules based on cells owned (Requirements 8.2-8.4)
            if (player1.getCellsOwned() > player2.getCellsOwned()) {
                outcome1 = "WIN";
                outcome2 = "LOSS";
                winnerUserId = player1.getUserId();
            } else if (player2.getCellsOwned() > player1.getCellsOwned()) {
                outcome1 = "LOSS";
                outcome2 = "WIN";
                winnerUserId = player2.getUserId();
            } else {
                outcome1 = "DRAW";
                outcome2 = "DRAW";
                winnerUserId = null;
            }
        }

        int rankDelta1 = calculateRankDelta(outcome1);
        int rankDelta2 = calculateRankDelta(outcome2);

        // 1. Insert MATCHES record (Requirement 9.9)
        Match match = new Match(
                request.getMatchId(),
                request.getRoomId(),
                request.getStartedAt(),
                request.getEndedAt(),
                request.getEndReason(),
                winnerUserId,
                request.getForfeitedUserId(),
                payloadHash
        );
        matchRepository.save(match);

        // 2. Insert MATCH_HISTORY records (Requirement 9.9, 9.10)
        MatchHistory history1 = new MatchHistory(
                request.getMatchId(),
                player1.getUserId(),
                player2.getUserId(),
                player1.getPlayerSlot(),
                player1.getCellsOwned(),
                outcome1,
                rankDelta1
        );

        MatchHistory history2 = new MatchHistory(
                request.getMatchId(),
                player2.getUserId(),
                player1.getUserId(),
                player2.getPlayerSlot(),
                player2.getCellsOwned(),
                outcome2,
                rankDelta2
        );

        matchHistoryRepository.save(history1);
        matchHistoryRepository.save(history2);

        // 3. Lock UserStats rows in deterministic order to prevent deadlocks (Requirement 9.12)
        List<Long> userIdsInOrder = Arrays.asList(
                Math.min(player1.getUserId(), player2.getUserId()),
                Math.max(player1.getUserId(), player2.getUserId())
        );

        List<UserStats> statsList = userStatsRepository.findByUserIdInForUpdate(userIdsInOrder);
        Map<Long, UserStats> statsMap = new HashMap<>();
        for (UserStats s : statsList) {
            statsMap.put(s.getUser().getId(), s);
        }

        UserStats stats1 = statsMap.computeIfAbsent(player1.getUserId(), id -> new UserStats(user1));
        UserStats stats2 = statsMap.computeIfAbsent(player2.getUserId(), id -> new UserStats(user2));

        // Update statistics counters (Requirements 9.10, 9.11)
        stats1.applyOutcome(com.territorypaint.common.protocol.MatchOutcome.valueOf(outcome1));
        stats2.applyOutcome(com.territorypaint.common.protocol.MatchOutcome.valueOf(outcome2));

        userStatsRepository.save(stats1);
        userStatsRepository.save(stats2);

        // Update total_score for both users (Requirement 9.11)
        user1.setTotalScore(Math.max(0L, user1.getTotalScore() + rankDelta1));
        user2.setTotalScore(Math.max(0L, user2.getTotalScore() + rankDelta2));

        userRepository.save(user1);
        userRepository.save(user2);

        // Build standings for response (Requirement 9.7, 9.8)
        List<PlayerStandingsDTO> standings = Arrays.asList(
                buildStandingsDTO(user1, stats1),
                buildStandingsDTO(user2, stats2)
        );

        return SaveMatchResponse.newlySaved(request.getMatchId(), standings);
    }

    /**
     * Get match history for a player with pagination.
     * Requirements: 11.1-11.9
     */
    @Transactional(readOnly = true)
    public PageResponse<MatchHistoryItemDTO> getMatchHistory(Long userId, int page, int size) {
        page = Math.max(0, page);
        size = Math.max(1, Math.min(100, size));
        Pageable pageable = PageRequest.of(page, size);

        Page<MatchHistory> historyPage = matchHistoryRepository.findHistoryByUserId(userId, pageable);

        List<MatchHistoryItemDTO> items = new ArrayList<>();
        for (MatchHistory mh : historyPage.getContent()) {
            Match m = matchRepository.findById(mh.getMatchId()).orElse(null);
            User opponent = userRepository.findById(mh.getOpponentId()).orElse(null);

            // Find opponent's history record to get opponent cells owned
            Integer opponentCells = matchHistoryRepository.findByMatchIdAndUserIdNot(mh.getMatchId(), userId)
                    .map(MatchHistory::getCellsOwned)
                    .orElse(0);

            Instant playedAt = (m != null) ? m.getEndedAt() : Instant.now();
            String endReason = (m != null) ? m.getEndReason() : "TIMEOUT";
            String opponentUsername = (opponent != null) ? opponent.getUsername() : "Unknown";

            items.add(new MatchHistoryItemDTO(
                    mh.getMatchId(),
                    playedAt,
                    mh.getOpponentId(),
                    opponentUsername,
                    mh.getCellsOwned(),
                    opponentCells,
                    mh.getOutcome(),
                    mh.getRankDelta(),
                    endReason
            ));
        }

        return new PageResponse<>(
                items,
                page,
                size,
                historyPage.getTotalElements(),
                historyPage.getTotalPages()
        );
    }

    private int calculateRankDelta(String outcome) {
        return switch (outcome) {
            case "WIN" -> 10;
            case "DRAW" -> 3;
            case "LOSS" -> -5;
            default -> 0;
        };
    }

    private String calculatePayloadHash(SaveMatchRequest request) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            StringBuilder sb = new StringBuilder();
            sb.append(request.getMatchId()).append(";")
              .append(request.getRoomId()).append(";")
              .append(request.getEndReason()).append(";")
              .append(request.getForfeitedUserId()).append(";");

            List<PlayerEntry> sortedPlayers = new ArrayList<>(request.getPlayers());
            sortedPlayers.sort(Comparator.comparing(PlayerEntry::getPlayerSlot));
            for (PlayerEntry p : sortedPlayers) {
                sb.append(p.getUserId()).append(":").append(p.getPlayerSlot()).append(":").append(p.getCellsOwned()).append(";");
            }

            byte[] hashBytes = digest.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private List<PlayerStandingsDTO> getCurrentStandings(List<PlayerEntry> players) {
        List<PlayerStandingsDTO> list = new ArrayList<>();
        for (PlayerEntry pe : players) {
            User user = userRepository.findById(pe.getUserId()).orElse(null);
            UserStats stats = userStatsRepository.findById(pe.getUserId()).orElse(null);
            if (user != null && stats != null) {
                list.add(buildStandingsDTO(user, stats));
            }
        }
        return list;
    }

    private PlayerStandingsDTO buildStandingsDTO(User user, UserStats stats) {
        return new PlayerStandingsDTO(
                user.getId(),
                user.getTotalScore(),
                stats.getMatchesPlayed(),
                stats.getMatchesWon(),
                stats.getMatchesLost(),
                stats.getMatchesDrawn(),
                stats.calculateWinRate()
        );
    }
}

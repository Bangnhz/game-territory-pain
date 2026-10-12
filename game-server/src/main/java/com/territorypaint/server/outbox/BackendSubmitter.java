package com.territorypaint.server.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.territorypaint.common.dto.Message;
import com.territorypaint.common.dto.PlayerDTO;
import com.territorypaint.common.dto.StandingsDTO;
import com.territorypaint.common.payload.ResultSavedPayload;
import com.territorypaint.common.protocol.ActionType;
import com.territorypaint.server.net.BackendClient;
import com.territorypaint.server.session.Session;
import com.territorypaint.server.session.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Handles background submission of pending match results with retry logic.
 * Requirements: 9.3, 9.17, 9.18, 9.19, 9.20, 9.21
 */
public class BackendSubmitter {

    private static final Logger logger = LoggerFactory.getLogger(BackendSubmitter.class);

    private final ResultOutbox outbox;
    private final BackendClient backendClient;
    private final SessionManager sessionManager;
    private final ScheduledExecutorService executor;
    private final ObjectMapper objectMapper;

    public BackendSubmitter(ResultOutbox outbox, BackendClient backendClient, SessionManager sessionManager) {
        this.outbox = outbox;
        this.backendClient = backendClient;
        this.sessionManager = sessionManager;
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "BackendSubmitter"));
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public void start() {
        // Retry pending records from previous runs on startup (Requirement 9.21)
        executor.scheduleWithFixedDelay(this::processPendingOutbox, 2, 30, TimeUnit.SECONDS);
    }

    public void submitAsync(String matchId, Object saveMatchRequest) {
        // Save to local disk outbox first (Requirement 9.1)
        boolean savedLocally = outbox.savePendingResult(matchId, saveMatchRequest);

        executor.execute(() -> {
            try {
                String json = objectMapper.writeValueAsString(saveMatchRequest);
                attemptSubmission(matchId, json);
            } catch (Exception e) {
                logger.error("Error serializing match result for submission: {}", e.getMessage());
            }
        });
    }

    private void processPendingOutbox() {
        List<ResultOutbox.OutboxEntry> entries = outbox.getPendingResults();
        for (ResultOutbox.OutboxEntry entry : entries) {
            attemptSubmission(entry.matchId, entry.jsonPayload);
        }
    }

    private void attemptSubmission(String matchId, String jsonPayload) {
        try {
            HttpResponse<String> response = backendClient.saveMatch(jsonPayload);
            int statusCode = response.statusCode();

            if (statusCode == 200 || statusCode == 201) {
                logger.info("Successfully persisted match result for matchId: {}", matchId);
                outbox.removeResult(matchId);
                notifyResultSaved(matchId, response.body());
            } else if (statusCode == 409 || statusCode == 422) {
                // Permanent conflict/validation error (Requirement 9.18)
                logger.error("Permanent error ({}) for matchId: {}. Stopping retries.", statusCode, matchId);
                notifyResultSaveFailed(matchId, "PERMANENT_ERROR_" + statusCode);
            } else {
                // Transient error: retain for scheduled retry (Requirement 9.17)
                logger.warn("Transient error ({}) for matchId: {}. Will retry.", statusCode, matchId);
            }
        } catch (Exception e) {
            logger.warn("Network exception submitting matchId {}: {}. Will retry.", matchId, e.getMessage());
        }
    }

    private void notifyResultSaved(String matchId, String responseJson) {
        try {
            Map<?, ?> responseMap = objectMapper.readValue(responseJson, Map.class);
            List<?> playersList = (List<?>) responseMap.get("players");
            List<StandingsDTO> standings = new ArrayList<>();

            if (playersList != null) {
                for (Object item : playersList) {
                    Map<?, ?> p = (Map<?, ?>) item;
                    Long userId = ((Number) p.get("userId")).longValue();
                    Long totalScore = ((Number) p.get("totalScore")).longValue();
                    Integer matchesPlayed = ((Number) p.get("matchesPlayed")).intValue();
                    Integer matchesWon = ((Number) p.get("matchesWon")).intValue();
                    Integer matchesLost = ((Number) p.get("matchesLost")).intValue();
                    Integer matchesDrawn = ((Number) p.get("matchesDrawn")).intValue();
                    Double winRate = ((Number) p.get("winRate")).doubleValue();

                    standings.add(new StandingsDTO(userId, totalScore, matchesPlayed, matchesWon, matchesLost, matchesDrawn, winRate));

                    // Update session cached ranking score (Requirement 9.20)
                    Session session = sessionManager.getSessionByUserId(userId);
                    if (session != null) {
                        session.setRankingScore(totalScore);
                    }
                }
            }

            // Send RESULT_SAVED to connected participants
            ResultSavedPayload payload = new ResultSavedPayload(matchId, standings);
            Message message = Message.event(ActionType.RESULT_SAVED, payload);

            for (StandingsDTO s : standings) {
                Session session = sessionManager.getSessionByUserId(s.getUserId());
                if (session != null && session.getClientHandler() != null && session.getClientHandler().isConnected()) {
                    session.getClientHandler().send(message);
                }
            }

            sessionManager.broadcastOnlineList();

        } catch (Exception e) {
            logger.error("Error processing RESULT_SAVED notification: {}", e.getMessage());
        }
    }

    private void notifyResultSaveFailed(String matchId, String errorCode) {
        Message message = Message.event(ActionType.RESULT_SAVE_FAILED, Map.of("matchId", matchId, "code", errorCode));
        for (Session session : sessionManager.getAllSessions()) {
            if (session.getClientHandler() != null && session.getClientHandler().isConnected()) {
                session.getClientHandler().send(message);
            }
        }
    }

    public void stop() {
        executor.shutdown();
    }
}

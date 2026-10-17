package com.territorypaint.server.room;

import com.territorypaint.common.dto.*;
import com.territorypaint.common.payload.*;
import com.territorypaint.common.protocol.*;
import com.territorypaint.server.game.CellCaptureEngine;
import com.territorypaint.server.game.GameState;
import com.territorypaint.server.outbox.BackendSubmitter;
import com.territorypaint.server.session.Session;
import com.territorypaint.server.session.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/**
 * Encapsulates match room lifecycle and serialized event handling for two players.
 * Requirements: 5.1-5.15, 6.1-6.18, 7.1-7.10, 8.1-8.16, 14.6-14.8, 18.1-18.15
 */
public class GameRoom {

    private static final Logger logger = LoggerFactory.getLogger(GameRoom.class);

    private final String roomId;
    private final Session player1Session;
    private final Session player2Session;
    private final CellCaptureEngine captureEngine;
    private final BackendSubmitter backendSubmitter;
    private final SessionManager sessionManager;
    private final RoomRegistry roomRegistry;

    // Single-threaded serial executor for thread-safe state mutation (Requirement 14.6)
    private final ExecutorService roomExecutor;

    private volatile RoomStatus status = RoomStatus.WAITING_READY;
    private String currentMatchId;
    private GameState gameState;

    private boolean ready1 = false;
    private boolean ready2 = false;

    private boolean rematch1 = false;
    private boolean rematch2 = false;

    private Instant matchStartedAt;
    private Instant matchDeadlineAt;
    private ScheduledFuture<?> readyTimeoutTask;
    private ScheduledFuture<?> matchTimerTask;
    private ScheduledFuture<?> matchEndTask;
    private ScheduledFuture<?> rematchTimeoutTask;

    private boolean finalized = false; // Ensures match finalized at most once (Requirement 8.6)

    public GameRoom(String roomId, Session player1Session, Session player2Session,
                    BackendSubmitter backendSubmitter, SessionManager sessionManager,
                    RoomRegistry roomRegistry, ScheduledExecutorService scheduler) {
        this.roomId = roomId;
        this.player1Session = player1Session;
        this.player2Session = player2Session;
        this.backendSubmitter = backendSubmitter;
        this.sessionManager = sessionManager;
        this.roomRegistry = roomRegistry;
        this.captureEngine = new CellCaptureEngine();
        this.roomExecutor = Executors.newSingleThreadExecutor(r -> new Thread(r, "RoomExec-" + roomId));

        player1Session.setStatus(PlayerStatus.IN_ROOM);
        player1Session.setCurrentRoomId(roomId);
        player2Session.setStatus(PlayerStatus.IN_ROOM);
        player2Session.setCurrentRoomId(roomId);

        initMatch(scheduler);
    }

    public String getRoomId() {
        return roomId;
    }

    public RoomStatus getStatus() {
        return status;
    }

    private void initMatch(ScheduledExecutorService scheduler) {
        this.currentMatchId = UUID.randomUUID().toString();
        this.gameState = new GameState(roomId, currentMatchId, player1Session.getUserId(), player1Session.getUsername(),
                player2Session.getUserId(), player2Session.getUsername());
        this.ready1 = false;
        this.ready2 = false;
        this.finalized = false;

        Instant readyDeadline = Instant.now().plusSeconds(10); // Requirement 5.9: 10s readiness deadline
        SnapshotDTO snapshot = gameState.toSnapshotDTO(60000);

        MatchStartPayload payload = new MatchStartPayload(snapshot, readyDeadline);
        Message startMsg = Message.event(ActionType.MATCH_START, payload);

        sendToBoth(startMsg);

        // Schedule readiness deadline check (Requirement 5.13)
        readyTimeoutTask = scheduler.schedule(() -> submitEvent(() -> checkReadyTimeout()), 10, TimeUnit.SECONDS);
    }

    private void checkReadyTimeout() {
        if (status == RoomStatus.WAITING_READY) {
            closeRoom("READY_TIMEOUT");
        }
    }

    public void submitEvent(Runnable action) {
        if (status != RoomStatus.CLOSED) {
            roomExecutor.execute(action);
        }
    }

    public void handleReady(Session session, String matchId) {
        submitEvent(() -> {
            if (status != RoomStatus.WAITING_READY) {
                return;
            }
            if (matchId != null && !matchId.isBlank() && !currentMatchId.equals(matchId)) {
                logger.warn("Ready matchId mismatch: expected {}, got {}", currentMatchId, matchId);
                return;
            }

            if (session.getUserId().equals(player1Session.getUserId())) {
                ready1 = true;
                logger.info("Player 1 ({}) is READY in room {}", session.getUsername(), roomId);
            } else if (session.getUserId().equals(player2Session.getUserId())) {
                ready2 = true;
                logger.info("Player 2 ({}) is READY in room {}", session.getUsername(), roomId);
            }

            if (ready1 && ready2) {
                if (readyTimeoutTask != null) readyTimeoutTask.cancel(false);
                startMatch();
            }
        });
    }

    private void startMatch() {
        this.status = RoomStatus.PLAYING;
        player1Session.setStatus(PlayerStatus.PLAYING);
        player2Session.setStatus(PlayerStatus.PLAYING);

        this.matchStartedAt = Instant.now();
        this.matchDeadlineAt = matchStartedAt.plusSeconds(60); // Requirement 5.11, 7.1: 60s match

        MatchBeginPayload beginPayload = new MatchBeginPayload(currentMatchId, matchStartedAt, matchDeadlineAt, 60000L);
        sendToBoth(Message.event(ActionType.MATCH_BEGIN, beginPayload));

        // Schedule 1-second TIME_SYNC broadcasts (Requirement 7.3)
        ScheduledExecutorService scheduler = ConcurrentUtils.getScheduler();
        matchTimerTask = scheduler.scheduleAtFixedRate(() -> submitEvent(this::broadcastTimeSync), 1, 1, TimeUnit.SECONDS);
        matchEndTask = scheduler.schedule(() -> submitEvent(() -> finalizeTimeoutMatch()), 60, TimeUnit.SECONDS);

        sessionManager.broadcastOnlineList();
    }

    private void broadcastTimeSync() {
        if (status != RoomStatus.PLAYING) return;

        long remainingMs = Math.max(0, matchDeadlineAt.toEpochMilli() - System.currentTimeMillis());
        TimeSyncPayload payload = new TimeSyncPayload(currentMatchId, remainingMs, matchDeadlineAt);
        sendToBoth(Message.event(ActionType.TIME_SYNC, payload));
    }

    public void handleMove(Session session, String matchId, long seq, Direction direction) {
        submitEvent(() -> {
            if (status != RoomStatus.PLAYING || !currentMatchId.equals(matchId)) {
                return;
            }

            // Check authoritative match deadline (Requirement 7.7, 7.9)
            if (Instant.now().isAfter(matchDeadlineAt)) {
                finalizeTimeoutMatch();
                return;
            }

            int slot = session.getUserId().equals(player1Session.getUserId()) ? 1 : 2;

            // Sequence check (Requirement 6.7, 6.16, 6.17)
            if (!gameState.checkAndRecordSeq(slot, seq)) {
                sendToSession(session, Message.event(ActionType.MOVE_REJECTED,
                        new MoveRejectedPayload(matchId, seq, "STALE_SEQ", gameState.getPlayerPosition(slot), gameState.getStateVersion())));
                return;
            }

            // 100ms Cooldown check (Requirement 6.6)
            long nowMs = System.currentTimeMillis();
            if (!gameState.checkCooldown(slot, nowMs)) {
                sendToSession(session, Message.event(ActionType.MOVE_REJECTED,
                        new MoveRejectedPayload(matchId, seq, "MOVE_TOO_FAST", gameState.getPlayerPosition(slot), gameState.getStateVersion())));
                return;
            }

            // Apply movement & cell capture (Requirement 6.8-6.14)
            PositionDTO currentPos = gameState.getPlayerPosition(slot);
            CellCaptureEngine.MoveResult result = captureEngine.applyMove(gameState.getGrid(), currentPos, direction, slot, gameState.getTerritoryScores());

            if (!result.valid) {
                sendToSession(session, Message.event(ActionType.MOVE_REJECTED,
                        new MoveRejectedPayload(matchId, seq, result.errorCode, currentPos, gameState.getStateVersion())));
                return;
            }

            // Valid movement: update position & state version, broadcast MAP_UPDATE (Requirement 6.15)
            gameState.setPlayerPosition(slot, result.newPosition);
            gameState.incrementStateVersion();

            long remainingMs = Math.max(0, matchDeadlineAt.toEpochMilli() - nowMs);
            SnapshotDTO snapshot = gameState.toSnapshotDTO(remainingMs);
            MapUpdatePayload mapUpdatePayload = new MapUpdatePayload(snapshot, session.getUserId(), seq);

            sendToBoth(Message.event(ActionType.MAP_UPDATE, mapUpdatePayload));
        });
    }

    private void finalizeTimeoutMatch() {
        if (finalized) return;
        finalized = true;

        cancelMatchTimers();
        this.status = RoomStatus.FINISHING;

        int[] scores = gameState.getTerritoryScores();
        Long winnerUserId = null;
        MatchOutcome outcome1 = MatchOutcome.DRAW;
        MatchOutcome outcome2 = MatchOutcome.DRAW;

        if (scores[0] > scores[1]) {
            winnerUserId = player1Session.getUserId();
            outcome1 = MatchOutcome.WIN;
            outcome2 = MatchOutcome.LOSS;
        } else if (scores[1] > scores[0]) {
            winnerUserId = player2Session.getUserId();
            outcome1 = MatchOutcome.LOSS;
            outcome2 = MatchOutcome.WIN;
        }

        buildAndDeliverResult(winnerUserId, MatchEndReason.TIMEOUT, null, outcome1, outcome2, scores[0], scores[1]);
    }

    public void handlePlayerDisconnectOrForfeit(Session disconnectingSession) {
        submitEvent(() -> {
            if (status == RoomStatus.CLOSED) return;
            if (status == RoomStatus.WAITING_READY) {
                closeRoom("PLAYER_LEFT");
                return;
            }
            if (finalized) return;
            finalized = true;

            cancelMatchTimers();
            this.status = RoomStatus.FINISHING;

            Long forfeitedUserId = disconnectingSession.getUserId();
            Long winnerUserId = disconnectingSession.getUserId().equals(player1Session.getUserId()) ?
                    player2Session.getUserId() : player1Session.getUserId();

            MatchOutcome outcome1 = disconnectingSession.getUserId().equals(player1Session.getUserId()) ? MatchOutcome.LOSS : MatchOutcome.WIN;
            MatchOutcome outcome2 = disconnectingSession.getUserId().equals(player2Session.getUserId()) ? MatchOutcome.LOSS : MatchOutcome.WIN;

            int[] scores = gameState.getTerritoryScores();
            buildAndDeliverResult(winnerUserId, MatchEndReason.FORFEIT, forfeitedUserId, outcome1, outcome2, scores[0], scores[1]);
        });
    }

    private void buildAndDeliverResult(Long winnerUserId, MatchEndReason endReason, Long forfeitedUserId,
                                       MatchOutcome outcome1, MatchOutcome outcome2, int cells1, int cells2) {

        int delta1 = (outcome1 == MatchOutcome.WIN) ? 10 : (outcome1 == MatchOutcome.DRAW ? 3 : 0);
        int delta2 = (outcome2 == MatchOutcome.WIN) ? 10 : (outcome2 == MatchOutcome.DRAW ? 3 : 0);

        PlayerResultDTO pr1 = new PlayerResultDTO(player1Session.getUserId(), 1, cells1, outcome1, delta1);
        PlayerResultDTO pr2 = new PlayerResultDTO(player2Session.getUserId(), 2, cells2, outcome2, delta2);

        Instant rematchDeadline = Instant.now().plusSeconds(15);
        MatchResultPayload resultPayload = new MatchResultPayload(roomId, currentMatchId, winnerUserId, endReason,
                List.of(pr1, pr2), PersistenceStatus.PENDING, rematchDeadline);

        sendToBoth(Message.event(ActionType.MATCH_RESULT, resultPayload));

        // Submit match result to backend via ResultOutbox / BackendSubmitter (Requirement 9.1-9.5)
        List<PlayerEntryDTO> playerEntries = List.of(
                new PlayerEntryDTO(player1Session.getUserId(), 1, cells1),
                new PlayerEntryDTO(player2Session.getUserId(), 2, cells2)
        );
        Map<String, Object> saveRequest = Map.of(
                "matchId", currentMatchId,
                "roomId", roomId,
                "startedAt", matchStartedAt != null ? matchStartedAt.toString() : Instant.now().toString(),
                "endedAt", Instant.now().toString(),
                "endReason", endReason.name(),
                "players", playerEntries,
                "forfeitedUserId", forfeitedUserId != null ? forfeitedUserId : Optional.empty()
        );

        backendSubmitter.submitAsync(currentMatchId, saveRequest);

        // Prepare for rematch (Requirement 18.3)
        if (player1Session.getClientHandler().isConnected() && player2Session.getClientHandler().isConnected()) {
            this.status = RoomStatus.WAITING_REMATCH;
            player1Session.setStatus(PlayerStatus.IN_ROOM);
            player2Session.setStatus(PlayerStatus.IN_ROOM);
            rematch1 = false;
            rematch2 = false;

            rematchTimeoutTask = ConcurrentUtils.getScheduler().schedule(() -> submitEvent(() -> closeRoom("REMATCH_TIMEOUT")), 15, TimeUnit.SECONDS);
        } else {
            closeRoom("PLAYER_LEFT");
        }
    }

    public void handleRematch(Session session, boolean accept) {
        submitEvent(() -> {
            if (status != RoomStatus.WAITING_REMATCH) return;

            if (!accept) {
                closeRoom("DECLINED");
                return;
            }

            if (session.getUserId().equals(player1Session.getUserId())) {
                rematch1 = true;
            } else if (session.getUserId().equals(player2Session.getUserId())) {
                rematch2 = true;
            }

            List<Long> acceptedUserIds = new ArrayList<>();
            if (rematch1) acceptedUserIds.add(player1Session.getUserId());
            if (rematch2) acceptedUserIds.add(player2Session.getUserId());

            RematchStatusPayload statusPayload = new RematchStatusPayload(roomId, currentMatchId, acceptedUserIds, Instant.now().plusSeconds(15));
            sendToBoth(Message.event(ActionType.REMATCH_STATUS, statusPayload));

            if (rematch1 && rematch2) { // Both accepted rematch (Requirement 18.10)
                if (rematchTimeoutTask != null) rematchTimeoutTask.cancel(false);
                this.status = RoomStatus.WAITING_READY;
                initMatch(ConcurrentUtils.getScheduler());
            }
        });
    }

    public void handleExitMatch(Session session) {
        submitEvent(() -> {
            if (status == RoomStatus.CLOSED) return;
            if (status == RoomStatus.WAITING_REMATCH || status == RoomStatus.WAITING_READY) {
                closeRoom("PLAYER_LEFT");
            } else if (!finalized) {
                handlePlayerDisconnectOrForfeit(session);
            }
        });
    }

    public void closeRoom(String reason) {
        if (status == RoomStatus.CLOSED) return;
        this.status = RoomStatus.CLOSED;

        cancelMatchTimers();

        sendToBoth(Message.event(ActionType.ROOM_CLOSED, Map.of("roomId", roomId, "reason", reason)));

        player1Session.setStatus(PlayerStatus.IDLE);
        player1Session.setCurrentRoomId(null);
        player1Session.setPendingChallengeId(null);
        player2Session.setStatus(PlayerStatus.IDLE);
        player2Session.setCurrentRoomId(null);
        player2Session.setPendingChallengeId(null);

        roomRegistry.removeRoom(roomId);
        sessionManager.broadcastOnlineList();

        roomExecutor.shutdown();
    }

    private void cancelMatchTimers() {
        if (readyTimeoutTask != null) readyTimeoutTask.cancel(false);
        if (matchTimerTask != null) matchTimerTask.cancel(false);
        if (matchEndTask != null) matchEndTask.cancel(false);
        if (rematchTimeoutTask != null) rematchTimeoutTask.cancel(false);
    }

    private void sendToBoth(Message message) {
        sendToSession(player1Session, message);
        sendToSession(player2Session, message);
    }

    private void sendToSession(Session session, Message message) {
        if (session != null && session.getClientHandler() != null && session.getClientHandler().isConnected()) {
            session.getClientHandler().send(message);
        }
    }

    private static class PlayerEntryDTO {
        public final Long userId;
        public final int playerSlot;
        public final int cellsOwned;

        public PlayerEntryDTO(Long userId, int playerSlot, int cellsOwned) {
            this.userId = userId;
            this.playerSlot = playerSlot;
            this.cellsOwned = cellsOwned;
        }
    }
}

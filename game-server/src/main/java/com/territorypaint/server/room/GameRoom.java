package com.territorypaint.server.room;

import com.territorypaint.common.dto.*;
import com.territorypaint.common.dto.ActiveItemDTO;
import com.territorypaint.common.payload.*;
import com.territorypaint.common.payload.QuickChatBroadcastPayload;
import com.territorypaint.common.payload.QuickChatPayload;
import com.territorypaint.common.protocol.*;
import com.territorypaint.server.game.CellCaptureEngine;
import com.territorypaint.server.game.GameState;
import com.territorypaint.server.game.PowerUpItem;
import com.territorypaint.server.outbox.BackendSubmitter;
import com.territorypaint.server.session.Session;
import com.territorypaint.server.session.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

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
    private ScheduledFuture<?> matchCountdownTask;
    private ScheduledFuture<?> matchTimerTask;
    private ScheduledFuture<?> matchEndTask;
    private ScheduledFuture<?> rematchTimeoutTask;

    private boolean finalized = false; // Ensures match finalized at most once (Requirement 8.6)

    // ── Quick Chat ────────────────────────────────────────────────────────────
    /** Predefined quick-chat phrases accepted from clients (index-based to save bandwidth). */
    private static final String[] QUICK_CHAT_PHRASES = {
        "Cay thế nhờ!",
        "Nhường tí đi chứ!",
        "Làm ván nữa không?",
        "Trận này căng phết!",
        "Tô đè rồi nhé!"
    };
    /** Rate-limit: minimum ms between chat messages per player slot (2 seconds). */
    private static final long CHAT_COOLDOWN_MS = 2000;
    private long lastChatMs1 = 0;
    private long lastChatMs2 = 0;
    // ─────────────────────────────────────────────────────────────────────────

    // ── Power-Up Item System ──────────────────────────────────────────────────
    private static final int  MAX_ACTIVE_ITEMS   = 2;
    private static final long ITEM_DESPAWN_MS    = 10_000L;  // 10s auto-despawn
    private static final long SPEED_BOOST_MS     = 4_000L;   // 4s duration
    private static final long FREEZE_MS          = 2_000L;   // 2s duration
    private static final long SHIELD_MS          = 5_000L;   // 5s duration
    // Spawn weights: SPEED_BOOST=45, SHIELD=35, FREEZE=20
    private static final PowerUpType[] SPAWN_TABLE;
    static {
        SPAWN_TABLE = new PowerUpType[100];
        int i = 0;
        for (; i < 45; i++) SPAWN_TABLE[i] = PowerUpType.SPEED_BOOST;
        for (; i < 80; i++) SPAWN_TABLE[i] = PowerUpType.SHIELD;
        for (; i < 100; i++) SPAWN_TABLE[i] = PowerUpType.FREEZE;
    }
    private final AtomicLong itemIdCounter = new AtomicLong(1);
    private final Map<Long, PowerUpItem> activeItems = new LinkedHashMap<>();
    private ScheduledFuture<?> itemSpawnTask;
    private final Random rng = new Random();
    // ─────────────────────────────────────────────────────────────────────────

    // ── Reconnect Grace Period ─────────────────────────────────────────────────
    private static final int GRACE_SECONDS = 10;
    private volatile Long graceDisconnectedUserId = null;  // userId of player in grace period
    private ScheduledFuture<?> gracePeriodTask = null;      // fires after 10s if no reconnect
    // ──────────────────────────────────────────────────────────────────────────

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
            if (matchId != null && !matchId.isBlank() && currentMatchId != null && !currentMatchId.equalsIgnoreCase(matchId)) {
                logger.warn("Ready matchId mismatch: expected {}, got {}, proceeding for room {}", currentMatchId, matchId, roomId);
            }

            int readySlot = 0;
            if (session.getUserId() != null && player1Session.getUserId() != null && session.getUserId().longValue() == player1Session.getUserId().longValue()) {
                ready1 = true;
                readySlot = 1;
                logger.info("Player 1 ({}) is READY in room {}", session.getUsername(), roomId);
            } else if (session.getUserId() != null && player2Session.getUserId() != null && session.getUserId().longValue() == player2Session.getUserId().longValue()) {
                ready2 = true;
                readySlot = 2;
                logger.info("Player 2 ({}) is READY in room {}", session.getUsername(), roomId);
            }

            int readyCount = (ready1 ? 1 : 0) + (ready2 ? 1 : 0);
            boolean allReady = ready1 && ready2;

            ReadyStatusPayload readyStatus = new ReadyStatusPayload(
                    roomId, currentMatchId, session.getUserId(), readySlot, readyCount, allReady, ready1, ready2
            );
            sendToBoth(Message.event(ActionType.READY, readyStatus));

            if (allReady) {
                if (readyTimeoutTask != null) readyTimeoutTask.cancel(false);
                matchCountdownTask = ConcurrentUtils.getScheduler().schedule(() -> submitEvent(this::startMatch), 3, TimeUnit.SECONDS);
            }
        });
    }

    private void startMatch() {
        if (status != RoomStatus.WAITING_READY) return;
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

        // Schedule power-up spawning: first item at T+15s, then every 15-20s
        scheduleNextItemSpawn(scheduler, 15);

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
            long nowMs = System.currentTimeMillis();

            // ── Freeze check ───────────────────────────────────────────
            if (gameState.isFrozen(slot, nowMs)) {
                sendToSession(session, Message.event(ActionType.MOVE_REJECTED,
                        new MoveRejectedPayload(matchId, seq, "FROZEN", gameState.getPlayerPosition(slot), gameState.getStateVersion())));
                return;
            }

            // Sequence check (Requirement 6.7, 6.16, 6.17)
            if (!gameState.checkAndRecordSeq(slot, seq)) {
                sendToSession(session, Message.event(ActionType.MOVE_REJECTED,
                        new MoveRejectedPayload(matchId, seq, "STALE_SEQ", gameState.getPlayerPosition(slot), gameState.getStateVersion())));
                return;
            }

            // 100ms Cooldown check – Speed Boost reduces to 67ms (Requirement 6.6)
            if (!gameState.checkCooldown(slot, nowMs)) {
                sendToSession(session, Message.event(ActionType.MOVE_REJECTED,
                        new MoveRejectedPayload(matchId, seq, "MOVE_TOO_FAST", gameState.getPlayerPosition(slot), gameState.getStateVersion())));
                return;
            }

            // Apply movement & cell capture with shield awareness (Requirement 6.8-6.14)
            PositionDTO currentPos = gameState.getPlayerPosition(slot);
            CellCaptureEngine.MoveResult result = captureEngine.applyMove(gameState, currentPos, direction, slot, nowMs);

            if (!result.valid) {
                sendToSession(session, Message.event(ActionType.MOVE_REJECTED,
                        new MoveRejectedPayload(matchId, seq, result.errorCode, currentPos, gameState.getStateVersion())));
                return;
            }

            // Valid movement: update position & state version
            gameState.setPlayerPosition(slot, result.newPosition);
            gameState.incrementStateVersion();

            // ── Power-up pickup check ──────────────────────────────────
            checkItemPickup(slot, result.newPosition.getX(), result.newPosition.getY(), session, matchId, nowMs);

            // Broadcast MAP_UPDATE (Requirement 6.15)
            long remainingMs = Math.max(0, matchDeadlineAt.toEpochMilli() - nowMs);
            SnapshotDTO snapshot = gameState.toSnapshotDTO(remainingMs, buildActiveItemDTOs());
            MapUpdatePayload mapUpdatePayload = new MapUpdatePayload(snapshot, session.getUserId(), seq);

            sendToBoth(Message.event(ActionType.MAP_UPDATE, mapUpdatePayload));
        });
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Power-Up Item System
    // ──────────────────────────────────────────────────────────────────────────

    private void scheduleNextItemSpawn(ScheduledExecutorService scheduler, long delaySec) {
        if (itemSpawnTask != null && !itemSpawnTask.isDone()) itemSpawnTask.cancel(false);
        itemSpawnTask = scheduler.schedule(
                () -> submitEvent(this::trySpawnItem),
                delaySec, TimeUnit.SECONDS);
    }

    /** Called on the room executor — tries to spawn one item then schedules the next attempt. */
    private void trySpawnItem() {
        if (status != RoomStatus.PLAYING || finalized) return;
        if (activeItems.size() < MAX_ACTIVE_ITEMS) {
            spawnItem();
        }
        // Schedule next attempt: 15-20 seconds from now
        long nextDelay = 15 + rng.nextInt(6); // 15..20 inclusive
        scheduleNextItemSpawn(ConcurrentUtils.getScheduler(), nextDelay);
    }

    private void spawnItem() {
        // Pick a random unoccupied cell
        PositionDTO p1pos = gameState.getPlayerPosition(1);
        PositionDTO p2pos = gameState.getPlayerPosition(2);

        List<int[]> candidates = new ArrayList<>();
        for (int y = 0; y < 20; y++) {
            for (int x = 0; x < 20; x++) {
                final int fx = x, fy = y;
                boolean occupiedByP1 = (p1pos.getX() == fx && p1pos.getY() == fy);
                boolean occupiedByP2 = (p2pos.getX() == fx && p2pos.getY() == fy);
                boolean hasItem = activeItems.values().stream()
                        .anyMatch(it -> it.getX() == fx && it.getY() == fy);
                if (!occupiedByP1 && !occupiedByP2 && !hasItem) {
                    candidates.add(new int[]{fx, fy});
                }
            }
        }
        if (candidates.isEmpty()) return;

        int[] chosen = candidates.get(rng.nextInt(candidates.size()));
        PowerUpType type = SPAWN_TABLE[rng.nextInt(100)];
        long itemId = itemIdCounter.getAndIncrement();
        long despawnAtMs = System.currentTimeMillis() + ITEM_DESPAWN_MS;

        PowerUpItem item = new PowerUpItem(itemId, type, chosen[0], chosen[1], despawnAtMs);
        activeItems.put(itemId, item);

        // Schedule auto-despawn
        ScheduledFuture<?> despawnTask = ConcurrentUtils.getScheduler().schedule(
                () -> submitEvent(() -> despawnItem(itemId)),
                ITEM_DESPAWN_MS, TimeUnit.MILLISECONDS);
        item.setDespawnTask(despawnTask);

        // Broadcast ITEM_SPAWN
        ItemSpawnPayload spawnPayload = new ItemSpawnPayload(itemId, type, chosen[0], chosen[1], despawnAtMs);
        sendToBoth(Message.event(ActionType.ITEM_SPAWN, spawnPayload));
        logger.debug("Spawned item {} ({}) at ({},{}) in room {}", itemId, type, chosen[0], chosen[1], roomId);
    }

    private void despawnItem(long itemId) {
        PowerUpItem item = activeItems.remove(itemId);
        if (item == null) return; // already picked up
        if (item.getDespawnTask() != null) item.getDespawnTask().cancel(false);
        sendToBoth(Message.event(ActionType.ITEM_DESPAWN, new ItemDespawnPayload(itemId)));
        logger.debug("Despawned item {} in room {}", itemId, roomId);
    }

    /** Called after each valid move — checks if the player stepped on an item. */
    private void checkItemPickup(int slot, int x, int y, Session session, String matchId, long nowMs) {
        PowerUpItem item = activeItems.values().stream()
                .filter(it -> it.getX() == x && it.getY() == y)
                .findFirst().orElse(null);
        if (item == null) return;

        // Cancel despawn timer and remove from map
        if (item.getDespawnTask() != null) item.getDespawnTask().cancel(false);
        activeItems.remove(item.getItemId());

        int opponentSlot = (slot == 1) ? 2 : 1;
        long effectEndMs;
        int targetSlot = -1;

        switch (item.getType()) {
            case SPEED_BOOST -> {
                effectEndMs = nowMs + SPEED_BOOST_MS;
                gameState.applySpeedBoost(slot, effectEndMs);
                logger.debug("Speed Boost applied to slot {} in room {}", slot, roomId);
            }
            case SHIELD -> {
                effectEndMs = nowMs + SHIELD_MS;
                gameState.applyShield(slot, effectEndMs);
                // Schedule shield expiry to clear protected cells
                ConcurrentUtils.getScheduler().schedule(
                        () -> submitEvent(() -> {
                            gameState.clearProtectedCells(slot);
                            logger.debug("Shield expired for slot {} in room {}", slot, roomId);
                        }), SHIELD_MS, TimeUnit.MILLISECONDS);
                logger.debug("Shield applied to slot {} in room {}", slot, roomId);
            }
            case FREEZE -> {
                effectEndMs = nowMs + FREEZE_MS;
                boolean applied = gameState.applyFreeze(opponentSlot, effectEndMs, nowMs);
                targetSlot = applied ? opponentSlot : -1;
                logger.debug("Freeze {} for slot {} in room {}", applied ? "applied" : "blocked by Shield", opponentSlot, roomId);
            }
            default -> effectEndMs = nowMs;
        }

        // Broadcast ITEM_PICKUP
        ItemPickupPayload pickupPayload = new ItemPickupPayload(
                item.getItemId(), item.getType(),
                slot, session.getUserId(), session.getUsername(),
                targetSlot, effectEndMs);
        sendToBoth(Message.event(ActionType.ITEM_PICKUP, pickupPayload));
    }

    /** Build the list of ActiveItemDTOs from the current active-items map for snapshot embedding. */
    private List<ActiveItemDTO> buildActiveItemDTOs() {
        List<ActiveItemDTO> list = new ArrayList<>();
        for (PowerUpItem item : activeItems.values()) {
            list.add(new ActiveItemDTO(item.getItemId(), item.getType(), item.getX(), item.getY(), item.getDespawnAtMs()));
        }
        return list;
    }

    /** Cancel item-spawn scheduler and despawn all active items (called when match ends). */
    private void cancelItemSystem() {
        if (itemSpawnTask != null) { itemSpawnTask.cancel(false); itemSpawnTask = null; }
        for (PowerUpItem item : activeItems.values()) {
            if (item.getDespawnTask() != null) item.getDespawnTask().cancel(false);
        }
        activeItems.clear();
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

            // During WAITING_READY or already finished → close immediately (no grace)
            if (status == RoomStatus.WAITING_READY) {
                closeRoom("PLAYER_LEFT");
                return;
            }
            if (finalized) return;

            // ── Grace Period Logic ─────────────────────────────────────────────
            // If the match is actively playing, give the disconnecting player 10s to reconnect.
            if (status == RoomStatus.PLAYING && graceDisconnectedUserId == null) {
                graceDisconnectedUserId = disconnectingSession.getUserId();

                // Register in SessionManager so ClientHandler can find the room on re-login
                sessionManager.registerPendingReconnect(disconnectingSession.getUserId(), roomId);

                long graceDeadlineEpochMs = System.currentTimeMillis() + GRACE_SECONDS * 1000L;

                // Notify the opponent: opponent dropped, waiting for reconnect
                Session waitingSession = getOpponentSession(disconnectingSession);
                OpponentDisconnectedPayload notifyPayload = new OpponentDisconnectedPayload(
                        roomId, currentMatchId,
                        disconnectingSession.getUsername(),
                        GRACE_SECONDS, graceDeadlineEpochMs
                );
                sendToSession(waitingSession,
                        Message.event(ActionType.OPPONENT_DISCONNECTED, notifyPayload));

                logger.info("Grace period started for user {} in room {} ({}s)",
                        disconnectingSession.getUsername(), roomId, GRACE_SECONDS);

                // Schedule forfeit if no reconnect within GRACE_SECONDS
                gracePeriodTask = ConcurrentUtils.getScheduler().schedule(
                        () -> submitEvent(() -> forfeitGraceExpired(disconnectingSession.getUserId())),
                        GRACE_SECONDS, TimeUnit.SECONDS
                );
                return; // Do NOT forfeit yet
            }

            // Fallback: forfeit immediately (e.g. second disconnect or non-PLAYING state)
            forfeitNow(disconnectingSession);
        });
    }

    /** Called when the grace period timer fires with no reconnect. */
    private void forfeitGraceExpired(Long disconnectedUserId) {
        if (finalized || status == RoomStatus.CLOSED) return;
        if (!disconnectedUserId.equals(graceDisconnectedUserId)) return; // already reconnected

        sessionManager.removePendingReconnect(disconnectedUserId);
        graceDisconnectedUserId = null;

        // Determine which session object to forfeit with (use stored userId)
        Session forfeitSession = disconnectedUserId.equals(player1Session.getUserId())
                ? player1Session : player2Session;
        logger.info("Grace period expired for user {} in room {}. Forfeiting.",
                disconnectedUserId, roomId);
        forfeitNow(forfeitSession);
    }

    /** Immediately finalize as forfeit. */
    private void forfeitNow(Session disconnectingSession) {
        if (finalized) return;
        finalized = true;

        cancelMatchTimers();
        cancelGracePeriod();
        this.status = RoomStatus.FINISHING;

        Long forfeitedUserId = disconnectingSession.getUserId();
        Long winnerUserId = disconnectingSession.getUserId().equals(player1Session.getUserId())
                ? player2Session.getUserId() : player1Session.getUserId();

        MatchOutcome outcome1 = disconnectingSession.getUserId().equals(player1Session.getUserId())
                ? MatchOutcome.LOSS : MatchOutcome.WIN;
        MatchOutcome outcome2 = disconnectingSession.getUserId().equals(player2Session.getUserId())
                ? MatchOutcome.LOSS : MatchOutcome.WIN;

        int[] scores = gameState.getTerritoryScores();
        buildAndDeliverResult(winnerUserId, MatchEndReason.FORFEIT, forfeitedUserId,
                outcome1, outcome2, scores[0], scores[1]);
    }

    /**
     * Called by GameServer when a player successfully reconnects during grace period.
     * Swaps the session reference and resumes the match.
     */
    public void handleReconnect(Session newSession) {
        submitEvent(() -> {
            if (status != RoomStatus.PLAYING || finalized) return;
            if (!newSession.getUserId().equals(graceDisconnectedUserId)) return;

            logger.info("Player {} reconnected to room {} during grace period.",
                    newSession.getUsername(), roomId);

            // Cancel the grace period forfeit timer
            cancelGracePeriod();
            sessionManager.removePendingReconnect(newSession.getUserId());
            graceDisconnectedUserId = null;

            // Update the session reference inside player1 / player2 field.
            // Swap in the new ClientHandler so future sendToSession() uses the new TCP connection.
            boolean isPlayer1 = newSession.getUserId().equals(player1Session.getUserId());
            Session reconSession = isPlayer1 ? player1Session : player2Session;
            reconSession.setClientHandler(newSession.getClientHandler()); // ← key swap
            reconSession.setStatus(PlayerStatus.PLAYING);
            reconSession.setCurrentRoomId(roomId);

            // Send RECONNECT_SUCCESS with current board snapshot
            long remainingMs = Math.max(0, matchDeadlineAt.toEpochMilli() - System.currentTimeMillis());
            SnapshotDTO snapshot = gameState.toSnapshotDTO(remainingMs);
            ReconnectSuccessPayload successPayload = new ReconnectSuccessPayload(
                    roomId, currentMatchId, snapshot, remainingMs
            );
            newSession.getClientHandler().send(
                    Message.event(ActionType.RECONNECT_SUCCESS, successPayload)
            );

            // Notify opponent that they came back
            Session opponentSession = isPlayer1 ? player2Session : player1Session;
            sendToSession(opponentSession, Message.event(ActionType.ONLINE_LIST,
                    new OnlineListPayload(sessionManager.getOnlinePlayerDTOs())));
            opponentSession.getClientHandler().send(
                    Message.event(ActionType.MAP_UPDATE,
                            new MapUpdatePayload(snapshot, newSession.getUserId(), -1L))
            );

            logger.info("Reconnect complete for {} in room {}, {}ms remaining.",
                    newSession.getUsername(), roomId, remainingMs);
        });
    }

    /** Return the other player's session. */
    private Session getOpponentSession(Session session) {
        return session.getUserId().equals(player1Session.getUserId()) ? player2Session : player1Session;
    }

    private void buildAndDeliverResult(Long winnerUserId, MatchEndReason endReason, Long forfeitedUserId,
                                       MatchOutcome outcome1, MatchOutcome outcome2, int cells1, int cells2) {

        int delta1 = (outcome1 == MatchOutcome.WIN) ? 10 : (outcome1 == MatchOutcome.DRAW ? 3 : -5);
        int delta2 = (outcome2 == MatchOutcome.WIN) ? 10 : (outcome2 == MatchOutcome.DRAW ? 3 : -5);

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
        Map<String, Object> saveRequest = new HashMap<>();
        saveRequest.put("matchId", currentMatchId);
        saveRequest.put("roomId", roomId);
        saveRequest.put("startedAt", (matchStartedAt != null ? matchStartedAt : Instant.now()).toString());
        saveRequest.put("endedAt", Instant.now().toString());
        saveRequest.put("endReason", endReason.name());
        saveRequest.put("players", playerEntries);
        if (forfeitedUserId != null) {
            saveRequest.put("forfeitedUserId", forfeitedUserId);
        }

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

    /**
     * Handles a quick-chat message sent by a player during a match.
     * Validates the phrase index, applies a per-player rate limit, then broadcasts to both.
     */
    public void handleQuickChat(Session session, int phraseIndex, String matchId) {
        submitEvent(() -> {
            // Only allow chat while actively playing
            if (status != RoomStatus.PLAYING) return;
            if (currentMatchId == null || !currentMatchId.equals(matchId)) return;

            // Validate phrase index
            if (phraseIndex < 0 || phraseIndex >= QUICK_CHAT_PHRASES.length) {
                logger.warn("Invalid quick chat phrase index {} from user {}", phraseIndex, session.getUsername());
                return;
            }

            // Rate-limit check (2s cooldown per player)
            int slot = session.getUserId().equals(player1Session.getUserId()) ? 1 : 2;
            long now = System.currentTimeMillis();
            if (slot == 1) {
                if (now - lastChatMs1 < CHAT_COOLDOWN_MS) return;
                lastChatMs1 = now;
            } else {
                if (now - lastChatMs2 < CHAT_COOLDOWN_MS) return;
                lastChatMs2 = now;
            }

            String phrase = QUICK_CHAT_PHRASES[phraseIndex];
            QuickChatBroadcastPayload broadcast = new QuickChatBroadcastPayload(
                    session.getUserId(), session.getUsername(), slot, phrase);
            sendToBoth(Message.event(ActionType.QUICK_CHAT, broadcast));

            logger.debug("Quick chat from {} (slot {}): {}", session.getUsername(), slot, phrase);
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
        if (matchCountdownTask != null) matchCountdownTask.cancel(false);
        if (matchTimerTask != null) matchTimerTask.cancel(false);
        if (matchEndTask != null) matchEndTask.cancel(false);
        if (rematchTimeoutTask != null) rematchTimeoutTask.cancel(false);
        cancelItemSystem(); // Stop spawning and clear all active power-up items
    }

    private void cancelGracePeriod() {
        if (gracePeriodTask != null) {
            gracePeriodTask.cancel(false);
            gracePeriodTask = null;
        }
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

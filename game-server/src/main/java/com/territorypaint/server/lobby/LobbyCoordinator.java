package com.territorypaint.server.lobby;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.territorypaint.common.dto.Message;
import com.territorypaint.common.payload.*;
import com.territorypaint.common.protocol.ActionType;
import com.territorypaint.common.protocol.PlayerStatus;
import com.territorypaint.server.challenge.Challenge;
import com.territorypaint.server.challenge.ChallengeManager;
import com.territorypaint.server.outbox.BackendSubmitter;
import com.territorypaint.server.room.ConcurrentUtils;
import com.territorypaint.server.room.GameRoom;
import com.territorypaint.server.room.RoomRegistry;
import com.territorypaint.server.session.Session;
import com.territorypaint.server.session.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Single-threaded sequential coordinator for atomic lobby operations.
 * Requirements: 4.1-4.16, 14.4, 14.5
 */
public class LobbyCoordinator {

    private static final Logger logger = LoggerFactory.getLogger(LobbyCoordinator.class);

    private final ExecutorService lobbyExecutor;
    private final SessionManager sessionManager;
    private final ChallengeManager challengeManager;
    private final RoomRegistry roomRegistry;
    private final BackendSubmitter backendSubmitter;
    private final ObjectMapper objectMapper;

    public LobbyCoordinator(SessionManager sessionManager, ChallengeManager challengeManager,
                            RoomRegistry roomRegistry, BackendSubmitter backendSubmitter) {
        this.lobbyExecutor = Executors.newSingleThreadExecutor(r -> new Thread(r, "LobbyCoordinator"));
        this.sessionManager = sessionManager;
        this.challengeManager = challengeManager;
        this.roomRegistry = roomRegistry;
        this.backendSubmitter = backendSubmitter;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public void handleChallenge(Session challenger, Message message) {
        lobbyExecutor.execute(() -> {
            try {
                ChallengePayload payload = objectMapper.convertValue(message.getPayload(), ChallengePayload.class);
                if (payload == null || payload.getTargetUserId() <= 0) {
                    sendError(challenger, "INVALID_TARGET", "Target user ID required");
                    return;
                }

                if (challenger.getUserId().equals(payload.getTargetUserId())) {
                    sendError(challenger, "INVALID_TARGET", "Cannot challenge yourself");
                    return;
                }

                Session target = sessionManager.getSessionByUserId(payload.getTargetUserId());
                if (target == null || target.getStatus() != PlayerStatus.IDLE || !target.canChallenge()) {
                    sendError(challenger, "PLAYER_UNAVAILABLE", "Target player is not available");
                    return;
                }

                if (!challenger.canChallenge()) {
                    sendError(challenger, "PLAYER_UNAVAILABLE", "Challenger is not available");
                    return;
                }

                // Create challenge (Requirement 4.3)
                Challenge challenge = challengeManager.createChallenge(challenger, target);
                if (challenge == null) {
                    sendError(challenger, "PLAYER_UNAVAILABLE", "Failed to create challenge");
                    return;
                }

                // Send PENDING result to challenger and CHALLENGE_RECEIVED to target (Requirement 4.5)
                ChallengeResultPayload resultPayload = new ChallengeResultPayload(challenge.getChallengeId(), ChallengeResultPayload.ChallengeStatus.PENDING, null);
                challenger.getClientHandler().send(Message.reply(ActionType.CHALLENGE_RESULT, message.getRequestId(), resultPayload));

                ChallengeReceivedPayload receivedPayload = new ChallengeReceivedPayload(challenge.getChallengeId(), challenger.toPlayerDTO(), challenge.getExpiresAt());
                target.getClientHandler().send(Message.event(ActionType.CHALLENGE_RECEIVED, receivedPayload));

                // Schedule 15s challenge expiration task (Requirement 4.4)
                ConcurrentUtils.getScheduler().schedule(() -> lobbyExecutor.execute(() -> expireChallenge(challenge.getChallengeId())), 15, TimeUnit.SECONDS);

                sessionManager.broadcastOnlineList();

            } catch (Exception e) {
                logger.error("Error handling challenge: {}", e.getMessage(), e);
            }
        });
    }

    public void handleChallengeAccept(Session target, Message message) {
        lobbyExecutor.execute(() -> {
            try {
                ChallengeAcceptPayload payload = objectMapper.convertValue(message.getPayload(), ChallengeAcceptPayload.class);
                if (payload == null || payload.getChallengeId() == null) {
                    return;
                }

                Challenge challenge = challengeManager.getChallenge(payload.getChallengeId());

                // Revalidate challenge expiration, players' availability, and active sessions (Requirement 4.10, 14.5)
                if (challenge == null || challenge.getStatus() != Challenge.Status.PENDING || challenge.isExpired()) {
                    sendError(target, "CHALLENGE_EXPIRED", "Challenge is invalid or expired");
                    return;
                }

                if (!challenge.getTargetUserId().equals(target.getUserId())) {
                    sendError(target, "INVALID_TARGET", "Not authorized for this challenge");
                    return;
                }

                Session challenger = sessionManager.getSessionByUserId(challenge.getChallengerUserId());
                if (challenger == null || challenger.getStatus() != PlayerStatus.IDLE || target.getStatus() != PlayerStatus.IDLE) {
                    sendError(target, "PLAYER_UNAVAILABLE", "One of the players is no longer available");
                    challengeManager.releaseChallenge(challenge.getChallengeId());
                    return;
                }

                // Mark challenge accepted
                challenge.setStatus(Challenge.Status.ACCEPTED);
                challengeManager.releaseChallenge(challenge.getChallengeId());

                // Create GameRoom atomically (Requirement 14.5)
                String roomId = UUID.randomUUID().toString();
                GameRoom room = new GameRoom(roomId, challenger, target, backendSubmitter, sessionManager, roomRegistry, ConcurrentUtils.getScheduler());

                if (!roomRegistry.registerRoom(room)) {
                    sendError(target, "SERVER_BUSY", "Room capacity reached");
                    sendError(challenger, "SERVER_BUSY", "Room capacity reached");
                    challenger.setStatus(PlayerStatus.IDLE);
                    target.setStatus(PlayerStatus.IDLE);
                    return;
                }

                ChallengeResultPayload acceptResult = new ChallengeResultPayload(challenge.getChallengeId(), ChallengeResultPayload.ChallengeStatus.ACCEPTED, null);
                if (challenger.getClientHandler().isConnected()) challenger.getClientHandler().send(Message.event(ActionType.CHALLENGE_RESULT, acceptResult));
                if (target.getClientHandler().isConnected()) target.getClientHandler().send(Message.event(ActionType.CHALLENGE_RESULT, acceptResult));

                sessionManager.broadcastOnlineList();

            } catch (Exception e) {
                logger.error("Error handling challenge accept: {}", e.getMessage(), e);
            }
        });
    }

    public void handleChallengeReject(Session target, Message message) {
        lobbyExecutor.execute(() -> {
            try {
                ChallengeRejectPayload payload = objectMapper.convertValue(message.getPayload(), ChallengeRejectPayload.class);
                if (payload == null || payload.getChallengeId() == null) return;

                Challenge challenge = challengeManager.getChallenge(payload.getChallengeId());
                if (challenge != null && challenge.getTargetUserId().equals(target.getUserId())) {
                    challenge.setStatus(Challenge.Status.REJECTED);
                    notifyAndReleaseChallenge(challenge, ChallengeResultPayload.ChallengeStatus.REJECTED, "REJECTED");
                }
            } catch (Exception e) {
                logger.error("Error handling challenge reject: {}", e.getMessage());
            }
        });
    }

    public void handleChallengeCancel(Session challenger, Message message) {
        lobbyExecutor.execute(() -> {
            try {
                ChallengeCancelPayload payload = objectMapper.convertValue(message.getPayload(), ChallengeCancelPayload.class);
                if (payload == null || payload.getChallengeId() == null) return;

                Challenge challenge = challengeManager.getChallenge(payload.getChallengeId());
                if (challenge != null && challenge.getChallengerUserId().equals(challenger.getUserId())) {
                    challenge.setStatus(Challenge.Status.CANCELLED);
                    notifyAndReleaseChallenge(challenge, ChallengeResultPayload.ChallengeStatus.CANCELLED, "CANCELLED");
                }
            } catch (Exception e) {
                logger.error("Error handling challenge cancel: {}", e.getMessage());
            }
        });
    }

    private void expireChallenge(UUID challengeId) {
        Challenge challenge = challengeManager.getChallenge(challengeId);
        if (challenge != null && challenge.getStatus() == Challenge.Status.PENDING) {
            challenge.setStatus(Challenge.Status.EXPIRED);
            notifyAndReleaseChallenge(challenge, ChallengeResultPayload.ChallengeStatus.EXPIRED, "EXPIRED");
        }
    }

    private void notifyAndReleaseChallenge(Challenge challenge, ChallengeResultPayload.ChallengeStatus status, String reason) {
        ChallengeResultPayload payload = new ChallengeResultPayload(challenge.getChallengeId(), status, reason);
        Message msg = Message.event(ActionType.CHALLENGE_RESULT, payload);

        Session challenger = sessionManager.getSessionByUserId(challenge.getChallengerUserId());
        Session target = sessionManager.getSessionByUserId(challenge.getTargetUserId());

        if (challenger != null) {
            challenger.setPendingChallengeId(null);
            if (challenger.getClientHandler().isConnected()) challenger.getClientHandler().send(msg);
        }

        if (target != null) {
            target.setPendingChallengeId(null);
            if (target.getClientHandler().isConnected()) target.getClientHandler().send(msg);
        }

        challengeManager.releaseChallenge(challenge.getChallengeId());
        sessionManager.broadcastOnlineList();
    }

    private void sendError(Session session, String code, String msg) {
        if (session != null && session.getClientHandler() != null && session.getClientHandler().isConnected()) {
            session.getClientHandler().sendError(code, msg);
        }
    }

    public void stop() {
        lobbyExecutor.shutdown();
    }
}

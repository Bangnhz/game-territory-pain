package com.territorypaint.server.challenge;

import com.territorypaint.server.session.Session;
import com.territorypaint.server.session.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages active challenges and reservations.
 * Requirements: 4.2, 4.3, 4.4, 4.10, 4.11, 4.14, 4.15
 */
public class ChallengeManager {

    private static final Logger logger = LoggerFactory.getLogger(ChallengeManager.class);

    private final SessionManager sessionManager;
    private final Map<UUID, Challenge> challenges = new ConcurrentHashMap<>();
    private final Map<Long, UUID> playerChallenges = new ConcurrentHashMap<>();

    public ChallengeManager() {
        this(null);
    }

    public ChallengeManager(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    /**
     * Create a challenge if both players are available and have no pending challenge.
     * Requirements: 4.2, 4.14, 4.15
     */
    public synchronized Challenge createChallenge(Session challenger, Session target) {
        if (!challenger.canChallenge() || !target.canChallenge()) {
            return null;
        }

        if (playerChallenges.containsKey(challenger.getUserId()) || playerChallenges.containsKey(target.getUserId())) {
            return null;
        }

        Challenge challenge = new Challenge(challenger.getUserId(), target.getUserId(), 15);
        challenges.put(challenge.getChallengeId(), challenge);
        playerChallenges.put(challenger.getUserId(), challenge.getChallengeId());
        playerChallenges.put(target.getUserId(), challenge.getChallengeId());

        challenger.setPendingChallengeId(challenge.getChallengeId());
        target.setPendingChallengeId(challenge.getChallengeId());

        logger.info("Created challenge {} from user {} to user {}", challenge.getChallengeId(), challenger.getUserId(), target.getUserId());
        return challenge;
    }

    public Challenge getChallenge(UUID challengeId) {
        return challenges.get(challengeId);
    }

    public synchronized void releaseChallenge(UUID challengeId) {
        Challenge challenge = challenges.remove(challengeId);
        if (challenge != null) {
            playerChallenges.remove(challenge.getChallengerUserId());
            playerChallenges.remove(challenge.getTargetUserId());

            if (sessionManager != null) {
                Session challenger = sessionManager.getSessionByUserId(challenge.getChallengerUserId());
                if (challenger != null) {
                    challenger.setPendingChallengeId(null);
                }
                Session target = sessionManager.getSessionByUserId(challenge.getTargetUserId());
                if (target != null) {
                    target.setPendingChallengeId(null);
                }
            }
        }
    }

    public synchronized void cancelChallengeForPlayer(Long userId) {
        UUID challengeId = playerChallenges.get(userId);
        if (challengeId != null) {
            Challenge challenge = challenges.get(challengeId);
            if (challenge != null && challenge.getStatus() == Challenge.Status.PENDING) {
                challenge.setStatus(Challenge.Status.CANCELLED);
            }
            releaseChallenge(challengeId);
        }
    }
}

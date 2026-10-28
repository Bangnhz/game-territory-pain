package com.territorypaint.server.session;

import com.territorypaint.common.dto.Message;
import com.territorypaint.common.dto.PlayerDTO;
import com.territorypaint.common.payload.OnlineListPayload;
import com.territorypaint.common.protocol.ActionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages authenticated player sessions on the Game Server.
 * Requirements: 2.5, 2.8, 2.9, 2.10, 2.12, 2.13, 3.1, 3.2, 3.3, 3.4
 */
public class SessionManager {

    private static final Logger logger = LoggerFactory.getLogger(SessionManager.class);

    private final Map<String, Session> sessionsBySessionId = new ConcurrentHashMap<>();
    private final Map<Long, Session> sessionsByUserId = new ConcurrentHashMap<>();

    /**
     * Pending reconnect entries: userId → roomId.
     * Added when a player disconnects during a match grace period;
     * consumed when they successfully log in again within the grace window.
     */
    private final Map<Long, String> pendingReconnects = new ConcurrentHashMap<>();

    /**
     * Register a new authenticated session.
     * Requirements: 2.5, 2.9, 2.10
     *
     * @return true if session was registered, false if user is ALREADY_CONNECTED
     */
    public synchronized boolean registerSession(Session session) {
        if (sessionsByUserId.containsKey(session.getUserId())) {
            logger.warn("User {} already connected with active session", session.getUserId());
            return false;
        }

        sessionsBySessionId.put(session.getSessionId(), session);
        sessionsByUserId.put(session.getUserId(), session);
        logger.info("Session {} registered for user {} ({})", session.getSessionId(), session.getUsername(), session.getUserId());
        return true;
    }

    public Session getSessionBySessionId(String sessionId) {
        return sessionsBySessionId.get(sessionId);
    }

    public Session getSessionByUserId(Long userId) {
        return sessionsByUserId.get(userId);
    }

    /**
     * Remove session idempotently ensuring session ID matches.
     * Requirement: 2.13
     */
    public synchronized boolean removeSession(Session session) {
        if (session == null) {
            return false;
        }

        Session existing = sessionsByUserId.get(session.getUserId());
        if (existing != null && existing.getSessionId().equals(session.getSessionId())) {
            sessionsByUserId.remove(session.getUserId());
            sessionsBySessionId.remove(session.getSessionId());
            logger.info("Removed session {} for user {}", session.getSessionId(), session.getUserId());
            return true;
        }
        return false;
    }

    public Collection<Session> getAllSessions() {
        return Collections.unmodifiableCollection(sessionsBySessionId.values());
    }

    /**
     * Build ONLINE_LIST payload containing all active online players.
     * Requirement: 3.4
     */
    public List<PlayerDTO> getOnlinePlayerDTOs() {
        List<PlayerDTO> list = new ArrayList<>();
        for (Session session : sessionsBySessionId.values()) {
            list.add(session.toPlayerDTO());
        }
        return list;
    }

    /**
     * Broadcast ONLINE_LIST to all authenticated connected players.
     * Requirements: 3.1, 3.2, 3.3
     */
    public void broadcastOnlineList() {
        List<PlayerDTO> players = getOnlinePlayerDTOs();
        OnlineListPayload payload = new OnlineListPayload(players);
        Message message = Message.event(ActionType.ONLINE_LIST, payload);

        for (Session session : sessionsBySessionId.values()) {
            if (session.getClientHandler() != null && session.getClientHandler().isConnected()) {
                session.getClientHandler().send(message);
            }
        }
    }

    // =========================================================================
    // Reconnect Grace Period support
    // =========================================================================

    /**
     * Register that a player is in grace period and may reconnect to the given room.
     * Called by GameRoom when it starts a grace period instead of immediately forfeiting.
     */
    public void registerPendingReconnect(Long userId, String roomId) {
        pendingReconnects.put(userId, roomId);
        logger.info("Registered pending reconnect for userId={} roomId={}", userId, roomId);
    }

    /**
     * Consume and return the roomId a player should reconnect to.
     * Returns null if no pending reconnect exists for this user.
     * Removes the entry so it can only be consumed once.
     */
    public String consumePendingReconnect(Long userId) {
        return pendingReconnects.remove(userId);
    }

    /**
     * Cancel a pending reconnect entry (e.g. grace period expired).
     */
    public void removePendingReconnect(Long userId) {
        pendingReconnects.remove(userId);
    }
}

package com.territorypaint.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.territorypaint.common.GameConstants;
import com.territorypaint.common.dto.Message;
import com.territorypaint.common.payload.*;
import com.territorypaint.common.protocol.ActionType;
import com.territorypaint.server.challenge.ChallengeManager;
import com.territorypaint.server.lobby.LobbyCoordinator;
import com.territorypaint.server.net.BackendClient;
import com.territorypaint.server.net.ClientHandler;
import com.territorypaint.server.outbox.BackendSubmitter;
import com.territorypaint.server.outbox.ResultOutbox;
import com.territorypaint.server.room.ConcurrentUtils;
import com.territorypaint.server.room.GameRoom;
import com.territorypaint.server.room.RoomRegistry;
import com.territorypaint.server.session.Session;
import com.territorypaint.server.session.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Main TCP Game Server application.
 * Requirements: 2.1, 2.12, 14.1, 14.2, 14.4, 14.13
 */
public class GameServer {

    private static final Logger logger = LoggerFactory.getLogger(GameServer.class);

    private final int port;
    private final SessionManager sessionManager;
    private final ChallengeManager challengeManager;
    private final RoomRegistry roomRegistry;
    private final BackendClient backendClient;
    private final ResultOutbox outbox;
    private final BackendSubmitter backendSubmitter;
    private final LobbyCoordinator lobbyCoordinator;

    private final ExecutorService connectionExecutor;
    private final ScheduledExecutorService scheduler;
    private final ObjectMapper objectMapper;

    private ServerSocket serverSocket;
    private volatile boolean running = false;

    public GameServer(int port, String backendBaseUrl, String serviceKey, String outboxDirPath) {
        this.port = port;
        this.sessionManager = new SessionManager();
        this.challengeManager = new ChallengeManager(sessionManager);
        this.roomRegistry = new RoomRegistry();
        this.backendClient = new BackendClient(backendBaseUrl, serviceKey);
        this.outbox = new ResultOutbox(outboxDirPath);
        this.backendSubmitter = new BackendSubmitter(outbox, backendClient, sessionManager);
        this.lobbyCoordinator = new LobbyCoordinator(sessionManager, challengeManager, roomRegistry, backendSubmitter);

        this.connectionExecutor = Executors.newFixedThreadPool(GameConstants.MAX_CONNECTIONS); // Requirement 14.13: 100 connections
        this.scheduler = ConcurrentUtils.getScheduler();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        backendSubmitter.start();

        logger.info("Game Server started on port {}", port);

        Runtime.getRuntime().addShutdownHook(new Thread(this::stop, "GameServerShutdownHook"));

        acceptLoop();
    }

    private void acceptLoop() {
        while (running && !serverSocket.isClosed()) {
            try {
                Socket socket = serverSocket.accept();
                logger.info("Accepted new connection from {}", socket.getRemoteSocketAddress());
                ClientHandler handler = new ClientHandler(socket, this, sessionManager, backendClient);
                connectionExecutor.execute(handler);
            } catch (IOException e) {
                if (running) {
                    logger.error("Error accepting connection: {}", e.getMessage());
                }
            }
        }
    }

    public void dispatchClientMessage(Session session, Message message) {
        if (session == null || message == null || message.getActionType() == null) return;

        ActionType type = message.getActionType();

        // Lobby / Challenge Actions
        if (type == ActionType.CHALLENGE) {
            lobbyCoordinator.handleChallenge(session, message);
        } else if (type == ActionType.CHALLENGE_ACCEPT) {
            lobbyCoordinator.handleChallengeAccept(session, message);
        } else if (type == ActionType.CHALLENGE_REJECT) {
            lobbyCoordinator.handleChallengeReject(session, message);
        } else if (type == ActionType.CHALLENGE_CANCEL) {
            lobbyCoordinator.handleChallengeCancel(session, message);
        }
        // Room Gameplay Actions
        else if (type == ActionType.READY) {
            if (session.getCurrentRoomId() != null) {
                GameRoom room = roomRegistry.getRoom(session.getCurrentRoomId());
                if (room != null) {
                    try {
                        String matchIdStr = null;
                        if (message.getPayload() instanceof Map<?, ?> map) {
                            Object m = map.get("matchId");
                            if (m != null && !m.toString().isBlank()) {
                                matchIdStr = m.toString();
                            }
                        } else {
                            ReadyPayload p = objectMapper.convertValue(message.getPayload(), ReadyPayload.class);
                            if (p != null && p.getMatchId() != null) {
                                matchIdStr = p.getMatchId().toString();
                            }
                        }
                        room.handleReady(session, matchIdStr);
                    } catch (Exception e) {
                        logger.warn("Error parsing READY payload: {}", e.getMessage());
                        room.handleReady(session, null);
                    }
                }
            }
        } else if (type == ActionType.MOVE) {
            if (session.getCurrentRoomId() != null) {
                GameRoom room = roomRegistry.getRoom(session.getCurrentRoomId());
                if (room != null) {
                    try {
                        MovePayload p = objectMapper.convertValue(message.getPayload(), MovePayload.class);
                        room.handleMove(session, p.getMatchId() != null ? p.getMatchId().toString() : null, p.getSeq(), p.getDirection());
                    } catch (Exception e) {
                        logger.warn("Error parsing MOVE payload: {}", e.getMessage());
                    }
                }
            }
        } else if (type == ActionType.REMATCH) {
            if (session.getCurrentRoomId() != null) {
                GameRoom room = roomRegistry.getRoom(session.getCurrentRoomId());
                if (room != null) {
                    try {
                        RematchPayload p = objectMapper.convertValue(message.getPayload(), RematchPayload.class);
                        room.handleRematch(session, p.isAccept());
                    } catch (Exception e) {
                        logger.warn("Error parsing REMATCH payload: {}", e.getMessage());
                    }
                }
            }
        } else if (type == ActionType.EXIT_MATCH) {
            if (session.getCurrentRoomId() != null) {
                GameRoom room = roomRegistry.getRoom(session.getCurrentRoomId());
                if (room != null) {
                    room.handleExitMatch(session);
                }
            }
        }
    }

    public void handleClientDisconnect(Session session) {
        if (session == null) return;

        logger.info("Handling disconnect cleanup for session user {}", session.getUserId());

        // 1. Cancel pending challenges (Requirement 2.12)
        challengeManager.cancelChallengeForPlayer(session.getUserId());

        // 2. Notify active room if player belongs to one (Requirement 2.12)
        if (session.getCurrentRoomId() != null) {
            GameRoom room = roomRegistry.getRoom(session.getCurrentRoomId());
            if (room != null) {
                room.handlePlayerDisconnectOrForfeit(session);
            }
        }
    }

    public ScheduledExecutorService getScheduler() {
        return scheduler;
    }

    public void stop() {
        if (!running) return;
        running = false;
        logger.info("Stopping Game Server...");

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ignored) {}

        lobbyCoordinator.stop();
        backendSubmitter.stop();
        connectionExecutor.shutdownNow();

        logger.info("Game Server stopped.");
    }

    public static void main(String[] args) throws Exception {
        int port = GameConstants.DEFAULT_SERVER_PORT;
        String backendBaseUrl = System.getProperty("backend.url", "http://localhost:8080");
        String serviceKey = System.getProperty("service.key", "dev_service_key_secret_change_me");
        String outboxDir = System.getProperty("outbox.dir", "./outbox");

        GameServer server = new GameServer(port, backendBaseUrl, serviceKey, outboxDir);
        server.start();
    }
}

package com.territorypaint.server.net;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.territorypaint.common.GameConstants;
import com.territorypaint.common.dto.Message;
import com.territorypaint.common.payload.ErrorPayload;
import com.territorypaint.common.payload.LoginPayload;
import com.territorypaint.common.payload.LoginResultPayload;
import com.territorypaint.common.protocol.ActionType;
import com.territorypaint.server.GameServer;
import com.territorypaint.server.session.Session;
import com.territorypaint.server.session.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles TCP network communication and lifecycle for one client connection.
 * Requirements: 2.1, 2.2, 2.6, 2.7, 13.4-13.11, 14.10, 14.13, 14.15, 14.16, 16.6, 16.7
 */
public class ClientHandler implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);
    private static final int MAX_MESSAGE_SIZE = 65536; // Requirement 13.11

    private final Socket socket;
    private final GameServer server;
    private final SessionManager sessionManager;
    private final BackendClient backendClient;

    private final BlockingQueue<Message> outboundQueue = new ArrayBlockingQueue<>(256); // Requirement 14.13
    private final ObjectMapper objectMapper;
    private final AtomicBoolean connected = new AtomicBoolean(true);
    private final AtomicBoolean authenticated = new AtomicBoolean(false);

    private BufferedReader reader;
    private PrintWriter writer;
    private Thread writerThread;
    private Session session;

    private ScheduledFuture<?> authTimeoutTask;
    private ScheduledFuture<?> pingTask;
    private volatile Instant lastPongTime = Instant.now();
    private volatile UUID pendingPingNonce;
    private int malformedCount = 0; // Requirement 16.14

    // Rate limiting (Requirement 14.16: max 50 msg/sec in two consecutive 1-sec windows)
    private long currentWindowSecond = 0;
    private int currentWindowMsgCount = 0;
    private int consecutiveExcessiveWindows = 0;

    public ClientHandler(Socket socket, GameServer server, SessionManager sessionManager, BackendClient backendClient) {
        this.socket = socket;
        this.server = server;
        this.sessionManager = sessionManager;
        this.backendClient = backendClient;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void run() {
        try {
            socket.setTcpNoDelay(true);
            socket.setSoTimeout(0); // We handle timeout via Heartbeat scheduler

            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

            // Start dedicated outbound writer thread (Requirement 14.10)
            writerThread = new Thread(this::writerLoop, "ClientWriter-" + socket.getRemoteSocketAddress());
            writerThread.setDaemon(true);
            writerThread.start();

            // Schedule 5-second login timeout (Requirement 2.2, 2.7)
            authTimeoutTask = server.getScheduler().schedule(this::checkAuthTimeout, 5, TimeUnit.SECONDS);

            String line;
            while (connected.get() && (line = reader.readLine()) != null) {
                if (line.length() > MAX_MESSAGE_SIZE) {
                    logger.warn("Oversized message received from client ({} bytes). Closing connection.", line.length());
                    sendErrorAndClose("OVERSIZED_MESSAGE", "Message exceeds 65536 bytes");
                    return;
                }

                if (!checkRateLimit()) {
                    logger.warn("Client exceeded rate limit (50 msg/s for 2 consecutive windows). Closing.");
                    sendErrorAndClose("RATE_LIMIT_EXCEEDED", "Rate limit exceeded");
                    return;
                }

                handleIncomingLine(line);
            }
        } catch (IOException e) {
            if (connected.get()) {
                logger.info("Connection closed for client {}: {}", socket.getRemoteSocketAddress(), e.getMessage());
            }
        } finally {
            close();
        }
    }

    private void handleIncomingLine(String line) {
        try {
            Message message = objectMapper.readValue(line, Message.class);
            malformedCount = 0; // Reset consecutive malformed count on success

            if (!authenticated.get()) {
                if (message.getActionType() == ActionType.LOGIN) {
                    handleLogin(message);
                } else {
                    sendError("AUTH_REQUIRED", "Login required first");
                }
                return;
            }

            // Authenticated message handling
            if (message.getActionType() == ActionType.PONG) {
                handlePong(message);
                return;
            }

            if (message.getActionType() == ActionType.LOGOUT) {
                close();
                return;
            }

            // Dispatch gameplay / lobby message to server coordinator / room
            server.dispatchClientMessage(session, message);

        } catch (Exception e) {
            malformedCount++;
            logger.warn("Malformed JSON received from client (count {}): {}", malformedCount, e.getMessage());
            sendError("INVALID_JSON", "Malformed message payload");
            if (malformedCount >= 3) { // Requirement 16.14
                logger.warn("Three consecutive malformed messages. Closing connection.");
                close();
            }
        }
    }

    private void handleLogin(Message message) {
        try {
            LoginPayload payload = objectMapper.convertValue(message.getPayload(), LoginPayload.class);
            if (payload == null || payload.getAccessToken() == null) {
                sendError("INVALID_TOKEN", "Missing access token");
                close();
                return;
            }

            BackendClient.ValidationResult vr = backendClient.validateSession(payload.getAccessToken());
            if (!vr.valid) {
                sendError("INVALID_TOKEN", "Authentication failed");
                close();
                return;
            }

            // Check if user is already connected (Requirement 2.10)
            Session newSession = new Session(vr.userId, vr.username, vr.totalScore, vr.expiresAt, this);
            if (!sessionManager.registerSession(newSession)) {
                sendError("ALREADY_CONNECTED", "User is already connected in another session");
                close();
                return;
            }

            this.session = newSession;
            this.authenticated.set(true);
            if (authTimeoutTask != null) {
                authTimeoutTask.cancel(false);
            }

            // Send LOGIN_RESULT (Requirement 2.5)
            LoginResultPayload resultPayload = new LoginResultPayload(true, newSession.toPlayerDTO(), vr.expiresAt);
            send(Message.reply(ActionType.LOGIN_RESULT, message.getRequestId(), resultPayload));

            // Start heartbeat pinging (Requirement 16.6)
            startPingSchedule();

            // Broadcast ONLINE_LIST (Requirement 3.1)
            sessionManager.broadcastOnlineList();

        } catch (Exception e) {
            logger.error("Error processing login: {}", e.getMessage(), e);
            sendError("AUTH_FAILED", "Authentication failed");
            close();
        }
    }

    private void startPingSchedule() {
        pingTask = server.getScheduler().scheduleAtFixedRate(this::sendPingCheck, 5, 5, TimeUnit.SECONDS);
    }

    private void sendPingCheck() {
        if (!connected.get() || !authenticated.get()) return;

        // Check heartbeat timeout (Requirement 16.7: 15 seconds)
        if (Instant.now().isAfter(lastPongTime.plusSeconds(15))) {
            logger.warn("Client {} heartbeat timed out (no PONG in 15 seconds). Closing session.", session.getUsername());
            close();
            return;
        }

        UUID nonce = UUID.randomUUID();
        this.pendingPingNonce = nonce;
        send(Message.event(ActionType.PING, Map.of("nonce", nonce.toString())));
    }

    private void handlePong(Message message) {
        try {
            Map<?, ?> payload = objectMapper.convertValue(message.getPayload(), Map.class);
            if (payload != null && payload.get("nonce") != null) {
                String nonceStr = payload.get("nonce").toString();
                if (pendingPingNonce != null && pendingPingNonce.toString().equals(nonceStr)) {
                    this.lastPongTime = Instant.now();
                }
            }
        } catch (Exception e) {
            logger.warn("Invalid PONG received: {}", e.getMessage());
        }
    }

    private void checkAuthTimeout() {
        if (!authenticated.get() && connected.get()) {
            logger.warn("Client failed to authenticate within 5 seconds timeout. Closing.");
            sendErrorAndClose("AUTH_TIMEOUT", "Authentication timeout");
        }
    }

    private boolean checkRateLimit() {
        long currentSec = System.currentTimeMillis() / 1000;
        if (currentSec == currentWindowSecond) {
            currentWindowMsgCount++;
        } else {
            if (currentWindowMsgCount > 50) {
                consecutiveExcessiveWindows++;
            } else {
                consecutiveExcessiveWindows = 0;
            }
            currentWindowSecond = currentSec;
            currentWindowMsgCount = 1;
        }

        if (currentWindowMsgCount > 50 && consecutiveExcessiveWindows >= 1) {
            return false;
        }
        return true;
    }

    public void send(Message message) {
        if (!connected.get()) return;

        boolean added = outboundQueue.offer(message);
        if (!added) { // Requirement 14.15: Queue full -> disconnect slow client
            logger.warn("Outbound queue full for client {}. Disconnecting slow client.",
                    session != null ? session.getUsername() : socket.getRemoteSocketAddress());
            close();
        }
    }

    public void sendError(String code, String message) {
        ErrorPayload payload = new ErrorPayload(code, message);
        send(Message.event(ActionType.ERROR, payload));
    }

    public void sendErrorAndClose(String code, String message) {
        sendError(code, message);
        close();
    }

    private void writerLoop() {
        try {
            while (connected.get()) {
                Message message = outboundQueue.poll(500, TimeUnit.MILLISECONDS);
                if (message != null && connected.get()) {
                    String json = objectMapper.writeValueAsString(message);
                    writer.println(json);
                }
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.warn("Writer thread error for client {}: {}", socket.getRemoteSocketAddress(), e.getMessage());
        } finally {
            close();
        }
    }

    public boolean isConnected() {
        return connected.get();
    }

    public Session getSession() {
        return session;
    }

    public void close() {
        if (connected.compareAndSet(true, false)) {
            logger.info("Closing ClientHandler for {}", session != null ? session.getUsername() : socket.getRemoteSocketAddress());

            if (authTimeoutTask != null) authTimeoutTask.cancel(false);
            if (pingTask != null) pingTask.cancel(false);

            if (session != null) {
                server.handleClientDisconnect(session);
                sessionManager.removeSession(session);
                sessionManager.broadcastOnlineList();
            }

            try {
                if (writer != null) writer.close();
                if (reader != null) reader.close();
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException ignored) {}

            if (writerThread != null && writerThread != Thread.currentThread()) {
                writerThread.interrupt();
            }
        }
    }
}

package com.territorypaint.client.net;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.territorypaint.common.dto.Message;
import com.territorypaint.common.payload.LoginPayload;
import com.territorypaint.common.protocol.ActionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * TCP Network Client for real-time Game Server communication.
 * Requirements: 2.1, 2.2, 13.3, 13.5, 13.7
 */
public class TcpClient {

    private static final Logger logger = LoggerFactory.getLogger(TcpClient.class);

    private final String host;
    private final int port;
    private final ObjectMapper objectMapper;
    private final List<Consumer<Message>> messageListeners = new CopyOnWriteArrayList();

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private ExecutorService networkExecutor;
    private volatile boolean connected = false;

    public TcpClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public synchronized boolean connectAndLogin(String jwtToken) {
        try {
            this.socket = new Socket(host, port);
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            this.writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            this.connected = true;

            this.networkExecutor = Executors.newSingleThreadExecutor(r -> new Thread(r, "TcpClientReader"));
            networkExecutor.execute(this::readerLoop);

            // Requirement 2.2: Send LOGIN payload with JWT token
            Message loginMessage = Message.event(ActionType.LOGIN, new LoginPayload(jwtToken));
            send(loginMessage);

            return true;
        } catch (Exception e) {
            logger.error("Failed to connect to Game Server at {}:{} - {}", host, port, e.getMessage());
            disconnect();
            return false;
        }
    }

    public void send(Message message) {
        if (!connected || writer == null) return;
        try {
            String json = objectMapper.writeValueAsString(message);
            writer.println(json);
        } catch (Exception e) {
            logger.error("Error sending message: {}", e.getMessage());
        }
    }

    public void addMessageListener(Consumer<Message> listener) {
        messageListeners.add(listener);
    }

    public void removeMessageListener(Consumer<Message> listener) {
        messageListeners.remove(listener);
    }

    private void readerLoop() {
        try {
            String line;
            while (connected && (line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                try {
                    Message msg = objectMapper.readValue(line, Message.class);

                    // Requirement 13.7: Respond to server PING with PONG (echo payload/nonce)
                    if (msg.getActionType() == ActionType.PING) {
                        send(Message.event(ActionType.PONG, msg.getPayload()));
                        continue;
                    }

                    for (Consumer<Message> listener : messageListeners) {
                        try {
                            listener.accept(msg);
                        } catch (Exception ex) {
                            logger.error("Error in message listener: {}", ex.getMessage());
                        }
                    }
                } catch (Exception ex) {
                    logger.error("Error parsing message line: {}", ex.getMessage());
                }
            }
        } catch (Exception e) {
            logger.warn("TCP connection reading ended: {}", e.getMessage());
        } finally {
            disconnect();
        }
    }

    public synchronized void disconnect() {
        if (!connected) return;
        connected = false;

        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (Exception ignored) {}

        if (networkExecutor != null) {
            networkExecutor.shutdownNow();
        }

        logger.info("TCP Client disconnected");
    }

    public boolean isConnected() {
        return connected;
    }
}

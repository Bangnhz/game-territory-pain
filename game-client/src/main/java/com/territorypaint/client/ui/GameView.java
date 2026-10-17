package com.territorypaint.client.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.territorypaint.client.state.ClientState;
import com.territorypaint.common.dto.Message;
import com.territorypaint.common.dto.PlayerResultDTO;
import com.territorypaint.common.dto.PositionDTO;
import com.territorypaint.common.dto.SnapshotDTO;
import com.territorypaint.common.payload.*;
import com.territorypaint.common.protocol.ActionType;
import com.territorypaint.common.protocol.Direction;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Authoritative 20x20 Game Board Canvas View & Keyboard Input Controller.
 * Requirements: 5.4-5.15, 6.1-6.18, 7.1-7.10, 8.1-8.16
 */
public class GameView {

    private static final int GRID_SIZE = 20;
    private static final double CELL_SIZE = 26.0;

    private final SceneManager sceneManager;
    private final BorderPane root;
    private final ObjectMapper objectMapper;
    private final Consumer<Message> messageListener;

    private Canvas canvas;
    private Label timerLabel;
    private Label player1ScoreLabel;
    private Label player2ScoreLabel;
    private Label statusLabel;
    private Button readyButton;

    private long lastMoveTimeMs = 0;
    private SnapshotDTO latestSnapshot;
    private String matchId;
    private Dialog<ButtonType> resultDialog;

    public GameView(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
        this.root = new BorderPane();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.messageListener = this::handleServerMessage;

        initView();
        sceneManager.getTcpClient().addMessageListener(messageListener);
    }

    public Region getView() {
        return root;
    }

    public void resetView() {
        onMatchStart(null);
    }

    public void onMatchStart(MatchStartPayload payload) {
        if (resultDialog != null) {
            try {
                resultDialog.close();
            } catch (Exception ignored) {}
            resultDialog = null;
        }

        if (payload != null && payload.getSnapshot() != null) {
            this.latestSnapshot = payload.getSnapshot();
            this.matchId = payload.getSnapshot().getMatchId().toString();
            sceneManager.getClientState().setCurrentMatchId(matchId);
            sceneManager.getClientState().setCurrentSnapshot(latestSnapshot);
        } else {
            this.matchId = sceneManager.getClientState().getCurrentMatchId();
        }

        this.readyButton.setVisible(true);
        this.readyButton.setDisable(false);
        this.readyButton.setText("Click READY to Start");
        this.statusLabel.setText("Waiting for both players to click READY...");
        this.timerLabel.setText("60s");

        if (latestSnapshot != null) {
            renderSnapshot(latestSnapshot);
        } else {
            renderEmptyGrid();
        }
    }

    private void initView() {
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #0F172A;");

        // Top HUD Header
        HBox hud = new HBox(30);
        hud.setAlignment(Pos.CENTER);
        hud.setPadding(new Insets(10, 20, 15, 20));
        hud.setStyle("-fx-background-color: #1E293B; -fx-background-radius: 8;");

        player1ScoreLabel = new Label("Player 1 (Blue): 1");
        player1ScoreLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #38BDF8;");

        timerLabel = new Label("60s");
        timerLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #FACC15;");

        player2ScoreLabel = new Label("Player 2 (Orange): 1");
        player2ScoreLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #FB923C;");

        hud.getChildren().addAll(player1ScoreLabel, timerLabel, player2ScoreLabel);
        root.setTop(hud);

        // Center Game Board Canvas
        VBox centerBox = new VBox(15);
        centerBox.setAlignment(Pos.CENTER);

        statusLabel = new Label("Waiting for players...");
        statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #94A3B8;");

        canvas = new Canvas(GRID_SIZE * CELL_SIZE, GRID_SIZE * CELL_SIZE);
        canvas.setFocusTraversable(true);
        canvas.setOnKeyPressed(this::handleKeyPressed);

        readyButton = new Button("Click READY to Start");
        readyButton.setStyle("-fx-background-color: #10B981; -fx-font-size: 16px; -fx-padding: 10 30;");
        readyButton.setOnAction(e -> sendReady());

        centerBox.getChildren().addAll(statusLabel, canvas, readyButton);
        root.setCenter(centerBox);

        renderEmptyGrid();
    }

    private void sendReady() {
        readyButton.setDisable(true);
        readyButton.setText("READY Submitted!");
        statusLabel.setText("Waiting for opponent to get READY...");
        sceneManager.getTcpClient().send(Message.event(ActionType.READY, new ReadyPayload(matchId)));
    }

    private void handleKeyPressed(KeyEvent event) {
        if (latestSnapshot == null || matchId == null) return;

        Direction dir = null;
        if (event.getCode() == KeyCode.W || event.getCode() == KeyCode.UP) dir = Direction.UP;
        else if (event.getCode() == KeyCode.S || event.getCode() == KeyCode.DOWN) dir = Direction.DOWN;
        else if (event.getCode() == KeyCode.A || event.getCode() == KeyCode.LEFT) dir = Direction.LEFT;
        else if (event.getCode() == KeyCode.D || event.getCode() == KeyCode.RIGHT) dir = Direction.RIGHT;

        if (dir == null) return;

        // Requirement 6.6: Client-side 100ms move throttling
        long now = System.currentTimeMillis();
        if (now - lastMoveTimeMs < 100) return;
        lastMoveTimeMs = now;

        long seq = sceneManager.getClientState().nextMoveSeq();
        MovePayload payload = new MovePayload(matchId, seq, dir);
        sceneManager.getTcpClient().send(Message.event(ActionType.MOVE, payload));

        // Phát âm thanh nếu có thao tác di chuyển
        SoundManager.getInstance().playMoveSfx();
    }

    private void handleServerMessage(Message msg) {
        if (msg == null || msg.getActionType() == null) return;

        switch (msg.getActionType()) {
            case MATCH_START -> {
                MatchStartPayload payload = objectMapper.convertValue(msg.getPayload(), MatchStartPayload.class);
                if (payload != null) {
                    Platform.runLater(() -> onMatchStart(payload));
                }
            }
            case MATCH_BEGIN -> {
                Platform.runLater(() -> {
                    statusLabel.setText("MATCH STARTED! Use W/A/S/D or Arrow Keys to paint territory!");
                    readyButton.setVisible(false);
                    canvas.requestFocus();
                });
            }
            case MAP_UPDATE -> {
                MapUpdatePayload payload = objectMapper.convertValue(msg.getPayload(), MapUpdatePayload.class);
                if (payload != null && payload.getSnapshot() != null) {
                    Platform.runLater(() -> {
                        this.latestSnapshot = payload.getSnapshot();
                        renderSnapshot(latestSnapshot);
                    });
                }
            }
            case TIME_SYNC -> {
                TimeSyncPayload payload = objectMapper.convertValue(msg.getPayload(), TimeSyncPayload.class);
                if (payload != null) {
                    Platform.runLater(() -> {
                        long remainingSec = Math.max(0, payload.getRemainingMs() / 1000);
                        timerLabel.setText(remainingSec + "s");
                    });
                }
            }
            case MATCH_RESULT -> {
                MatchResultPayload payload = objectMapper.convertValue(msg.getPayload(), MatchResultPayload.class);
                if (payload != null) {
                    Platform.runLater(() -> showMatchResultDialog(payload));
                }
            }
            case REMATCH_STATUS -> {
                RematchStatusPayload payload = objectMapper.convertValue(msg.getPayload(), RematchStatusPayload.class);
                if (payload != null) {
                    Platform.runLater(() -> {
                        statusLabel.setText("Rematch accepted by: " + payload.getAcceptedUserIds());
                    });
                }
            }
            case ROOM_CLOSED -> {
                Platform.runLater(() -> {
                    if (resultDialog != null) {
                        try {
                            resultDialog.close();
                        } catch (Exception ignored) {}
                        resultDialog = null;
                    }
                    sceneManager.showLobbyScene();

                    String reason = null;
                    if (msg.getPayload() instanceof Map<?, ?> map) {
                        Object r = map.get("reason");
                        if (r != null) reason = r.toString();
                    }
                    if ("READY_TIMEOUT".equalsIgnoreCase(reason)) {
                        sceneManager.showAlert("Match Cancelled", "The match was cancelled because one or both players did not click READY in time (10s timeout).", Alert.AlertType.INFORMATION);
                    } else if ("PLAYER_LEFT".equalsIgnoreCase(reason) || "DISCONNECT".equalsIgnoreCase(reason) || "FORFEIT".equalsIgnoreCase(reason)) {
                        sceneManager.showAlert("Match Ended", "Opponent left the match or disconnected.", Alert.AlertType.INFORMATION);
                    }
                });
            }
        }
    }

    private void renderSnapshot(SnapshotDTO snapshot) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        int[][] grid = snapshot.getGrid();
        if (grid == null) return;

        // Draw 20x20 grid cells
        for (int y = 0; y < GRID_SIZE; y++) {
            for (int x = 0; x < GRID_SIZE; x++) {
                int owner = grid[y][x];
                if (owner == 1) {
                    gc.setFill(Color.web("#2672D6")); // Player 1 Blue
                } else if (owner == 2) {
                    gc.setFill(Color.web("#F28C28")); // Player 2 Orange
                } else {
                    gc.setFill(Color.web("#1E293B")); // Unowned Dark Cell
                }
                gc.fillRect(x * CELL_SIZE, y * CELL_SIZE, CELL_SIZE - 1, CELL_SIZE - 1);
            }
        }

        // Draw Player position markers and update HUD scores
        if (snapshot.getPlayers() != null) {
            for (var p : snapshot.getPlayers()) {
                if (p.getSlot() == 1) {
                    player1ScoreLabel.setText(p.getUsername() + " (Blue): " + p.getCellsOwned());
                } else {
                    player2ScoreLabel.setText(p.getUsername() + " (Orange): " + p.getCellsOwned());
                }

                PositionDTO pos = p.getPosition();
                if (pos != null) {
                    double px = pos.getX() * CELL_SIZE + CELL_SIZE / 2;
                    double py = pos.getY() * CELL_SIZE + CELL_SIZE / 2;

                    gc.setFill(p.getSlot() == 1 ? Color.WHITE : Color.YELLOW);
                    gc.fillOval(px - 8, py - 8, 16, 16);

                    gc.setStroke(Color.BLACK);
                    gc.setLineWidth(2);
                    gc.strokeOval(px - 8, py - 8, 16, 16);
                }
            }
        }
    }

    private void renderEmptyGrid() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.web("#1E293B"));
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        gc.setStroke(Color.web("#334155"));
        gc.setLineWidth(1);
        for (int i = 0; i <= GRID_SIZE; i++) {
            gc.strokeLine(i * CELL_SIZE, 0, i * CELL_SIZE, canvas.getHeight());
            gc.strokeLine(0, i * CELL_SIZE, canvas.getWidth(), i * CELL_SIZE);
        }
    }

    private void showMatchResultDialog(MatchResultPayload payload) {
        Dialog<ButtonType> dialog = new Dialog<>();
        this.resultDialog = dialog;

        Long currentUserId = sceneManager.getClientState().getUserId();
        String resultText = "DRAW";
        if (payload.getWinnerUserId() != null) {
            resultText = payload.getWinnerUserId().equals(currentUserId) ? "YOU WIN! 🎉" : "YOU LOST 😞";
        }

        dialog.setTitle("Match Finished");
        dialog.setHeaderText(resultText + " (Reason: " + payload.getEndReason() + ")");

        VBox contentBox = new VBox(10);
        contentBox.setPadding(new Insets(15));

        if (payload.getPlayerResults() != null) {
            for (PlayerResultDTO pr : payload.getPlayerResults()) {
                contentBox.getChildren().add(new Label(
                        "Player " + pr.getSlot() + " (ID: " + pr.getUserId() + "): " +
                                pr.getCellsOwned() + " cells | Outcome: " + pr.getOutcome() + " (+" + pr.getRankDelta() + " rating)"
                ));
            }
        }

        contentBox.getChildren().add(new Label("Persistence Status: " + payload.getPersistenceStatus()));

        ButtonType rematchButton = new ButtonType("Rematch", ButtonBar.ButtonData.OK_DONE);
        ButtonType exitButton = new ButtonType("Exit to Lobby", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(rematchButton, exitButton);
        dialog.getDialogPane().setContent(contentBox);

        dialog.showAndWait().ifPresent(res -> {
            if (res == rematchButton) {
                sceneManager.getTcpClient().send(Message.event(ActionType.REMATCH, Map.of("accept", true)));
                statusLabel.setText("Rematch requested. Waiting for opponent...");
            } else {
                sceneManager.getTcpClient().send(Message.event(ActionType.EXIT_MATCH, Map.of()));
                sceneManager.showLobbyScene();
            }
        });
    }
}

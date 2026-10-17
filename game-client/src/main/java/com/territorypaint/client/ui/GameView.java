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
import com.territorypaint.common.protocol.MatchEndReason;
import com.territorypaint.common.protocol.MatchOutcome;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Authoritative 20x20 Game Board Canvas View & Keyboard Input Controller.
 * Features Cyber-Paint Casual Arena styling and in-scene match finished dialog.
 * Requirements: 5.4-5.15, 6.1-6.18, 7.1-7.10, 8.1-8.16
 */
public class GameView {

    private static final int GRID_SIZE = 20;
    private static final double CELL_SIZE = 26.0;

    private final SceneManager sceneManager;
    private final StackPane root;
    private final BorderPane gameLayout;
    private final StackPane modalOverlay;
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

    // Rematch countdown & modal elements
    private ScheduledExecutorService resultCountdownScheduler;
    private ScheduledFuture<?> resultCountdownTask;
    private Label modalRematchStatusLabel;
    private Button modalRematchButton;

    // Preparation & Ready Overlay elements
    private StackPane prepOverlay;
    private VBox prepCard;
    private Label prepBadge;
    private Label prepTimerLabel;
    private Label prepSubtitleLabel;
    private Label prepStatusBadge;
    private Label prepCountdownNum;
    private ScheduledExecutorService prepCountdownScheduler;
    private ScheduledFuture<?> prepCountdownTask;
    private volatile boolean matchPlaying = false;

    public GameView(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
        this.root = new StackPane();
        this.gameLayout = new BorderPane();
        this.modalOverlay = new StackPane();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.messageListener = this::handleServerMessage;

        initView();
        sceneManager.getTcpClient().addMessageListener(messageListener);
    }

    public Region getView() {
        return root;
    }

    private void loadStylesheet() {
        try {
            var cssUrl = getClass().getResource("/css/lobby.css");
            if (cssUrl != null) {
                root.getStylesheets().add(cssUrl.toExternalForm());
            }
        } catch (Exception ignored) {}
    }

    public void resetView() {
        cancelPrepCountdown();
        matchPlaying = false;
        onMatchStart(null);
    }

    public void onMatchStart(MatchStartPayload payload) {
        closeResultModal();
        cancelPrepCountdown();
        matchPlaying = false;

        if (payload != null && payload.getSnapshot() != null) {
            this.latestSnapshot = payload.getSnapshot();
            this.matchId = payload.getSnapshot().getMatchId().toString();
            sceneManager.getClientState().setCurrentMatchId(matchId);
            sceneManager.getClientState().setCurrentSnapshot(latestSnapshot);
        } else {
            this.matchId = sceneManager.getClientState().getCurrentMatchId();
        }

        // Reset preparation overlay in center of board
        prepOverlay.setVisible(true);
        prepOverlay.setManaged(true);
        prepOverlay.setOpacity(1.0);

        prepBadge.setText("⚡ MATCH PREPARATION");
        prepBadge.setVisible(true);
        prepBadge.setManaged(true);

        prepTimerLabel.setText("10s");
        prepTimerLabel.setStyle("-fx-font-size: 38px; -fx-font-weight: 900; -fx-text-fill: #FACC15; -fx-effect: dropshadow(three-pass-box, rgba(250, 204, 21, 0.6), 16, 0, 0, 0);");
        prepTimerLabel.setVisible(true);
        prepTimerLabel.setManaged(true);

        prepSubtitleLabel.setVisible(true);
        prepSubtitleLabel.setManaged(true);

        prepStatusBadge.setText("0/2 PLAYERS READY");
        prepStatusBadge.setStyle(null);
        prepStatusBadge.setVisible(true);
        prepStatusBadge.setManaged(true);

        readyButton.setVisible(true);
        readyButton.setManaged(true);
        readyButton.setDisable(false);
        readyButton.setText("⚡ READY TO PLAY");
        readyButton.getStyleClass().removeAll("btn-ready-submitted");
        if (!readyButton.getStyleClass().contains("btn-ready-start")) {
            readyButton.getStyleClass().add("btn-ready-start");
        }

        prepCountdownNum.setVisible(false);
        prepCountdownNum.setManaged(false);
        prepCountdownNum.setText("");

        this.statusLabel.setText("Waiting for both players to click READY...");
        this.timerLabel.setText("60s");

        startPrep10sCountdown(10);

        if (latestSnapshot != null) {
            renderSnapshot(latestSnapshot);
        } else {
            renderEmptyGrid();
        }
    }

    private void initView() {
        loadStylesheet();

        root.setStyle("-fx-background-color: #0F172A;");
        gameLayout.setPadding(new Insets(20));

        // Top HUD Header
        HBox hud = new HBox(20);
        hud.setAlignment(Pos.CENTER);
        hud.setPadding(new Insets(10, 20, 10, 20));
        hud.setStyle("-fx-background-color: #1E293B; -fx-background-radius: 8;");

        player1ScoreLabel = new Label("Player 1 (Blue): 1");
        player1ScoreLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #38BDF8;");

        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);

        timerLabel = new Label("60s");
        timerLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #FACC15;");

        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        player2ScoreLabel = new Label("Player 2 (Orange): 1");
        player2ScoreLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #FB923C;");

        Button soundToggleBtn = SoundManager.getInstance().createSoundToggleButton();

        hud.getChildren().addAll(player1ScoreLabel, spacer1, timerLabel, spacer2, player2ScoreLabel, soundToggleBtn);
        gameLayout.setTop(hud);

        // Center Game Board Canvas & Centered Preparation Overlay
        canvas = new Canvas(GRID_SIZE * CELL_SIZE, GRID_SIZE * CELL_SIZE);
        canvas.setFocusTraversable(true);
        canvas.setOnKeyPressed(this::handleKeyPressed);

        prepOverlay = new StackPane();
        prepOverlay.setAlignment(Pos.CENTER);
        prepOverlay.setStyle("-fx-background-color: rgba(15, 23, 42, 0.72); -fx-background-radius: 12px;");
        prepOverlay.setMaxSize(GRID_SIZE * CELL_SIZE, GRID_SIZE * CELL_SIZE);

        prepCard = new VBox(14);
        prepCard.setAlignment(Pos.CENTER);
        prepCard.setMaxWidth(380);
        prepCard.getStyleClass().add("ready-prep-card");

        prepBadge = new Label("⚡ MATCH PREPARATION");
        prepBadge.getStyleClass().add("prep-badge");

        prepTimerLabel = new Label("10s");
        prepTimerLabel.setStyle("-fx-font-size: 38px; -fx-font-weight: 900; -fx-text-fill: #FACC15; -fx-effect: dropshadow(three-pass-box, rgba(250, 204, 21, 0.6), 16, 0, 0, 0);");

        prepSubtitleLabel = new Label("Both players must click READY to start");
        prepSubtitleLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #94A3B8; -fx-font-weight: 600;");

        prepStatusBadge = new Label("0/2 PLAYERS READY");
        prepStatusBadge.getStyleClass().add("prep-status-badge");

        readyButton = new Button("⚡ READY TO PLAY");
        readyButton.getStyleClass().add("btn-ready-start");
        addScaleHoverAnimation(readyButton, 1.05);
        readyButton.setOnAction(e -> sendReady());

        prepCountdownNum = new Label("");
        prepCountdownNum.getStyleClass().add("prep-countdown-num");
        prepCountdownNum.setVisible(false);
        prepCountdownNum.setManaged(false);

        prepCard.getChildren().addAll(prepBadge, prepTimerLabel, prepSubtitleLabel, prepStatusBadge, readyButton, prepCountdownNum);
        prepOverlay.getChildren().add(prepCard);

        StackPane boardStack = new StackPane();
        boardStack.setAlignment(Pos.CENTER);
        boardStack.getChildren().addAll(canvas, prepOverlay);

        VBox centerBox = new VBox(12);
        centerBox.setAlignment(Pos.CENTER);
        statusLabel = new Label("Match preparation phase. Click Ready when you are prepared.");
        statusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #94A3B8;");

        centerBox.getChildren().addAll(statusLabel, boardStack);
        gameLayout.setCenter(centerBox);

        // Modal Overlay for Match Finished dialog
        modalOverlay.getStyleClass().add("modal-overlay");
        modalOverlay.setAlignment(Pos.CENTER);
        modalOverlay.setVisible(false);
        modalOverlay.setManaged(false);

        root.getChildren().addAll(gameLayout, modalOverlay);

        renderEmptyGrid();
    }

    private void sendReady() {
        SoundManager.getInstance().playClickSfx();
        readyButton.setDisable(true);
        readyButton.setText("✅ READY SUBMITTED");
        readyButton.getStyleClass().removeAll("btn-ready-start");
        readyButton.getStyleClass().add("btn-ready-submitted");
        statusLabel.setText("Waiting for opponent to get READY...");
        prepStatusBadge.setText("1/2 PLAYERS READY (WAITING FOR OPPONENT...)");
        prepStatusBadge.setStyle("-fx-text-fill: #38BDF8;");

        UUID mId = null;
        try {
            if (matchId != null && !matchId.isBlank()) {
                mId = UUID.fromString(matchId);
            }
        } catch (Exception ignored) {}
        sceneManager.getTcpClient().send(Message.event(ActionType.READY, new ReadyPayload(mId)));
    }

    private void handleKeyPressed(KeyEvent event) {
        if (latestSnapshot == null || matchId == null || !matchPlaying) return;

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
            case READY -> {
                if (msg.getPayload() instanceof Map<?, ?> map) {
                    Object allReadyObj = map.get("allReady");
                    boolean allReady = Boolean.TRUE.equals(allReadyObj) || "true".equalsIgnoreCase(String.valueOf(allReadyObj));

                    Platform.runLater(() -> {
                        if (allReady) {
                            start3SecondCountdown();
                        } else {
                            if (readyButton.isDisabled()) {
                                prepStatusBadge.setText("1/2 PLAYERS READY (WAITING FOR OPPONENT...)");
                            } else {
                                prepStatusBadge.setText("1/2 PLAYERS READY (OPPONENT IS READY!)");
                                prepStatusBadge.setStyle("-fx-text-fill: #34D399; -fx-font-weight: 800;");
                            }
                        }
                    });
                }
            }
            case MATCH_BEGIN -> {
                Platform.runLater(() -> {
                    statusLabel.setText("MATCH STARTED! Use W/A/S/D or Arrow Keys to paint territory!");
                    timerLabel.setText("60s");
                    if (prepOverlay.isVisible() && !prepCountdownNum.isVisible()) {
                        start3SecondCountdown();
                    } else if (!prepOverlay.isVisible()) {
                        matchPlaying = true;
                        canvas.requestFocus();
                    }
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
                        updateRematchStatus(payload);
                    });
                }
            }
            case ROOM_CLOSED -> {
                Platform.runLater(() -> {
                    String reason = null;
                    if (msg.getPayload() instanceof Map<?, ?> map) {
                        Object r = map.get("reason");
                        if (r != null) reason = r.toString();
                    }
                    showMatchCancelledModal(reason);
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
        closeResultModal();

        Long currentUserId = sceneManager.getClientState().getUserId();
        Long winnerUserId = payload.getWinnerUserId();
        boolean isWinner = winnerUserId != null && winnerUserId.equals(currentUserId);
        boolean isLoser = winnerUserId != null && !winnerUserId.equals(currentUserId);
        boolean isDraw = winnerUserId == null;

        // Play feedback sound
        SoundManager.getInstance().playClickSfx();

        VBox dialogCard = new VBox(16);
        dialogCard.setAlignment(Pos.CENTER);
        dialogCard.setMaxWidth(520);
        dialogCard.setPadding(new Insets(24, 28, 24, 28));

        String cardThemeClass = isWinner ? "result-card-victory" : (isLoser ? "result-card-defeat" : "result-card-draw");
        dialogCard.getStyleClass().addAll("modal-card", cardThemeClass);

        // 1. Header category tag & End reason
        HBox topMetaBox = new HBox(12);
        topMetaBox.setAlignment(Pos.CENTER);

        Label arenaTag = new Label("🏁 MATCH COMPLETED");
        arenaTag.setStyle("-fx-text-fill: #38BDF8; -fx-font-size: 11px; -fx-font-weight: 800;");

        String reasonText;
        if (payload.getEndReason() == MatchEndReason.TIMEOUT) {
            reasonText = "⏱️ Time Limit (60s)";
        } else if (payload.getEndReason() == MatchEndReason.FORFEIT) {
            reasonText = "⚡ Opponent Forfeit / Disconnected";
        } else {
            reasonText = "🏁 " + (payload.getEndReason() != null ? payload.getEndReason() : "Finished");
        }
        Label reasonBadge = new Label(reasonText);
        reasonBadge.getStyleClass().add("result-reason-badge");

        topMetaBox.getChildren().addAll(arenaTag, reasonBadge);

        // 2. Banner Title & Subtitle
        VBox titleBox = new VBox(4);
        titleBox.setAlignment(Pos.CENTER);

        Label titleLabel = new Label();
        titleLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: 900;");

        Label subtitleLabel = new Label();
        subtitleLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 600;");

        if (isWinner) {
            titleLabel.setText("🏆 VICTORY!");
            titleLabel.setStyle(titleLabel.getStyle() + " -fx-text-fill: #34D399; -fx-effect: dropshadow(three-pass-box, rgba(52, 211, 153, 0.6), 16, 0, 0, 0);");
            subtitleLabel.setText("Territory Dominated! Exceptional battlefield control.");
            subtitleLabel.setStyle(subtitleLabel.getStyle() + " -fx-text-fill: #A7F3D0;");
        } else if (isLoser) {
            titleLabel.setText("💀 DEFEAT");
            titleLabel.setStyle(titleLabel.getStyle() + " -fx-text-fill: #F87171; -fx-effect: dropshadow(three-pass-box, rgba(248, 113, 113, 0.6), 16, 0, 0, 0);");
            subtitleLabel.setText("Outpainted this round! Regroup and challenge again.");
            subtitleLabel.setStyle(subtitleLabel.getStyle() + " -fx-text-fill: #FECACA;");
        } else {
            titleLabel.setText("🤝 DRAW GAME");
            titleLabel.setStyle(titleLabel.getStyle() + " -fx-text-fill: #38BDF8; -fx-effect: dropshadow(three-pass-box, rgba(56, 189, 248, 0.6), 16, 0, 0, 0);");
            subtitleLabel.setText("Deadlock! Both painters demonstrated equal prowess.");
            subtitleLabel.setStyle(subtitleLabel.getStyle() + " -fx-text-fill: #BAE6FD;");
        }
        titleBox.getChildren().addAll(titleLabel, subtitleLabel);

        // 3. Head-to-Head Comparison
        PlayerResultDTO p1Result = null;
        PlayerResultDTO p2Result = null;
        if (payload.getPlayerResults() != null) {
            for (PlayerResultDTO pr : payload.getPlayerResults()) {
                if (pr.getSlot() == 1) p1Result = pr;
                else if (pr.getSlot() == 2) p2Result = pr;
            }
        }

        String p1Name = "Player 1";
        String p2Name = "Player 2";
        if (latestSnapshot != null && latestSnapshot.getPlayers() != null) {
            for (var p : latestSnapshot.getPlayers()) {
                if (p.getSlot() == 1 && p.getUsername() != null && !p.getUsername().isBlank()) {
                    p1Name = p.getUsername();
                } else if (p.getSlot() == 2 && p.getUsername() != null && !p.getUsername().isBlank()) {
                    p2Name = p.getUsername();
                }
            }
        }

        boolean p1IsMe = (p1Result != null && currentUserId != null && currentUserId == p1Result.getUserId());
        boolean p2IsMe = (p2Result != null && currentUserId != null && currentUserId == p2Result.getUserId());
        if (!p1IsMe && !p2IsMe && latestSnapshot != null && latestSnapshot.getPlayers() != null && currentUserId != null) {
            for (var p : latestSnapshot.getPlayers()) {
                if (p.getUserId() == currentUserId) {
                    if (p.getSlot() == 1) p1IsMe = true;
                    else if (p.getSlot() == 2) p2IsMe = true;
                }
            }
        }

        int p1Cells = p1Result != null ? p1Result.getCellsOwned() : 0;
        int p2Cells = p2Result != null ? p2Result.getCellsOwned() : 0;
        double p1Pct = (p1Cells * 100.0) / 400.0;
        double p2Pct = (p2Cells * 100.0) / 400.0;
        boolean p1Winner = winnerUserId != null && p1Result != null && winnerUserId == p1Result.getUserId();
        boolean p2Winner = winnerUserId != null && p2Result != null && winnerUserId == p2Result.getUserId();

        VBox p1Box = createPlayerResultBox(1, p1Name, p1IsMe, p1Cells, p1Pct, p1Winner, p1Result != null ? p1Result.getRankDelta() : 0);
        VBox p2Box = createPlayerResultBox(2, p2Name, p2IsMe, p2Cells, p2Pct, p2Winner, p2Result != null ? p2Result.getRankDelta() : 0);

        Label vsBadge = new Label("VS");
        vsBadge.getStyleClass().add("result-vs-badge");

        HBox versusRow = new HBox(12);
        versusRow.setAlignment(Pos.CENTER);
        HBox.setHgrow(p1Box, Priority.ALWAYS);
        HBox.setHgrow(p2Box, Priority.ALWAYS);
        p1Box.setMaxWidth(Double.MAX_VALUE);
        p2Box.setMaxWidth(Double.MAX_VALUE);
        versusRow.getChildren().addAll(p1Box, vsBadge, p2Box);

        // 4. Territory Conquest Progress Bar
        VBox conquestBox = new VBox(6);
        conquestBox.setAlignment(Pos.CENTER);
        conquestBox.setPadding(new Insets(4, 0, 4, 0));

        HBox conquestBar = new HBox(2);
        conquestBar.setAlignment(Pos.CENTER_LEFT);
        conquestBar.getStyleClass().add("result-bar-container");
        conquestBar.setPrefHeight(16);
        conquestBar.setMaxWidth(460);

        double totalBarWidth = 450.0;
        double w1 = Math.max(p1Cells > 0 ? 8 : 0, totalBarWidth * (p1Cells / 400.0));
        double w2 = Math.max(p2Cells > 0 ? 8 : 0, totalBarWidth * (p2Cells / 400.0));
        double wNeutral = Math.max(0, totalBarWidth - w1 - w2);

        Region blueBar = new Region();
        blueBar.setStyle("-fx-background-color: #2672D6; -fx-background-radius: 6 0 0 6;");
        blueBar.setPrefWidth(w1);
        blueBar.setPrefHeight(10);

        Region neutralBar = new Region();
        neutralBar.setStyle("-fx-background-color: #334155;");
        neutralBar.setPrefWidth(wNeutral);
        neutralBar.setPrefHeight(10);

        Region orangeBar = new Region();
        orangeBar.setStyle("-fx-background-color: #F28C28; -fx-background-radius: 0 6 6 0;");
        orangeBar.setPrefWidth(w2);
        orangeBar.setPrefHeight(10);

        conquestBar.getChildren().addAll(blueBar, neutralBar, orangeBar);

        HBox legendBox = new HBox(18);
        legendBox.setAlignment(Pos.CENTER);
        Label blueLegend = new Label(String.format("🟦 Blue: %.1f%%", p1Pct));
        blueLegend.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #38BDF8;");

        double neutralPct = Math.max(0, 100.0 - p1Pct - p2Pct);
        Label neutralLegend = new Label(String.format("⬛ Neutral: %.1f%%", neutralPct));
        neutralLegend.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: #64748B;");

        Label orangeLegend = new Label(String.format("🟧 Orange: %.1f%%", p2Pct));
        orangeLegend.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #FB923C;");

        legendBox.getChildren().addAll(blueLegend, neutralLegend, orangeLegend);
        conquestBox.getChildren().addAll(conquestBar, legendBox);

        // 5. Rematch Countdown & Status
        VBox rematchSection = new VBox(6);
        rematchSection.setAlignment(Pos.CENTER);
        rematchSection.setPadding(new Insets(4, 0, 0, 0));

        ProgressBar countdownBar = new ProgressBar(1.0);
        countdownBar.getStyleClass().add("modal-countdown-bar");
        countdownBar.setPrefWidth(450);
        countdownBar.setPrefHeight(6);

        Label countdownLabel = new Label("⏳ Rematch window closes in 15s...");
        countdownLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #94A3B8;");

        modalRematchStatusLabel = new Label("Click Rematch to battle again with the same opponent.");
        modalRematchStatusLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #CBD5E1;");

        rematchSection.getChildren().addAll(countdownBar, countdownLabel, modalRematchStatusLabel);

        // 6. Action Buttons
        HBox buttonBox = new HBox(14);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(6, 0, 0, 0));

        modalRematchButton = new Button("⚔️ REQUEST REMATCH");
        modalRematchButton.getStyleClass().add("btn-result-rematch");
        addScaleHoverAnimation(modalRematchButton, 1.04);

        Button exitButton = new Button("🚪 EXIT TO LOBBY");
        exitButton.getStyleClass().add("btn-result-exit");
        addScaleHoverAnimation(exitButton, 1.04);

        modalRematchButton.setOnAction(e -> {
            SoundManager.getInstance().playClickSfx();
            modalRematchButton.setDisable(true);
            modalRematchButton.setText("⏳ REMATCH REQUESTED");
            modalRematchStatusLabel.setText("Rematch requested! Waiting for opponent to accept...");
            modalRematchStatusLabel.setStyle("-fx-text-fill: #38BDF8; -fx-font-weight: bold;");
            sceneManager.getTcpClient().send(Message.event(ActionType.REMATCH, Map.of("accept", true)));
        });

        exitButton.setOnAction(e -> {
            SoundManager.getInstance().playClickSfx();
            closeResultModal();
            sceneManager.getTcpClient().send(Message.event(ActionType.EXIT_MATCH, Map.of()));
            sceneManager.showLobbyScene();
        });

        buttonBox.getChildren().addAll(modalRematchButton, exitButton);

        // Assemble card
        dialogCard.getChildren().addAll(topMetaBox, titleBox, versusRow, conquestBox, rematchSection, buttonBox);

        // 7. Rematch 15-second Countdown Scheduler
        long deadlineMs = System.currentTimeMillis() + 15000;
        resultCountdownScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ResultCountdownThread");
            t.setDaemon(true);
            return t;
        });
        resultCountdownTask = resultCountdownScheduler.scheduleAtFixedRate(() -> {
            long remainingMs = deadlineMs - System.currentTimeMillis();
            if (remainingMs <= 0) {
                cancelResultCountdown();
                Platform.runLater(() -> {
                    if (modalOverlay.isVisible()) {
                        closeResultModal();
                        sceneManager.getTcpClient().send(Message.event(ActionType.EXIT_MATCH, Map.of()));
                        sceneManager.showLobbyScene();
                    }
                });
            } else {
                double progress = Math.max(0.0, (double) remainingMs / 15000.0);
                int seconds = (int) Math.ceil(remainingMs / 1000.0);
                Platform.runLater(() -> {
                    countdownBar.setProgress(progress);
                    if (seconds <= 5 && !countdownBar.getStyleClass().contains("modal-countdown-bar-danger")) {
                        countdownBar.getStyleClass().add("modal-countdown-bar-danger");
                    }
                    countdownLabel.setText("⏳ Rematch window closes in " + seconds + "s...");
                });
            }
        }, 0, 100, TimeUnit.MILLISECONDS);

        // 8. Entrance Animation
        modalOverlay.getChildren().setAll(dialogCard);
        modalOverlay.setVisible(true);
        modalOverlay.setManaged(true);

        dialogCard.setScaleX(0.85);
        dialogCard.setScaleY(0.85);
        dialogCard.setOpacity(0.0);

        ScaleTransition st = new ScaleTransition(Duration.millis(320), dialogCard);
        st.setToX(1.0);
        st.setToY(1.0);

        FadeTransition ft = new FadeTransition(Duration.millis(320), dialogCard);
        ft.setToValue(1.0);

        st.play();
        ft.play();
    }

    private VBox createPlayerResultBox(int slot, String name, boolean isMe, int cells, double pct, boolean isWinner, int rankDelta) {
        VBox box = new VBox(8);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add(slot == 1 ? "result-player-box-p1" : "result-player-box-p2");

        // Top row: circle + name + [YOU] + (WINNER badge)
        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Circle colorCircle = new Circle(6, slot == 1 ? Color.web("#38BDF8") : Color.web("#FB923C"));
        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: #F8FAFC;");

        topRow.getChildren().addAll(colorCircle, nameLabel);

        if (isMe) {
            Label meBadge = new Label("YOU");
            meBadge.setStyle(slot == 1
                    ? "-fx-background-color: rgba(56, 189, 248, 0.25); -fx-text-fill: #38BDF8; -fx-font-weight: 900; -fx-font-size: 10px; -fx-padding: 2 6; -fx-background-radius: 6;"
                    : "-fx-background-color: rgba(251, 146, 60, 0.25); -fx-text-fill: #FB923C; -fx-font-weight: 900; -fx-font-size: 10px; -fx-padding: 2 6; -fx-background-radius: 6;");
            topRow.getChildren().add(meBadge);
        }

        if (isWinner) {
            Label winBadge = new Label("👑 WINNER");
            winBadge.getStyleClass().add("result-winner-badge");
            topRow.getChildren().add(winBadge);
        }

        // Center row: Score & %
        HBox scoreRow = new HBox(8);
        scoreRow.setAlignment(Pos.BASELINE_LEFT);

        Label cellCountLabel = new Label(String.valueOf(cells));
        cellCountLabel.setStyle("-fx-font-size: 26px; -fx-font-weight: 900; -fx-text-fill: #FFFFFF;");

        Label cellUnitLabel = new Label("cells " + String.format("(%.1f%%)", pct));
        cellUnitLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #94A3B8;");

        scoreRow.getChildren().addAll(cellCountLabel, cellUnitLabel);

        // Bottom row: LP Delta badge
        Label lpBadge = new Label();
        if (rankDelta >= 0) {
            lpBadge.setText("+" + rankDelta + " LP");
            lpBadge.getStyleClass().add("result-lp-win");
        } else {
            lpBadge.setText(rankDelta + " LP");
            lpBadge.getStyleClass().add("result-lp-loss");
        }

        box.getChildren().addAll(topRow, scoreRow, lpBadge);
        return box;
    }

    private void updateRematchStatus(RematchStatusPayload payload) {
        if (payload == null || modalRematchStatusLabel == null) return;
        int count = payload.getAcceptedUserIds() != null ? payload.getAcceptedUserIds().size() : 0;
        Long myId = sceneManager.getClientState().getUserId();
        boolean iAccepted = payload.getAcceptedUserIds() != null && myId != null && payload.getAcceptedUserIds().contains(myId);

        if (count == 1) {
            if (iAccepted) {
                modalRematchStatusLabel.setText("You requested rematch! Waiting for opponent...");
                modalRematchStatusLabel.setStyle("-fx-text-fill: #34D399; -fx-font-weight: bold;");
            } else {
                modalRematchStatusLabel.setText("Opponent wants a rematch! Click Rematch to accept!");
                modalRematchStatusLabel.setStyle("-fx-text-fill: #FBBF24; -fx-font-weight: bold;");
            }
        } else if (count >= 2) {
            modalRematchStatusLabel.setText("Both players accepted! Starting new match...");
            modalRematchStatusLabel.setStyle("-fx-text-fill: #34D399; -fx-font-weight: bold;");
        }
    }

    private void closeResultModal() {
        cancelResultCountdown();
        cancelPrepCountdown();
        modalOverlay.getChildren().clear();
        modalOverlay.setVisible(false);
        modalOverlay.setManaged(false);
    }

    private void startPrep10sCountdown(int seconds) {
        cancelPrepCountdown();
        final int[] remaining = {seconds};
        prepCountdownScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "PrepCountdownThread");
            t.setDaemon(true);
            return t;
        });
        prepCountdownTask = prepCountdownScheduler.scheduleAtFixedRate(() -> {
            remaining[0]--;
            int sec = Math.max(0, remaining[0]);
            Platform.runLater(() -> {
                prepTimerLabel.setText(sec + "s");
                if (sec <= 3) {
                    prepTimerLabel.setStyle("-fx-font-size: 38px; -fx-font-weight: 900; -fx-text-fill: #EF4444; -fx-effect: dropshadow(three-pass-box, rgba(239, 68, 68, 0.6), 16, 0, 0, 0);");
                }
            });
            if (sec <= 0) {
                cancelPrepCountdown();
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    private void start3SecondCountdown() {
        cancelPrepCountdown();

        Platform.runLater(() -> {
            prepBadge.setText("🔥 BATTLE STARTS IN");
            prepTimerLabel.setVisible(false);
            prepTimerLabel.setManaged(false);
            prepSubtitleLabel.setVisible(false);
            prepSubtitleLabel.setManaged(false);
            prepStatusBadge.setVisible(false);
            prepStatusBadge.setManaged(false);
            readyButton.setVisible(false);
            readyButton.setManaged(false);

            prepCountdownNum.setVisible(true);
            prepCountdownNum.setManaged(true);
            prepCountdownNum.setStyle("-fx-font-size: 80px; -fx-font-weight: 900; -fx-text-fill: #FACC15; -fx-effect: dropshadow(three-pass-box, rgba(250, 204, 21, 0.7), 24, 0, 0, 0);");
            prepCountdownNum.setText("3");
            animateCountdownPulse(prepCountdownNum);
            SoundManager.getInstance().playMoveSfx();
        });

        final int[] step = {2};
        prepCountdownScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "BattleCountdownThread");
            t.setDaemon(true);
            return t;
        });

        prepCountdownTask = prepCountdownScheduler.scheduleAtFixedRate(() -> {
            int currentStep = step[0]--;
            Platform.runLater(() -> {
                if (currentStep == 2) {
                    prepCountdownNum.setText("2");
                    animateCountdownPulse(prepCountdownNum);
                    SoundManager.getInstance().playMoveSfx();
                } else if (currentStep == 1) {
                    prepCountdownNum.setText("1");
                    animateCountdownPulse(prepCountdownNum);
                    SoundManager.getInstance().playMoveSfx();
                } else if (currentStep <= 0) {
                    cancelPrepCountdown();
                    prepCountdownNum.setText("PAINT!");
                    prepCountdownNum.setStyle("-fx-font-size: 64px; -fx-font-weight: 900; -fx-text-fill: #10B981; -fx-effect: dropshadow(three-pass-box, rgba(16, 185, 129, 0.8), 24, 0, 0, 0);");
                    animateCountdownPulse(prepCountdownNum);
                    SoundManager.getInstance().playClickSfx();

                    FadeTransition ft = new FadeTransition(Duration.millis(350), prepOverlay);
                    ft.setFromValue(1.0);
                    ft.setToValue(0.0);
                    ft.setDelay(Duration.millis(300));
                    ft.setOnFinished(ev -> {
                        prepOverlay.setVisible(false);
                        prepOverlay.setManaged(false);
                        matchPlaying = true;
                        canvas.requestFocus();
                    });
                    ft.play();
                }
            });
        }, 1, 1, TimeUnit.SECONDS);
    }

    private void animateCountdownPulse(Node node) {
        if (node == null) return;
        node.setScaleX(1.4);
        node.setScaleY(1.4);
        ScaleTransition st = new ScaleTransition(Duration.millis(320), node);
        st.setToX(1.0);
        st.setToY(1.0);
        st.play();
    }

    private void cancelPrepCountdown() {
        if (prepCountdownTask != null && !prepCountdownTask.isDone()) {
            prepCountdownTask.cancel(true);
            prepCountdownTask = null;
        }
        if (prepCountdownScheduler != null && !prepCountdownScheduler.isShutdown()) {
            prepCountdownScheduler.shutdownNow();
            prepCountdownScheduler = null;
        }
    }

    private void cancelResultCountdown() {
        if (resultCountdownTask != null && !resultCountdownTask.isDone()) {
            resultCountdownTask.cancel(true);
            resultCountdownTask = null;
        }
        if (resultCountdownScheduler != null && !resultCountdownScheduler.isShutdown()) {
            resultCountdownScheduler.shutdownNow();
            resultCountdownScheduler = null;
        }
    }

    private void addScaleHoverAnimation(Node node, double scaleTo) {
        if (node == null) return;
        node.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(120), node);
            st.setToX(scaleTo);
            st.setToY(scaleTo);
            st.play();
        });
        node.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(120), node);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });
    }

    private void showMatchCancelledModal(String reason) {
        closeResultModal();
        SoundManager.getInstance().playClickSfx();

        VBox card = new VBox(18);
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(480);
        card.setPadding(new Insets(28, 32, 28, 32));
        card.getStyleClass().addAll("modal-card", "modal-card-cancelled");

        // 1. Top Badges
        HBox topBadges = new HBox(10);
        topBadges.setAlignment(Pos.CENTER);

        Label cancelBadge = new Label("⚠️ MATCH CANCELLED");
        cancelBadge.getStyleClass().add("modal-badge-cancelled");

        String subBadgeText;
        String descText;
        if ("READY_TIMEOUT".equalsIgnoreCase(reason)) {
            subBadgeText = "⏱️ 10s READY TIMEOUT";
            descText = "The preparation window timed out because one or both players did not click READY in time.";
        } else if ("PLAYER_LEFT".equalsIgnoreCase(reason) || "DISCONNECT".equalsIgnoreCase(reason) || "FORFEIT".equalsIgnoreCase(reason)) {
            subBadgeText = "⚡ OPPONENT LEFT";
            descText = "The match was aborted because the opponent disconnected or left the room.";
        } else if ("DECLINED".equalsIgnoreCase(reason)) {
            subBadgeText = "🚫 REMATCH DECLINED";
            descText = "The rematch challenge was declined by the opponent.";
        } else if ("REMATCH_TIMEOUT".equalsIgnoreCase(reason)) {
            subBadgeText = "⌛ REMATCH TIMEOUT";
            descText = "The 15-second rematch decision window has expired.";
        } else {
            subBadgeText = "⚠️ " + (reason != null ? reason : "ABORTED");
            descText = "The match session was terminated by the game server.";
        }

        Label subBadge = new Label(subBadgeText);
        subBadge.getStyleClass().add("result-reason-badge");
        topBadges.getChildren().addAll(cancelBadge, subBadge);

        // 2. Title & Description
        VBox titleBox = new VBox(8);
        titleBox.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("MATCH CANCELLED");
        titleLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: 900; -fx-text-fill: #FBBF24; -fx-effect: dropshadow(three-pass-box, rgba(245, 158, 11, 0.6), 16, 0, 0, 0);");

        Label descLabel = new Label(descText);
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(420);
        descLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #E2E8F0; -fx-font-weight: 600; -fx-text-alignment: center;");

        titleBox.getChildren().addAll(titleLabel, descLabel);

        // 3. LP Safety Badge
        Label safeLpBadge = new Label("🛡️ Rating Protected: No LP Gained or Deducted");
        safeLpBadge.getStyleClass().add("badge-safe-lp");

        // 4. Auto-redirect Countdown
        VBox redirectBox = new VBox(6);
        redirectBox.setAlignment(Pos.CENTER);

        ProgressBar redirectBar = new ProgressBar(1.0);
        redirectBar.getStyleClass().addAll("modal-countdown-bar", "modal-countdown-bar-danger");
        redirectBar.setPrefWidth(420);
        redirectBar.setPrefHeight(6);

        Label redirectLabel = new Label("⏳ Returning to lobby in 5s...");
        redirectLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #94A3B8; -fx-font-weight: 600;");

        redirectBox.getChildren().addAll(redirectBar, redirectLabel);

        // 5. Return Button
        Button returnBtn = new Button("🚪 RETURN TO LOBBY");
        returnBtn.getStyleClass().add("btn-cancelled-return");
        addScaleHoverAnimation(returnBtn, 1.04);
        returnBtn.setOnAction(e -> {
            SoundManager.getInstance().playClickSfx();
            closeResultModal();
            sceneManager.showLobbyScene();
        });

        card.getChildren().addAll(topBadges, titleBox, safeLpBadge, redirectBox, returnBtn);

        // 6. Schedule 5s Auto-Redirect
        long deadlineMs = System.currentTimeMillis() + 5000;
        resultCountdownScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "MatchCancelledCountdownThread");
            t.setDaemon(true);
            return t;
        });
        resultCountdownTask = resultCountdownScheduler.scheduleAtFixedRate(() -> {
            long remainingMs = deadlineMs - System.currentTimeMillis();
            if (remainingMs <= 0) {
                cancelResultCountdown();
                Platform.runLater(() -> {
                    if (modalOverlay.isVisible()) {
                        closeResultModal();
                        sceneManager.showLobbyScene();
                    }
                });
            } else {
                double progress = Math.max(0.0, (double) remainingMs / 5000.0);
                int seconds = (int) Math.ceil(remainingMs / 1000.0);
                Platform.runLater(() -> {
                    redirectBar.setProgress(progress);
                    redirectLabel.setText("⏳ Returning to lobby in " + seconds + "s...");
                });
            }
        }, 0, 100, TimeUnit.MILLISECONDS);

        // 7. Entrance Animation
        modalOverlay.getChildren().setAll(card);
        modalOverlay.setVisible(true);
        modalOverlay.setManaged(true);

        card.setScaleX(0.85);
        card.setScaleY(0.85);
        card.setOpacity(0.0);

        ScaleTransition st = new ScaleTransition(Duration.millis(260), card);
        st.setToX(1.0);
        st.setToY(1.0);

        FadeTransition ft = new FadeTransition(Duration.millis(260), card);
        ft.setToValue(1.0);

        st.play();
        ft.play();
    }
}

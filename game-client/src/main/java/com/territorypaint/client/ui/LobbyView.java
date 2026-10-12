package com.territorypaint.client.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.territorypaint.client.state.ClientState;
import com.territorypaint.common.dto.Message;
import com.territorypaint.common.dto.PlayerDTO;
import com.territorypaint.common.payload.*;
import com.territorypaint.common.protocol.ActionType;
import com.territorypaint.common.protocol.PlayerStatus;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Modern "Cyber-Paint Casual Arena" Lobby View.
 * Requirements: 3.1-3.9, 4.1-4.16
 * 
 * Architecture:
 * 1. Top Navbar: Game Logo + Glow effect + Arcade navigation buttons.
 * 2. Left Column: Glassmorphism Player Profile Card (Avatar, Score, Win Rate & Stats).
 * 3. Right Column: Cyber Arena Online Players List with dynamically generated Player Cards.
 */
public class LobbyView {

    private final SceneManager sceneManager;
    private final BorderPane root;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Consumer<Message> messageListener;

    // Online players data & container
    private final List<PlayerDTO> playerList = new ArrayList<>();
    private VBox playerListContainer;
    private Label onlineCountBadge;

    // Profile UI components
    private Label profileAvatarText;
    private Label profileUsernameLabel;
    private Label profileRatingLabel;
    private Label statMatchesLabel;
    private Label statWinsLabel;
    private Label statLossesLabel;
    private Label statWinRateLabel;
    private ProgressBar winRateBar;

    // Challenge dialog & timers
    private Dialog<ButtonType> activeChallengeDialog;
    private ScheduledExecutorService countdownScheduler;
    private ScheduledFuture<?> countdownTask;

    // Avatar palette generator
    private static final String[] AVATAR_PALETTE = {
            "#0284C7", // Cyan Neon
            "#FF7A00", // Fire Orange
            "#8B5CF6", // Cyber Purple
            "#10B981", // Emerald Neon
            "#EC4899", // Neon Pink
            "#F59E0B"  // Amber
    };

    public LobbyView(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
        this.root = new BorderPane();
        this.messageListener = this::handleServerMessage;

        loadStylesheet();
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
        } catch (Exception ignored) {
        }
    }

    private void initView() {
        root.getStyleClass().add("lobby-root");
        root.setPadding(new Insets(24));

        // 1. Top Navbar
        root.setTop(buildHeader());

        // Main Center Content: Split into Left Profile Column & Right Online Arena Column
        HBox contentBox = new HBox(24);
        contentBox.setAlignment(Pos.TOP_LEFT);
        contentBox.setPadding(new Insets(20, 0, 0, 0));

        VBox leftColumn = buildProfilePanel();
        VBox rightColumn = buildPlayerListPanel();

        HBox.setHgrow(rightColumn, Priority.ALWAYS);

        contentBox.getChildren().addAll(leftColumn, rightColumn);
        root.setCenter(contentBox);
    }

    // =========================================================================
    // 1. TOP NAVBAR COMPONENT
    // =========================================================================
    private HBox buildHeader() {
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 14, 0));

        // Game Brand Logo
        Label logoTerritorial = new Label("TERRITORIAL");
        logoTerritorial.getStyleClass().add("logo-title-territorial");

        Label logoPaint = new Label("PAINT");
        logoPaint.getStyleClass().add("logo-title-paint");

        Label arenaBadge = new Label("CASUAL ARENA");
        arenaBadge.getStyleClass().add("logo-badge");

        // Glow drop-shadow effect on brand
        DropShadow glow = new DropShadow();
        glow.setColor(Color.web("#00D2FF", 0.5));
        glow.setRadius(14);
        glow.setSpread(0.2);
        logoTerritorial.setEffect(glow);

        HBox logoBox = new HBox(6, logoTerritorial, logoPaint, arenaBadge);
        logoBox.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Arcade Navigation Buttons
        Button leaderboardBtn = createNavButton("🏆 Leaderboard", false);
        leaderboardBtn.setOnAction(e -> sceneManager.showLeaderboardScene());

        Button historyBtn = createNavButton("📜 Match History", false);
        historyBtn.setOnAction(e -> sceneManager.showHistoryScene());

        Button refreshBtn = createNavButton("🔄 Refresh", false);
        refreshBtn.setOnAction(e -> refreshData());

        Button logoutBtn = createNavButton("🚪 Logout", true);
        logoutBtn.setOnAction(e -> handleLogout());

        header.getChildren().addAll(logoBox, spacer, leaderboardBtn, historyBtn, refreshBtn, logoutBtn);
        return header;
    }

    private Button createNavButton(String text, boolean isDanger) {
        Button btn = new Button(text);
        btn.getStyleClass().add(isDanger ? "nav-btn-danger" : "nav-btn");
        addScaleHoverAnimation(btn, 1.04);
        return btn;
    }

    // =========================================================================
    // 2. CỘT TRÁI - THẺ PROFILE NGƯỜI CHƠI (PLAYER PROFILE CARD)
    // =========================================================================
    private VBox buildProfilePanel() {
        VBox profileCard = new VBox(16);
        profileCard.getStyleClass().add("glass-card");
        profileCard.setPrefWidth(330);
        profileCard.setMinWidth(300);
        profileCard.setMaxWidth(360);
        profileCard.setPadding(new Insets(24));
        profileCard.setAlignment(Pos.TOP_CENTER);

        // Card Header Title
        HBox cardHeader = new HBox(8);
        cardHeader.setAlignment(Pos.CENTER_LEFT);
        cardHeader.getStyleClass().add("glass-card-header");

        Circle statusDot = new Circle(4, Color.web("#10B981"));
        DropShadow dotGlow = new DropShadow(8, Color.web("#10B981"));
        statusDot.setEffect(dotGlow);

        Label headerTitle = new Label("PLAYER PROFILE");
        headerTitle.getStyleClass().add("profile-title");
        cardHeader.getChildren().addAll(statusDot, headerTitle);

        // Avatar Component
        StackPane avatarPane = new StackPane();
        Circle outerRing = new Circle(44);
        outerRing.setFill(Color.TRANSPARENT);
        outerRing.setStroke(Color.web("#00D2FF", 0.6));
        outerRing.setStrokeWidth(2.5);

        DropShadow ringGlow = new DropShadow(12, Color.web("#00D2FF", 0.45));
        outerRing.setEffect(ringGlow);

        Circle innerCircle = new Circle(38);
        LinearGradient avatarGradient = new LinearGradient(
                0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#00D2FF")),
                new Stop(1, Color.web("#2563EB"))
        );
        innerCircle.setFill(avatarGradient);

        profileAvatarText = new Label("P");
        profileAvatarText.setStyle("-fx-font-size: 32px; -fx-font-weight: 900; -fx-text-fill: white;");

        avatarPane.getChildren().addAll(outerRing, innerCircle, profileAvatarText);

        // Name & Score
        profileUsernameLabel = new Label("Player");
        profileUsernameLabel.getStyleClass().add("profile-username");

        profileRatingLabel = new Label("🏆 1500 LP");
        profileRatingLabel.getStyleClass().add("profile-rating-badge");

        VBox userDetailBox = new VBox(6, profileUsernameLabel, profileRatingLabel);
        userDetailBox.setAlignment(Pos.CENTER);

        // Stats Overview Title
        Label statsSectionTitle = new Label("BATTLE PERFORMANCE");
        statsSectionTitle.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 11px; -fx-font-weight: bold; -fx-letter-spacing: 0.5px;");

        // 2x2 Stats Grid
        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(10);
        statsGrid.setVgap(10);
        statsGrid.setAlignment(Pos.CENTER);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        statsGrid.getColumnConstraints().addAll(col1, col2);

        statMatchesLabel = new Label("0");
        statMatchesLabel.getStyleClass().add("stat-value-highlight");
        VBox matchesTile = createStatTile("🎮 Matches", statMatchesLabel);

        statWinsLabel = new Label("0");
        statWinsLabel.getStyleClass().add("stat-value-winrate");
        VBox winsTile = createStatTile("⚔️ Wins", statWinsLabel);

        statLossesLabel = new Label("0");
        statLossesLabel.setStyle("-fx-text-fill: #F87171; -fx-font-size: 16px; -fx-font-weight: 800;");
        VBox lossesTile = createStatTile("💀 Losses", statLossesLabel);

        statWinRateLabel = new Label("0.0%");
        statWinRateLabel.getStyleClass().add("stat-value");
        VBox winRateTile = createStatTile("🎯 Win Rate", statWinRateLabel);

        statsGrid.add(matchesTile, 0, 0);
        statsGrid.add(winsTile, 1, 0);
        statsGrid.add(lossesTile, 0, 1);
        statsGrid.add(winRateTile, 1, 1);

        // Win Rate Progress Bar
        VBox progressBox = new VBox(6);
        Label progressTitle = new Label("WIN RATE RATIO");
        progressTitle.setStyle("-fx-text-fill: #64748B; -fx-font-size: 10px; -fx-font-weight: 700;");

        winRateBar = new ProgressBar(0.0);
        winRateBar.getStyleClass().add("winrate-bar");
        winRateBar.setMaxWidth(Double.MAX_VALUE);
        winRateBar.setPrefHeight(10);

        progressBox.getChildren().addAll(progressTitle, winRateBar);

        // Status Footer
        HBox statusFooter = new HBox(8);
        statusFooter.setAlignment(Pos.CENTER);
        statusFooter.setPadding(new Insets(10, 0, 0, 0));
        statusFooter.setStyle("-fx-border-color: rgba(56, 189, 248, 0.1) transparent transparent transparent; -fx-border-width: 1 0 0 0;");

        Circle lobbyPulse = new Circle(4, Color.web("#38BDF8"));
        Label lobbyState = new Label("Connected to Server (Lobby Mode)");
        lobbyState.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 11px;");
        statusFooter.getChildren().addAll(lobbyPulse, lobbyState);

        profileCard.getChildren().addAll(
                cardHeader,
                avatarPane,
                userDetailBox,
                statsSectionTitle,
                statsGrid,
                progressBox,
                statusFooter
        );

        updateProfileDisplay();
        return profileCard;
    }

    private VBox createStatTile(String title, Label valueLabel) {
        VBox tile = new VBox(4);
        tile.getStyleClass().add("stat-tile");
        tile.setAlignment(Pos.CENTER_LEFT);

        Label label = new Label(title);
        label.getStyleClass().add("stat-label");

        tile.getChildren().addAll(label, valueLabel);
        return tile;
    }

    // =========================================================================
    // 3. CỘT PHẢI - DANH SÁCH NGƯỜI CHƠI ONLINE (ONLINE PLAYERS LIST)
    // =========================================================================
    private VBox buildPlayerListPanel() {
        VBox container = new VBox(16);
        container.getStyleClass().add("glass-card");
        container.setPadding(new Insets(24));
        VBox.setVgrow(container, Priority.ALWAYS);

        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("glass-card-header");

        Label sectionHeading = new Label("ONLINE ARENA PLAYERS");
        sectionHeading.getStyleClass().add("section-heading");

        onlineCountBadge = new Label("● 0 Players");
        onlineCountBadge.getStyleClass().add("online-count-badge");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label tipLabel = new Label("💡 Select an IDLE player to challenge 1v1");
        tipLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px;");

        header.getChildren().addAll(sectionHeading, onlineCountBadge, spacer, tipLabel);

        // Player Cards Scrollable Container
        playerListContainer = new VBox(10);
        playerListContainer.setPadding(new Insets(6, 4, 6, 4));

        ScrollPane scrollPane = new ScrollPane(playerListContainer);
        scrollPane.getStyleClass().add("cyber-scroll-pane");
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        container.getChildren().addAll(header, scrollPane);

        renderPlayerCards();
        return container;
    }

    /**
     * Phương thức tạo động Player Card (Node) từ PlayerDTO.
     */
    public Node createPlayerCard(PlayerDTO player) {
        HBox card = new HBox(16);
        card.getStyleClass().add("player-card");
        card.setAlignment(Pos.CENTER_LEFT);

        Long currentUserIdObj = sceneManager.getClientState().getUserId();
        long currentUserId = currentUserIdObj != null ? currentUserIdObj : -1L;
        boolean isSelf = (player.getUserId() == currentUserId);

        // 1. Mini Avatar
        StackPane avatarBox = new StackPane();
        Circle avatarCircle = new Circle(22);
        String avatarColor = getPaletteColorForUsername(player.getUsername());
        avatarCircle.setFill(Color.web(avatarColor));

        Label initialLabel = new Label(getInitialChar(player.getUsername()));
        initialLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: 900; -fx-text-fill: white;");

        avatarBox.getChildren().addAll(avatarCircle, initialLabel);

        // 2. Username & Rating
        VBox infoBox = new VBox(3);
        infoBox.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label(player.getUsername() != null ? player.getUsername() : "Unknown");
        nameLabel.getStyleClass().add("player-card-username");

        Label scoreLabel = new Label("🏆 " + player.getTotalScore() + " LP");
        scoreLabel.getStyleClass().add("player-card-score");

        infoBox.getChildren().addAll(nameLabel, scoreLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // 3. Status Badge
        HBox statusBadge = new HBox(6);
        statusBadge.setAlignment(Pos.CENTER);

        PlayerStatus status = player.getStatus() != null ? player.getStatus() : PlayerStatus.IDLE;
        Circle dot = new Circle(4);
        Label statusText = new Label();

        switch (status) {
            case IDLE -> {
                dot.setFill(Color.web("#10B981"));
                dot.setEffect(new DropShadow(6, Color.web("#10B981")));
                statusText.setText("READY");
                statusBadge.getStyleClass().add("badge-idle");
            }
            case IN_ROOM -> {
                dot.setFill(Color.web("#60A5FA"));
                statusText.setText("IN ROOM");
                statusBadge.getStyleClass().add("badge-inroom");
            }
            case PLAYING -> {
                dot.setFill(Color.web("#F59E0B"));
                statusText.setText("IN GAME");
                statusBadge.getStyleClass().add("badge-ingame");
            }
        }
        statusBadge.getChildren().addAll(dot, statusText);

        // 4. Action Button or Self Indicator
        Node actionNode;
        if (isSelf) {
            Label selfBadge = new Label("YOU");
            selfBadge.getStyleClass().add("badge-self");
            actionNode = selfBadge;
        } else {
            Button challengeBtn = new Button("⚡ CHALLENGE");
            challengeBtn.getStyleClass().add("btn-challenge");

            boolean canChallenge = (status == PlayerStatus.IDLE) && player.isCanChallenge();
            challengeBtn.setDisable(!canChallenge);

            if (canChallenge) {
                addScaleHoverAnimation(challengeBtn, 1.06);
                challengeBtn.setOnAction(e -> sendChallenge(player));
            }

            actionNode = challengeBtn;
        }

        card.getChildren().addAll(avatarBox, infoBox, spacer, statusBadge, actionNode);
        addScaleHoverAnimation(card, 1.015);
        return card;
    }

    private void renderPlayerCards() {
        if (playerListContainer == null) return;

        playerListContainer.getChildren().clear();

        if (onlineCountBadge != null) {
            onlineCountBadge.setText("● " + playerList.size() + " Online");
        }

        if (playerList.isEmpty()) {
            VBox emptyBox = new VBox(12);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(50, 20, 50, 20));

            Label emptyIcon = new Label("📡");
            emptyIcon.setStyle("-fx-font-size: 40px;");

            Label emptyTitle = new Label("Searching for opponents...");
            emptyTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #94A3B8;");

            Label emptySubtitle = new Label("No other players are currently online in the lobby.\nOpen a 2nd game client to start a 1v1 battle!");
            emptySubtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748B; -fx-text-alignment: center;");

            emptyBox.getChildren().addAll(emptyIcon, emptyTitle, emptySubtitle);
            playerListContainer.getChildren().add(emptyBox);
            return;
        }

        for (PlayerDTO player : playerList) {
            playerListContainer.getChildren().add(createPlayerCard(player));
        }
    }

    // =========================================================================
    // PROFILE & DATA SYNC
    // =========================================================================
    public void refreshData() {
        updateProfileDisplay();
        sceneManager.getRestClient().getProfile().thenAccept(res -> {
            if (res != null && res.containsKey("userId")) {
                Number scoreNum = (Number) res.getOrDefault("totalScore", 1500L);
                if (scoreNum != null) {
                    sceneManager.getClientState().setRankingScore(scoreNum.longValue());
                }

                Platform.runLater(() -> applyProfileStats(res));
            }
        }).exceptionally(ex -> {
            Platform.runLater(this::updateProfileDisplay);
            return null;
        });
    }

    private void updateProfileDisplay() {
        ClientState state = sceneManager.getClientState();
        String username = state.getUsername() != null ? state.getUsername() : "Player";
        long score = state.getRankingScore() != null ? state.getRankingScore() : 1500L;

        if (profileAvatarText != null) {
            profileAvatarText.setText(getInitialChar(username));
        }
        if (profileUsernameLabel != null) {
            profileUsernameLabel.setText(username);
        }
        if (profileRatingLabel != null) {
            profileRatingLabel.setText("🏆 " + score + " LP");
        }
    }

    private void applyProfileStats(Map<String, Object> profileData) {
        updateProfileDisplay();

        int matches = getIntValue(profileData, "matchesPlayed", 0);
        int wins = getIntValue(profileData, "matchesWon", 0);
        int losses = getIntValue(profileData, "matchesLost", 0);
        double winRate = getDoubleValue(profileData, "winRate", 0.0);

        if (statMatchesLabel != null) statMatchesLabel.setText(String.valueOf(matches));
        if (statWinsLabel != null) statWinsLabel.setText(String.valueOf(wins));
        if (statLossesLabel != null) statLossesLabel.setText(String.valueOf(losses));
        if (statWinRateLabel != null) statWinRateLabel.setText(String.format("%.1f%%", winRate));

        if (winRateBar != null) {
            winRateBar.setProgress(Math.min(1.0, Math.max(0.0, winRate / 100.0)));
        }
    }

    // =========================================================================
    // CHALLENGE & SOCKET OPERATIONS
    // =========================================================================
    private void sendChallenge(PlayerDTO target) {
        ChallengePayload payload = new ChallengePayload(target.getUserId());
        sceneManager.getTcpClient().send(Message.event(ActionType.CHALLENGE, payload));
        sceneManager.showAlert("Challenge Dispatched", "Challenge sent to " + target.getUsername() + ". Awaiting response...", Alert.AlertType.INFORMATION);
    }

    private void handleServerMessage(Message msg) {
        if (msg == null || msg.getActionType() == null) return;

        switch (msg.getActionType()) {
            case ONLINE_LIST -> {
                OnlineListPayload payload = objectMapper.convertValue(msg.getPayload(), OnlineListPayload.class);
                if (payload != null && payload.getPlayers() != null) {
                    Platform.runLater(() -> {
                        playerList.clear();
                        playerList.addAll(payload.getPlayers());
                        sceneManager.getClientState().setOnlinePlayers(payload.getPlayers());
                        renderPlayerCards();
                    });
                }
            }
            case CHALLENGE_RECEIVED -> {
                ChallengeReceivedPayload payload = objectMapper.convertValue(msg.getPayload(), ChallengeReceivedPayload.class);
                if (payload != null) {
                    Platform.runLater(() -> showIncomingChallengeDialog(payload));
                }
            }
            case CHALLENGE_RESULT -> {
                ChallengeResultPayload payload = objectMapper.convertValue(msg.getPayload(), ChallengeResultPayload.class);
                if (payload != null) {
                    Platform.runLater(() -> {
                        closeActiveChallengeDialog();
                        sceneManager.showAlert("Challenge Status", "Challenge " + payload.getStatus() + ": " + payload.getReason(), Alert.AlertType.INFORMATION);
                    });
                }
            }
            case MATCH_START -> {
                Platform.runLater(() -> {
                    closeActiveChallengeDialog();
                    sceneManager.showGameScene();
                });
            }
            case ERROR -> {
                ErrorPayload err = objectMapper.convertValue(msg.getPayload(), ErrorPayload.class);
                if (err != null) {
                    Platform.runLater(() -> sceneManager.showAlert("Error", err.getMessage(), Alert.AlertType.ERROR));
                }
            }
        }
    }

    private void showIncomingChallengeDialog(ChallengeReceivedPayload payload) {
        closeActiveChallengeDialog();

        Dialog<ButtonType> dialog = new Dialog<>();
        this.activeChallengeDialog = dialog;
        dialog.setTitle("Battle Challenge Received!");
        dialog.setHeaderText("⚡ Incoming Challenge from " + payload.getChallengerUsername() + "!");

        ButtonType acceptButtonType = new ButtonType("Accept Battle", ButtonBar.ButtonData.OK_DONE);
        ButtonType rejectButtonType = new ButtonType("Decline", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(acceptButtonType, rejectButtonType);

        Label countdownLabel = new Label("Time remaining: 15s");
        countdownLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #EF4444;");

        Label descLabel = new Label("Player " + payload.getChallengerUsername() + " wants to compete with you in 1v1 Territorial Paint.");
        descLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #334155;");

        dialog.getDialogPane().setContent(new VBox(12, descLabel, countdownLabel));

        int[] remaining = {15};
        countdownScheduler = Executors.newSingleThreadScheduledExecutor();
        countdownTask = countdownScheduler.scheduleAtFixedRate(() -> {
            remaining[0]--;
            Platform.runLater(() -> countdownLabel.setText("Time remaining: " + remaining[0] + "s"));
            if (remaining[0] <= 0) {
                Platform.runLater(dialog::close);
            }
        }, 1, 1, TimeUnit.SECONDS);

        dialog.showAndWait().ifPresent(response -> {
            cancelCountdown();
            if (response == acceptButtonType) {
                sceneManager.getTcpClient().send(Message.event(ActionType.CHALLENGE_ACCEPT, new ChallengeAcceptPayload(payload.getChallengeId())));
            } else {
                sceneManager.getTcpClient().send(Message.event(ActionType.CHALLENGE_REJECT, new ChallengeRejectPayload(payload.getChallengeId())));
            }
        });
    }

    private void closeActiveChallengeDialog() {
        cancelCountdown();
        if (activeChallengeDialog != null) {
            activeChallengeDialog.close();
            activeChallengeDialog = null;
        }
    }

    private void cancelCountdown() {
        if (countdownTask != null) {
            countdownTask.cancel(true);
            countdownTask = null;
        }
        if (countdownScheduler != null) {
            countdownScheduler.shutdownNow();
            countdownScheduler = null;
        }
    }

    private void handleLogout() {
        SoundManager.getInstance().stopLobbyBgm();
        sceneManager.getTcpClient().disconnect();
        sceneManager.showLoginScene();
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================
    private void addScaleHoverAnimation(Node node, double scaleTo) {
        ScaleTransition stIn = new ScaleTransition(Duration.millis(120), node);
        stIn.setToX(scaleTo);
        stIn.setToY(scaleTo);

        ScaleTransition stOut = new ScaleTransition(Duration.millis(120), node);
        stOut.setToX(1.0);
        stOut.setToY(1.0);

        node.setOnMouseEntered(e -> stIn.playFromStart());
        node.setOnMouseExited(e -> stOut.playFromStart());
    }

    private String getInitialChar(String name) {
        if (name == null || name.isBlank()) return "?";
        return name.trim().substring(0, 1).toUpperCase();
    }

    private String getPaletteColorForUsername(String name) {
        if (name == null || name.isEmpty()) return AVATAR_PALETTE[0];
        int hash = Math.abs(name.hashCode());
        return AVATAR_PALETTE[hash % AVATAR_PALETTE.length];
    }

    private int getIntValue(Map<String, Object> map, String key, int defVal) {
        if (map == null || !map.containsKey(key)) return defVal;
        Object val = map.get(key);
        if (val instanceof Number num) return num.intValue();
        return defVal;
    }

    private double getDoubleValue(Map<String, Object> map, String key, double defVal) {
        if (map == null || !map.containsKey(key)) return defVal;
        Object val = map.get(key);
        if (val instanceof Number num) return num.doubleValue();
        return defVal;
    }
}

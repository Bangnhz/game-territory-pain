package com.territorypaint.client.ui;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;
import java.util.Map;

/**
 * Paginated Leaderboard View.
 * Requirements: 10.6, 10.7, 10.8, 10.9, 10.10
 */
public class LeaderboardView {

    private final SceneManager sceneManager;
    private final BorderPane root;

    private TableView<Map<String, Object>> table;
    private ObservableList<Map<String, Object>> list;
    private Label pageLabel;
    private Button prevBtn;
    private Button nextBtn;

    private int currentPage = 0;
    private int totalPages = 1;

    public LeaderboardView(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
        this.root = new BorderPane();
        initView();
    }

    public Region getView() {
        return root;
    }

    private void initView() {
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #121824;");

        // Header Navigation
        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 20, 0));

        Label title = new Label("Global Leaderboard");
        title.getStyleClass().add("title-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button backBtn = new Button("Back to Lobby");
        backBtn.setOnAction(e -> sceneManager.showLobbyScene());

        Button refreshBtn = new Button("🔄 Refresh");
        refreshBtn.getStyleClass().add("button-secondary");
        refreshBtn.setOnAction(e -> loadPage(currentPage));

        Button soundToggleBtn = SoundManager.getInstance().createSoundToggleButton();
        header.getChildren().addAll(title, spacer, refreshBtn, soundToggleBtn, backBtn);
        root.setTop(header);

        // Leaderboard Table
        list = FXCollections.observableArrayList();
        table = new TableView<>(list);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        Label placeholder = new Label("No players registered yet.\nRankings will appear as players join!");
        placeholder.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 14px; -fx-alignment: center; -fx-text-alignment: center;");
        table.setPlaceholder(placeholder);

        TableColumn<Map<String, Object>, String> rankCol = new TableColumn<>("Rank");
        rankCol.setPrefWidth(90);
        rankCol.setCellValueFactory(data -> {
            Object rank = data.getValue().get("rank");
            if (rank instanceof Number num) {
                int r = num.intValue();
                if (r == 1) return new SimpleStringProperty("🥇 #1");
                if (r == 2) return new SimpleStringProperty("🥈 #2");
                if (r == 3) return new SimpleStringProperty("🥉 #3");
                return new SimpleStringProperty("#" + r);
            }
            return new SimpleStringProperty("#-");
        });
        rankCol.setCellFactory(col -> new TableCell<Map<String, Object>, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.contains("🥇")) {
                        setStyle("-fx-text-fill: #FACC15; -fx-font-weight: bold;");
                    } else if (item.contains("🥈")) {
                        setStyle("-fx-text-fill: #E2E8F0; -fx-font-weight: bold;");
                    } else if (item.contains("🥉")) {
                        setStyle("-fx-text-fill: #F97316; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #38BDF8; -fx-font-weight: bold;");
                    }
                }
            }
        });

        TableColumn<Map<String, Object>, String> nameCol = new TableColumn<>("Username");
        nameCol.setPrefWidth(180);
        nameCol.setCellValueFactory(data -> {
            Object uname = data.getValue().get("username");
            String name = uname != null ? uname.toString() : "Unknown";
            Long currentUserId = sceneManager.getClientState().getUserId();
            boolean isMe = Boolean.TRUE.equals(data.getValue().get("isCurrentUser"));
            if (!isMe && currentUserId != null) {
                Object uId = data.getValue().get("userId");
                if (uId instanceof Number num && num.longValue() == currentUserId.longValue()) {
                    isMe = true;
                }
            }
            return new SimpleStringProperty(isMe ? name + " ⭐ (You)" : name);
        });

        TableColumn<Map<String, Object>, String> scoreCol = new TableColumn<>("Total Score");
        scoreCol.setPrefWidth(120);
        scoreCol.setCellValueFactory(data -> {
            Object val = data.getValue().get("totalScore");
            return new SimpleStringProperty((val != null ? val.toString() : "0") + " pts");
        });
        scoreCol.setCellFactory(col -> new TableCell<Map<String, Object>, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #34D399; -fx-font-weight: bold;");
                }
            }
        });

        TableColumn<Map<String, Object>, String> matchesCol = new TableColumn<>("Played");
        matchesCol.setPrefWidth(100);
        matchesCol.setCellValueFactory(data -> {
            Object val = data.getValue().get("matchesPlayed");
            return new SimpleStringProperty(val != null ? String.valueOf(val) : "0");
        });

        TableColumn<Map<String, Object>, String> winsCol = new TableColumn<>("Wins");
        winsCol.setPrefWidth(100);
        winsCol.setCellValueFactory(data -> {
            Object val = data.getValue().get("matchesWon");
            return new SimpleStringProperty(val != null ? String.valueOf(val) : "0");
        });

        TableColumn<Map<String, Object>, String> winRateCol = new TableColumn<>("Win Rate");
        winRateCol.setPrefWidth(110);
        winRateCol.setCellValueFactory(data -> {
            Object wr = data.getValue().get("winRate");
            if (wr instanceof Number num) {
                double d = num.doubleValue();
                double pct = (d <= 1.0 && d > 0.0) ? d * 100.0 : d;
                return new SimpleStringProperty(String.format("%.1f%%", pct));
            }
            return new SimpleStringProperty("0.0%");
        });

        table.getColumns().addAll(rankCol, nameCol, scoreCol, matchesCol, winsCol, winRateCol);

        // Highlight Current User row
        table.setRowFactory(tv -> new TableRow<Map<String, Object>>() {
            @Override
            protected void updateItem(Map<String, Object> item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else {
                    Long currentUserId = sceneManager.getClientState().getUserId();
                    boolean isMe = Boolean.TRUE.equals(item.get("isCurrentUser"));
                    if (!isMe && currentUserId != null) {
                        Object uId = item.get("userId");
                        if (uId instanceof Number num && num.longValue() == currentUserId.longValue()) {
                            isMe = true;
                        }
                    }
                    if (isMe) {
                        setStyle("-fx-background-color: rgba(56, 189, 248, 0.22); -fx-font-weight: bold; -fx-border-color: #38BDF8; -fx-border-width: 1px 0 1px 0;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        root.setCenter(table);

        // Status Bar
        HBox pagination = new HBox(15);
        pagination.setAlignment(Pos.CENTER);
        pagination.setPadding(new Insets(15, 0, 0, 0));

        pageLabel = new Label("Showing Top 100 Leaderboard");
        pageLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #94A3B8;");

        pagination.getChildren().addAll(pageLabel);
        root.setBottom(pagination);
    }

    @SuppressWarnings("unchecked")
    public void loadPage(int page) {
        this.currentPage = page;
        sceneManager.getRestClient().getLeaderboard(0, 100)
            .thenAccept(res -> Platform.runLater(() -> {
                List<Map<String, Object>> entries = null;
                if (res != null && res.containsKey("entries")) {
                    entries = (List<Map<String, Object>>) res.get("entries");
                } else if (res != null && res.containsKey("content")) {
                    entries = (List<Map<String, Object>>) res.get("content");
                }

                if (entries != null) {
                    list.setAll(entries);
                    int total = entries.size();
                    boolean hasAppendedUser = entries.stream().anyMatch(e -> {
                        Object r = e.get("rank");
                        return r instanceof Number && ((Number) r).intValue() > 100;
                    });

                    if (hasAppendedUser) {
                        pageLabel.setText(String.format("Top 100 Server Ranking + Your Rank (#%s) (Total: %d)",
                                entries.get(entries.size() - 1).get("rank"), total));
                    } else {
                        pageLabel.setText(String.format("Showing %d players (Top 100 Server Ranking)", total));
                    }
                } else if (res != null && res.containsKey("error")) {
                    table.setPlaceholder(new Label("Failed to load leaderboard: " + res.getOrDefault("message", "Error")));
                }
            }))
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    table.setPlaceholder(new Label("Error connecting to server. Please try again."));
                });
                return null;
            });
    }
}

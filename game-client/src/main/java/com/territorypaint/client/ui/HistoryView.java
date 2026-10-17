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
 * Paginated Match History View.
 * Requirements: 11.1-11.9
 */
public class HistoryView {

    private final SceneManager sceneManager;
    private final BorderPane root;

    private TableView<Map<String, Object>> table;
    private ObservableList<Map<String, Object>> list;
    private Label pageLabel;
    private Button prevBtn;
    private Button nextBtn;

    private int currentPage = 0;
    private int totalPages = 1;

    public HistoryView(SceneManager sceneManager) {
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

        Label title = new Label("Match History");
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

        // History Table
        list = FXCollections.observableArrayList();
        table = new TableView<>(list);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        Label placeholder = new Label("No matches played yet.\nPlay a match to see your battle history!");
        placeholder.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 14px; -fx-alignment: center; -fx-text-alignment: center;");
        table.setPlaceholder(placeholder);

        TableColumn<Map<String, Object>, String> idCol = new TableColumn<>("Match ID");
        idCol.setPrefWidth(120);
        idCol.setCellValueFactory(data -> {
            Object val = data.getValue().get("matchId");
            if (val != null) {
                String s = val.toString();
                return new SimpleStringProperty(s.length() >= 8 ? s.substring(0, 8) + "..." : s);
            }
            return new SimpleStringProperty("-");
        });

        TableColumn<Map<String, Object>, String> opponentCol = new TableColumn<>("Opponent");
        opponentCol.setPrefWidth(140);
        opponentCol.setCellValueFactory(data -> {
            Object val = data.getValue().get("opponentUsername");
            return new SimpleStringProperty(val != null ? val.toString() : "Unknown");
        });

        TableColumn<Map<String, Object>, String> outcomeCol = new TableColumn<>("Outcome");
        outcomeCol.setPrefWidth(110);
        outcomeCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getOrDefault("outcome", "-"))));
        outcomeCol.setCellFactory(col -> new TableCell<Map<String, Object>, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    if ("WIN".equalsIgnoreCase(item)) {
                        setText("🏆 VICTORY");
                        setStyle("-fx-text-fill: #34D399; -fx-font-weight: bold;");
                    } else if ("LOSS".equalsIgnoreCase(item)) {
                        setText("💀 DEFEAT");
                        setStyle("-fx-text-fill: #F87171; -fx-font-weight: bold;");
                    } else if ("DRAW".equalsIgnoreCase(item)) {
                        setText("🤝 DRAW");
                        setStyle("-fx-text-fill: #38BDF8; -fx-font-weight: bold;");
                    } else {
                        setText(item);
                        setStyle("");
                    }
                }
            }
        });

        TableColumn<Map<String, Object>, String> scoreCol = new TableColumn<>("Score (Cells)");
        scoreCol.setPrefWidth(120);
        scoreCol.setCellValueFactory(data -> {
            Object my = data.getValue().get("myCells");
            if (my == null) my = data.getValue().get("cellsOwned");
            Object opp = data.getValue().get("opponentCells");
            return new SimpleStringProperty((my != null ? my : "0") + " - " + (opp != null ? opp : "0"));
        });

        TableColumn<Map<String, Object>, String> deltaCol = new TableColumn<>("Rank Delta");
        deltaCol.setPrefWidth(100);
        deltaCol.setCellValueFactory(data -> {
            Object delta = data.getValue().get("rankDelta");
            if (delta instanceof Number num) {
                int d = num.intValue();
                return new SimpleStringProperty(d > 0 ? "+" + d : String.valueOf(d));
            }
            return new SimpleStringProperty("+0");
        });
        deltaCol.setCellFactory(col -> new TableCell<Map<String, Object>, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.startsWith("+") && !item.equals("+0")) {
                        setStyle("-fx-text-fill: #34D399; -fx-font-weight: bold;");
                    } else if (item.startsWith("-")) {
                        setStyle("-fx-text-fill: #F87171; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #94A3B8;");
                    }
                }
            }
        });

        TableColumn<Map<String, Object>, String> reasonCol = new TableColumn<>("Reason");
        reasonCol.setPrefWidth(110);
        reasonCol.setCellValueFactory(data -> {
            Object reason = data.getValue().get("endReason");
            return new SimpleStringProperty(reason != null ? reason.toString() : "TIMEOUT");
        });

        TableColumn<Map<String, Object>, String> dateCol = new TableColumn<>("Played At");
        dateCol.setPrefWidth(160);
        dateCol.setCellValueFactory(data -> {
            Object playedAt = data.getValue().get("playedAt");
            if (playedAt == null) playedAt = data.getValue().get("endedAt");
            if (playedAt != null) {
                String str = playedAt.toString();
                if (str.contains("T")) {
                    return new SimpleStringProperty(str.replace("T", " ").replaceAll("\\..*", "").replace("Z", ""));
                }
                return new SimpleStringProperty(str);
            }
            return new SimpleStringProperty("-");
        });

        table.getColumns().addAll(idCol, opponentCol, outcomeCol, scoreCol, deltaCol, reasonCol, dateCol);
        root.setCenter(table);

        // Pagination Bar
        HBox pagination = new HBox(15);
        pagination.setAlignment(Pos.CENTER);
        pagination.setPadding(new Insets(15, 0, 0, 0));

        prevBtn = new Button("Previous");
        prevBtn.getStyleClass().add("button-secondary");
        prevBtn.setOnAction(e -> {
            if (currentPage > 0) loadPage(currentPage - 1);
        });

        pageLabel = new Label("Page 1 of 1");
        pageLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #94A3B8;");

        nextBtn = new Button("Next");
        nextBtn.getStyleClass().add("button-secondary");
        nextBtn.setOnAction(e -> {
            if (currentPage < totalPages - 1) loadPage(currentPage + 1);
        });

        pagination.getChildren().addAll(prevBtn, pageLabel, nextBtn);
        root.setBottom(pagination);
    }

    @SuppressWarnings("unchecked")
    public void loadPage(int page) {
        this.currentPage = page;
        sceneManager.getRestClient().getMatchHistory(page, 10)
            .thenAccept(res -> Platform.runLater(() -> {
                if (res != null && res.containsKey("content")) {
                    List<Map<String, Object>> content = (List<Map<String, Object>>) res.get("content");
                    list.setAll(content);
                    Number totalPagesNum = (Number) res.getOrDefault("totalPages", 1);
                    this.totalPages = totalPagesNum != null ? Math.max(1, totalPagesNum.intValue()) : 1;
                    pageLabel.setText("Page " + (currentPage + 1) + " of " + Math.max(1, totalPages));
                    prevBtn.setDisable(currentPage == 0);
                    nextBtn.setDisable(currentPage >= totalPages - 1);
                } else if (res != null && res.containsKey("error")) {
                    table.setPlaceholder(new Label("Failed to load match history: " + res.getOrDefault("message", "Error")));
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

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

        header.getChildren().addAll(title, spacer, backBtn);
        root.setTop(header);

        // Leaderboard Table
        list = FXCollections.observableArrayList();
        table = new TableView<>(list);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Map<String, Object>, String> rankCol = new TableColumn<>("Rank");
        rankCol.setCellValueFactory(data -> {
            int index = table.getItems().indexOf(data.getValue()) + 1 + (currentPage * 10);
            return new SimpleStringProperty("#" + index);
        });

        TableColumn<Map<String, Object>, String> nameCol = new TableColumn<>("Username");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().get("username"))));

        TableColumn<Map<String, Object>, String> scoreCol = new TableColumn<>("Total Score");
        scoreCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().get("totalScore"))));

        TableColumn<Map<String, Object>, String> matchesCol = new TableColumn<>("Played");
        matchesCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().get("matchesPlayed"))));

        TableColumn<Map<String, Object>, String> winsCol = new TableColumn<>("Wins");
        winsCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().get("matchesWon"))));

        TableColumn<Map<String, Object>, String> winRateCol = new TableColumn<>("Win Rate");
        winRateCol.setCellValueFactory(data -> {
            Object wr = data.getValue().get("winRate");
            return new SimpleStringProperty(wr != null ? String.format("%.1f%%", Double.parseDouble(wr.toString()) * 100) : "0.0%");
        });

        table.getColumns().addAll(rankCol, nameCol, scoreCol, matchesCol, winsCol, winRateCol);
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
        sceneManager.getRestClient().getLeaderboard(page, 10).thenAccept(res -> Platform.runLater(() -> {
            if (res.containsKey("content")) {
                List<Map<String, Object>> content = (List<Map<String, Object>>) res.get("content");
                list.setAll(content);
                Number totalPagesNum = (Number) res.getOrDefault("totalPages", 1);
                this.totalPages = totalPagesNum != null ? totalPagesNum.intValue() : 1;
                pageLabel.setText("Page " + (currentPage + 1) + " of " + Math.max(1, totalPages));
                prevBtn.setDisable(currentPage == 0);
                nextBtn.setDisable(currentPage >= totalPages - 1);
            }
        }));
    }
}

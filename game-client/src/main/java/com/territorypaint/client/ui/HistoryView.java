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

        header.getChildren().addAll(title, spacer, backBtn);
        root.setTop(header);

        // History Table
        list = FXCollections.observableArrayList();
        table = new TableView<>(list);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Map<String, Object>, String> idCol = new TableColumn<>("Match ID");
        idCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().get("matchId")).substring(0, 8) + "..."));

        TableColumn<Map<String, Object>, String> opponentCol = new TableColumn<>("Opponent");
        opponentCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getOrDefault("opponentUsername", "Opponent"))));

        TableColumn<Map<String, Object>, String> slotCol = new TableColumn<>("Slot");
        slotCol.setCellValueFactory(data -> new SimpleStringProperty("Slot " + data.getValue().get("playerSlot")));

        TableColumn<Map<String, Object>, String> cellsCol = new TableColumn<>("Cells Owned");
        cellsCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().get("cellsOwned"))));

        TableColumn<Map<String, Object>, String> outcomeCol = new TableColumn<>("Outcome");
        outcomeCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().get("outcome"))));

        TableColumn<Map<String, Object>, String> deltaCol = new TableColumn<>("Rank Delta");
        deltaCol.setCellValueFactory(data -> new SimpleStringProperty("+" + data.getValue().get("rankDelta")));

        TableColumn<Map<String, Object>, String> dateCol = new TableColumn<>("Ended At");
        dateCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().get("endedAt"))));

        table.getColumns().addAll(idCol, opponentCol, slotCol, cellsCol, outcomeCol, deltaCol, dateCol);
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
        sceneManager.getRestClient().getMatchHistory(page, 10).thenAccept(res -> Platform.runLater(() -> {
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

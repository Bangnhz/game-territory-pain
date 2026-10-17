package com.territorypaint.client.ui;

import com.territorypaint.client.net.RestClient;
import com.territorypaint.client.net.TcpClient;
import com.territorypaint.client.state.ClientState;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Navigation and Scene Manager for JavaFX Client.
 */
public class SceneManager {

    private static SceneManager instance;

    private final Stage stage;
    private final RestClient restClient;
    private final TcpClient tcpClient;
    private final ClientState clientState;

    private LoginView loginView;
    private LobbyView lobbyView;
    private GameView gameView;
    private LeaderboardView leaderboardView;
    private HistoryView historyView;

    public SceneManager(Stage stage, String backendUrl, String gameServerHost, int gameServerPort) {
        instance = this;
        this.stage = stage;
        this.restClient = new RestClient(backendUrl);
        this.tcpClient = new TcpClient(gameServerHost, gameServerPort);
        this.clientState = ClientState.getInstance();

        this.stage.setTitle("Territorial Paint Game");
        this.stage.setMinWidth(900);
        this.stage.setMinHeight(680);
    }

    public static SceneManager getInstance() {
        return instance;
    }

    public RestClient getRestClient() {
        return restClient;
    }

    public TcpClient getTcpClient() {
        return tcpClient;
    }

    public ClientState getClientState() {
        return clientState;
    }

    public void showLoginScene() {
        SoundManager.getInstance().stopLobbyBgm();
        if (loginView == null) {
            loginView = new LoginView(this);
        }
        setScene(loginView.getView());
    }

    public void showLobbyScene() {
        if (lobbyView == null) {
            lobbyView = new LobbyView(this);
        }
        lobbyView.refreshData();
        setScene(lobbyView.getView());
        SoundManager.getInstance().playLobbyBgm();
    }

    public void showGameScene() {
        showGameScene(null);
    }

    public void showGameScene(com.territorypaint.common.payload.MatchStartPayload payload) {
        SoundManager.getInstance().stopLobbyBgm();
        if (gameView == null) {
            gameView = new GameView(this);
        }
        if (payload != null) {
            gameView.onMatchStart(payload);
        } else {
            gameView.resetView();
        }
        setScene(gameView.getView());
    }

    public void showLeaderboardScene() {
        if (leaderboardView == null) {
            leaderboardView = new LeaderboardView(this);
        }
        leaderboardView.loadPage(0);
        setScene(leaderboardView.getView());
    }

    public void showHistoryScene() {
        if (historyView == null) {
            historyView = new HistoryView(this);
        }
        historyView.loadPage(0);
        setScene(historyView.getView());
    }

    private void setScene(Region root) {
        Platform.runLater(() -> {
            SoundManager.getInstance().attachButtonSounds(root);
            Scene scene = stage.getScene();
            if (scene == null) {
                scene = new Scene(root, 1000, 720);
                scene.getStylesheets().add(getStylesheetUrl());
                stage.setScene(scene);
            } else {
                scene.setRoot(root);
            }
            stage.show();
        });
    }

    public void showAlert(String title, String content, Alert.AlertType type) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type, content, ButtonType.OK);
            if (stage != null) {
                alert.initOwner(stage);
            }
            alert.setTitle(title);
            alert.setHeaderText(null);
            try {
                var cssUrl = getClass().getResource("/css/lobby.css");
                if (cssUrl != null) {
                    alert.getDialogPane().getStylesheets().add(cssUrl.toExternalForm());
                }
            } catch (Exception ignored) {}
            alert.getDialogPane().getStyleClass().add("cyber-alert-dialog");
            alert.showAndWait();
        });
    }

    public static String getStylesheetUrl() {
        // Embedded dark CSS design system
        return "data:text/css," + encodeCss(DARK_THEME_CSS);
    }

    private static String encodeCss(String css) {
        return css.replace(" ", "%20").replace("\n", "%20").replace("#", "%23");
    }

    private static final String DARK_THEME_CSS = """
        .root {
            -fx-background-color: #121824;
            -fx-font-family: 'Segoe UI', Roboto, sans-serif;
        }
        .label {
            -fx-text-fill: #E2E8F0;
        }
        .title-label {
            -fx-font-size: 28px;
            -fx-font-weight: bold;
            -fx-text-fill: #38BDF8;
        }
        .subtitle-label {
            -fx-font-size: 14px;
            -fx-text-fill: #94A3B8;
        }
        .button {
            -fx-background-color: #0284C7;
            -fx-text-fill: white;
            -fx-font-weight: bold;
            -fx-font-size: 14px;
            -fx-padding: 10 20;
            -fx-background-radius: 6;
            -fx-cursor: hand;
        }
        .button:hover {
            -fx-background-color: #38BDF8;
        }
        .button-secondary {
            -fx-background-color: #334155;
            -fx-text-fill: #F1F5F9;
        }
        .button-secondary:hover {
            -fx-background-color: #475569;
        }
        .text-field, .password-field {
            -fx-background-color: #1E293B;
            -fx-text-fill: #F8FAFC;
            -fx-prompt-text-fill: #64748B;
            -fx-background-radius: 6;
            -fx-padding: 10;
            -fx-font-size: 14px;
            -fx-border-color: #334155;
            -fx-border-radius: 6;
        }
        .table-view {
            -fx-background-color: #1E293B;
            -fx-background-radius: 8;
        }
        .table-view .column-header-background {
            -fx-background-color: #0F172A;
        }
        .table-view .column-header {
            -fx-background-color: transparent;
            -fx-size: 40px;
        }
        .table-view .column-header .label {
            -fx-text-fill: #94A3B8;
            -fx-font-weight: bold;
        }
        .table-row-cell {
            -fx-background-color: #1E293B;
            -fx-text-fill: white;
            -fx-cell-size: 44px;
        }
        .table-row-cell:odd {
            -fx-background-color: #162032;
        }
        .table-row-cell:hover {
            -fx-background-color: #334155;
        }
    """;
}

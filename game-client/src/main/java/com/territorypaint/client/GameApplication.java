package com.territorypaint.client;

import com.territorypaint.client.ui.SceneManager;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Main JavaFX Application Entry Point.
 */
public class GameApplication extends Application {

    private static final String DEFAULT_BACKEND_URL = "http://localhost:8080";
    private static final String DEFAULT_GAME_SERVER_HOST = "localhost";
    private static final int DEFAULT_GAME_SERVER_PORT = 8888;

    @Override
    public void start(Stage primaryStage) {
        String backendUrl = System.getProperty("backend.url", DEFAULT_BACKEND_URL);
        String serverHost = System.getProperty("gameserver.host", DEFAULT_GAME_SERVER_HOST);
        int serverPort = Integer.getInteger("gameserver.port", DEFAULT_GAME_SERVER_PORT);

        SceneManager sceneManager = new SceneManager(primaryStage, backendUrl, serverHost, serverPort);
        sceneManager.showLoginScene();
    }

    @Override
    public void stop() throws Exception {
        if (SceneManager.getInstance() != null && SceneManager.getInstance().getTcpClient() != null) {
            SceneManager.getInstance().getTcpClient().disconnect();
        }
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

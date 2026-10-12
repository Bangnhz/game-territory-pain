package com.territorypaint.client;

import com.territorypaint.client.ui.SceneManager;
import javafx.application.Application;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Main JavaFX Application Entry Point.
 */
public class GameApplication extends Application {

    private static final Logger logger = LoggerFactory.getLogger(GameApplication.class);

    private static final String CONFIG_FILE = "application.properties";
    private static final String DEFAULT_BACKEND_URL = "http://localhost:8080";
    private static final String DEFAULT_GAME_SERVER_HOST = "localhost";
    private static final int DEFAULT_GAME_SERVER_PORT = 8888;

    private static final Properties properties = new Properties();

    static {
        loadProperties();
    }

    private static void loadProperties() {
        // 1. Try external config file first (current working directory)
        File externalConfig = new File(CONFIG_FILE);
        if (externalConfig.exists() && externalConfig.isFile()) {
            try (InputStream is = new FileInputStream(externalConfig)) {
                properties.load(is);
                logger.info("Loaded configuration from external file: {}", externalConfig.getAbsolutePath());
                return;
            } catch (IOException e) {
                logger.warn("Failed to load external configuration: {}", e.getMessage());
            }
        }

        // 2. Fallback to classpath resource
        try (InputStream is = GameApplication.class.getResourceAsStream("/" + CONFIG_FILE)) {
            if (is != null) {
                properties.load(is);
                logger.info("Loaded configuration from classpath: {}", CONFIG_FILE);
            } else {
                logger.warn("Configuration file '{}' not found in classpath. Using defaults.", CONFIG_FILE);
            }
        } catch (IOException e) {
            logger.warn("Failed to load configuration from classpath: {}", e.getMessage());
        }
    }

    @Override
    public void start(Stage primaryStage) {
        String propBackendUrl = properties.getProperty("backend.url", DEFAULT_BACKEND_URL);
        String propServerHost = properties.getProperty("gameserver.host", DEFAULT_GAME_SERVER_HOST);
        int propServerPort = DEFAULT_GAME_SERVER_PORT;
        try {
            String portVal = properties.getProperty("gameserver.port");
            if (portVal != null && !portVal.isBlank()) {
                propServerPort = Integer.parseInt(portVal.trim());
            }
        } catch (NumberFormatException e) {
            logger.warn("Invalid gameserver.port in properties, fallback to {}", DEFAULT_GAME_SERVER_PORT);
        }

        // System properties (-Dbackend.url, etc.) take highest priority if provided
        String backendUrl = System.getProperty("backend.url", propBackendUrl);
        String serverHost = System.getProperty("gameserver.host", propServerHost);
        int serverPort = Integer.getInteger("gameserver.port", propServerPort);

        logger.info("Client connecting to Backend: {}, Game Server: {}:{}", backendUrl, serverHost, serverPort);

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

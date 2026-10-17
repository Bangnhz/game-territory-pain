package com.territorypaint.client.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Modern Dark Mode Login & Registration View.
 * Requirements: 1.1, 1.2, 1.3, 1.7, 1.11, 2.2
 */
public class LoginView {

    private final SceneManager sceneManager;
    private final VBox root;

    private TextField usernameField;
    private PasswordField passwordField;
    private Label errorLabel;
    private Button actionButton;
    private Hyperlink switchLink;
    private boolean isLoginMode = true;

    public LoginView(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
        this.root = new VBox(20);
        initView();
    }

    public Region getView() {
        return root;
    }

    private void initView() {
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setStyle("-fx-background-color: #121824;");

        VBox card = new VBox(18);
        card.setMaxWidth(400);
        card.setPadding(new Insets(30));
        card.setStyle("-fx-background-color: #1E293B; -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 15, 0, 0, 8);");
        card.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("Territorial Paint");
        titleLabel.getStyleClass().add("title-label");

        Label subtitleLabel = new Label("Sign in to your account to play");
        subtitleLabel.getStyleClass().add("subtitle-label");

        VBox titleBox = new VBox(6, titleLabel, subtitleLabel);
        titleBox.setAlignment(Pos.CENTER);

        Label userLabel = new Label("Username");
        usernameField = new TextField();
        usernameField.setPromptText("Enter username (3-20 characters)");

        Label passLabel = new Label("Password");
        passwordField = new PasswordField();
        passwordField.setPromptText("Enter password (3-72 characters)");

        errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 13px;");
        errorLabel.setWrapText(true);
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);

        actionButton = new Button("Sign In");
        actionButton.setMaxWidth(Double.MAX_VALUE);
        actionButton.setOnAction(e -> handleAction());

        switchLink = new Hyperlink("Need an account? Register");
        switchLink.setStyle("-fx-text-fill: #38BDF8; -fx-border-color: transparent;");
        switchLink.setOnAction(e -> toggleMode());

        HBox switchBox = new HBox(switchLink);
        switchBox.setAlignment(Pos.CENTER);

        card.getChildren().addAll(
                titleBox,
                new VBox(6, userLabel, usernameField),
                new VBox(6, passLabel, passwordField),
                errorLabel,
                actionButton,
                switchBox
        );

        HBox topBox = new HBox();
        topBox.setMaxWidth(400);
        topBox.setAlignment(Pos.CENTER_RIGHT);
        Button soundToggleBtn = SoundManager.getInstance().createSoundToggleButton();
        topBox.getChildren().add(soundToggleBtn);

        root.getChildren().addAll(topBox, card);
    }

    private void toggleMode() {
        isLoginMode = !isLoginMode;
        clearError();
        if (isLoginMode) {
            actionButton.setText("Sign In");
            switchLink.setText("Need an account? Register");
        } else {
            actionButton.setText("Register");
            switchLink.setText("Already have an account? Sign In");
        }
    }

    private void handleAction() {
        String username = usernameField.getText() != null ? usernameField.getText().trim() : "";
        String password = passwordField.getText() != null ? passwordField.getText() : "";

        if (username.length() < 3 || username.length() > 20) {
            showError("Username must be between 3 and 20 characters");
            return;
        }

        if (password.length() < 3 || password.length() > 72) {
            showError("Password must be between 3 and 72 characters");
            return;
        }

        clearError();
        actionButton.setDisable(true);

        if (isLoginMode) {
            sceneManager.getRestClient().login(username, password)
                    .thenAccept(res -> Platform.runLater(() -> processLoginResult(res, username)))
                    .exceptionally(ex -> {
                        Platform.runLater(() -> {
                            actionButton.setDisable(false);
                            showError("Connection failed: " + ex.getMessage());
                        });
                        return null;
                    });
        } else {
            sceneManager.getRestClient().register(username, password)
                    .thenAccept(res -> Platform.runLater(() -> {
                        actionButton.setDisable(false);
                        if (res.containsKey("token") || res.containsKey("profile") || res.containsKey("userId")) {
                            sceneManager.showAlert("Success", "Account created successfully! Please sign in.", Alert.AlertType.INFORMATION);
                            toggleMode();
                        } else {
                            showError(extractErrorMessage(res, "Registration failed"));
                        }
                    }))
                    .exceptionally(ex -> {
                        Platform.runLater(() -> {
                            actionButton.setDisable(false);
                            showError("Registration error: " + ex.getMessage());
                        });
                        return null;
                    });
        }
    }

    private void processLoginResult(java.util.Map<String, Object> res, String username) {
        actionButton.setDisable(false);
        if (res.containsKey("token")) {
            String jwt = (String) res.get("token");
            Number userIdNum = (Number) res.get("userId");
            Number ratingNum = (Number) res.getOrDefault("rankingScore", 1500L);

            sceneManager.getClientState().setJwtToken(jwt);
            sceneManager.getClientState().setUserId(userIdNum != null ? userIdNum.longValue() : 1L);
            sceneManager.getClientState().setUsername((String) res.getOrDefault("username", username));
            sceneManager.getClientState().setRankingScore(ratingNum != null ? ratingNum.longValue() : 1500L);

            // Connect to TCP Game Server with JWT
            boolean connected = sceneManager.getTcpClient().connectAndLogin(jwt);
            if (connected) {
                sceneManager.showLobbyScene();
            } else {
                showError("Could not connect to Game Server at localhost:8888");
            }
        } else {
            showError(extractErrorMessage(res, "Invalid credentials"));
        }
    }

    @SuppressWarnings("unchecked")
    private String extractErrorMessage(java.util.Map<String, Object> res, String defaultMsg) {
        if (res == null || res.isEmpty()) {
            return defaultMsg;
        }
        if (res.get("message") instanceof String msg && !msg.isBlank()) {
            return msg;
        }
        if (res.get("fields") instanceof java.util.Map<?, ?> fields && !fields.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            fields.forEach((k, v) -> {
                if (sb.length() > 0) sb.append("\n");
                sb.append(k).append(": ").append(v);
            });
            return sb.toString();
        }
        if (res.get("error") instanceof String err && !err.isBlank()) {
            return err;
        }
        return defaultMsg;
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void clearError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}

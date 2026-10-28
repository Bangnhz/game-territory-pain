package com.territorypaint.client.ui;

import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.input.MouseEvent;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Audio and Sound Effects Manager for Territorial Paint Client.
 * Manages background music (BGM) and low-latency sound effects (SFX).
 */
public class SoundManager {

    private static final Logger log = LoggerFactory.getLogger(SoundManager.class);
    private static SoundManager instance;

    private MediaPlayer lobbyBgmPlayer;
    private AudioClip moveSfx;
    private AudioClip clickSfx;

    private boolean muted = false;
    private final List<Consumer<Boolean>> muteListeners = new CopyOnWriteArrayList<>();

    private SoundManager() {
        initSounds();
    }

    public static synchronized SoundManager getInstance() {
        if (instance == null) {
            instance = new SoundManager();
        }
        return instance;
    }

    private void initSounds() {
        try {
            // 1. Nhạc nền Lobby (BGM) - thử mp3 trước, sau đó wav
            URL bgmUrl = getClass().getResource("/sounds/lobby_bgm.mp3");
            if (bgmUrl == null) {
                bgmUrl = getClass().getResource("/sounds/lobby_bgm.wav");
            }

            if (bgmUrl != null) {
                Media bgmMedia = new Media(bgmUrl.toExternalForm());
                lobbyBgmPlayer = new MediaPlayer(bgmMedia);
                lobbyBgmPlayer.setCycleCount(MediaPlayer.INDEFINITE); // Lặp vô tận
                lobbyBgmPlayer.setVolume(0.3); // Âm lượng 30%
            } else {
                log.warn("Không tìm thấy file nhạc nền sảnh (/sounds/lobby_bgm.mp3 hoặc .wav)");
            }

            // 2. Âm thanh di chuyển (SFX) - dùng AudioClip vì có độ trễ cực thấp
            URL moveUrl = getClass().getResource("/sounds/move_sfx.wav");
            if (moveUrl != null) {
                moveSfx = new AudioClip(moveUrl.toExternalForm());
                moveSfx.setVolume(0.5);
            } else {
                log.warn("Không tìm thấy file âm thanh di chuyển (/sounds/move_sfx.wav)");
            }

            // 3. Âm thanh Click nút bấm (SFX)
            URL clickUrl = getClass().getResource("/sounds/click_sfx.wav");
            if (clickUrl != null) {
                clickSfx = new AudioClip(clickUrl.toExternalForm());
                clickSfx.setVolume(0.4);
            } else {
                log.warn("Không tìm thấy file âm thanh click nút (/sounds/click_sfx.wav)");
            }
        } catch (Throwable e) {
            log.error("Không thể nạp file âm thanh: {}", e.getMessage());
        }
    }

    // --- Điều khiển MUTE / UNMUTE toàn bộ âm thanh ---
    public boolean isMuted() {
        return muted;
    }

    public synchronized void setMuted(boolean mute) {
        this.muted = mute;
        try {
            if (lobbyBgmPlayer != null) {
                lobbyBgmPlayer.setMute(muted);
                if (muted) {
                    lobbyBgmPlayer.pause();
                } else {
                    lobbyBgmPlayer.play();
                }
            }
        } catch (Throwable e) {
            log.warn("Lỗi đồng bộ trạng thái âm thanh: {}", e.getMessage());
        }
        notifyMuteListeners(muted);
    }

    public synchronized boolean toggleMute() {
        setMuted(!muted);
        return muted;
    }

    public void addMuteListener(Consumer<Boolean> listener) {
        if (listener != null) {
            muteListeners.add(listener);
            listener.accept(muted);
        }
    }

    public void removeMuteListener(Consumer<Boolean> listener) {
        if (listener != null) {
            muteListeners.remove(listener);
        }
    }

    private void notifyMuteListeners(boolean isMuted) {
        for (Consumer<Boolean> listener : muteListeners) {
            try {
                listener.accept(isMuted);
            } catch (Throwable t) {
                log.warn("Lỗi notify mute listener: {}", t.getMessage());
            }
        }
    }

    /**
     * Tạo một nút bấm bật/tắt tiếng phong cách Cyber Arena,
     * tự động đồng bộ trạng thái âm thanh trên toàn ứng dụng.
     */
    public Button createSoundToggleButton() {
        Button btn = new Button();
        btn.getStyleClass().add("sound-toggle-btn");
        updateSoundButtonDisplay(btn, isMuted());

        btn.setOnAction(e -> {
            toggleMute();
            playClickSfx();
        });

        // Hover scale animation
        btn.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(120), btn);
            st.setToX(1.05);
            st.setToY(1.05);
            st.play();
        });
        btn.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(120), btn);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });

        addMuteListener(isMutedState -> Platform.runLater(() -> updateSoundButtonDisplay(btn, isMutedState)));
        return btn;
    }

    private void updateSoundButtonDisplay(Button btn, boolean isMuted) {
        if (isMuted) {
            btn.setText("🔇");
            btn.getStyleClass().removeAll("sound-btn-on");
            if (!btn.getStyleClass().contains("sound-btn-off")) {
                btn.getStyleClass().add("sound-btn-off");
            }
        } else {
            btn.setText("🔊");
            btn.getStyleClass().removeAll("sound-btn-off");
            if (!btn.getStyleClass().contains("sound-btn-on")) {
                btn.getStyleClass().add("sound-btn-on");
            }
        }
    }

    // --- Điều khiển BGM Sảnh chờ ---
    public void playLobbyBgm() {
        if (muted) return;
        try {
            if (lobbyBgmPlayer != null && lobbyBgmPlayer.getStatus() != MediaPlayer.Status.PLAYING) {
                lobbyBgmPlayer.setMute(false);
                lobbyBgmPlayer.play();
            }
        } catch (Throwable e) {
            log.warn("Lỗi phát nhạc sảnh: {}", e.getMessage());
        }
    }

    public void stopLobbyBgm() {
        try {
            if (lobbyBgmPlayer != null) {
                lobbyBgmPlayer.stop();
            }
        } catch (Throwable e) {
            log.warn("Lỗi dừng nhạc sảnh: {}", e.getMessage());
        }
    }

    // --- Điều khiển SFX Di chuyển ---
    public void playMoveSfx() {
        if (muted) return;
        try {
            if (moveSfx != null) {
                moveSfx.play();
            }
        } catch (Throwable e) {
            log.warn("Lỗi phát âm thanh di chuyển: {}", e.getMessage());
        }
    }

    // --- Điều khiển SFX Click nút ---
    public void playClickSfx() {
        if (muted) return;
        try {
            if (clickSfx != null) {
                clickSfx.play();
            }
        } catch (Throwable e) {
            log.warn("Lỗi phát âm thanh click: {}", e.getMessage());
        }
    }

    /**
     * Tự động gắn âm thanh click cho tất cả ButtonBase bên trong cây giao diện.
     * Sử dụng EventFilter bắt sự kiện click chuột MOUSE_PRESSED.
     *
     * @param root Node cha của Scene/View
     */
    public void attachButtonSounds(Parent root) {
        if (root == null) return;

        root.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (event.getTarget() instanceof Node targetNode) {
                Node current = targetNode;
                while (current != null && current != root) {
                    if (current instanceof ButtonBase btn && !btn.isDisabled()) {
                        playClickSfx();
                        break;
                    }
                    current = current.getParent();
                }
            }
        });
    }
}

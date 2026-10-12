package com.territorypaint.client.ui;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ButtonBase;
import javafx.scene.input.MouseEvent;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;

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

    // --- Điều khiển BGM Sảnh chờ ---
    public void playLobbyBgm() {
        try {
            if (lobbyBgmPlayer != null && lobbyBgmPlayer.getStatus() != MediaPlayer.Status.PLAYING) {
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

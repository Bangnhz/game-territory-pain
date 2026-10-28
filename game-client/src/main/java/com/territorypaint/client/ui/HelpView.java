package com.territorypaint.client.ui;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * Help / How to Play dialog overlay.
 * Shows game rules and detailed gameplay instructions.
 */
public class HelpView {

        /**
         * Shows the help dialog overlaid on the root StackPane.
         *
         * @param overlayContainer the root StackPane of the scene
         */
        public static void show(StackPane overlayContainer) {
                // ── Backdrop ─────────────────────────────────────────────────────────
                Region backdrop = new Region();
                backdrop.setStyle("-fx-background-color: rgba(0,0,0,0.72);");
                backdrop.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

                // ── Dialog box ───────────────────────────────────────────────────────
                VBox dialog = new VBox(0);
                dialog.setPrefWidth(660);
                dialog.setMaxWidth(700);
                dialog.setMinWidth(400);
                dialog.setPrefHeight(560);
                dialog.setMaxHeight(580);
                dialog.setStyle(
                                "-fx-background-color: linear-gradient(to bottom, #1a2236, #0f1623);" +
                                                "-fx-border-color: #00D2FF; -fx-border-width: 1.5;" +
                                                "-fx-border-radius: 16; -fx-background-radius: 16;");
                dialog.setEffect(createGlow());

                // ── Header ───────────────────────────────────────────────────────────
                HBox header = new HBox(0);
                header.setAlignment(Pos.CENTER_LEFT);
                header.setPadding(new Insets(18, 20, 14, 20));
                header.setStyle(
                                "-fx-background-color: linear-gradient(to right, #0d2137, #162840);" +
                                                "-fx-background-radius: 14 14 0 0;");

                Label icon = new Label("📖");
                icon.setStyle("-fx-font-size: 24px;");

                Label title = new Label("  Hướng Dẫn Chơi");
                title.setStyle(
                                "-fx-text-fill: #00D2FF; -fx-font-size: 19px;" +
                                                "-fx-font-weight: bold; -fx-font-family: 'Segoe UI';");

                Region spacerH = new Region();
                HBox.setHgrow(spacerH, Priority.ALWAYS);

                Button closeBtn = new Button("✕");
                closeBtn.setStyle(
                                "-fx-background-color: transparent; -fx-text-fill: #94A3B8;" +
                                                "-fx-font-size: 16px; -fx-cursor: hand; -fx-padding: 4 10;");
                closeBtn.setOnMouseEntered(e -> closeBtn.setStyle(
                                "-fx-background-color: rgba(255,80,80,0.2); -fx-text-fill: #FF6B6B;" +
                                                "-fx-font-size: 16px; -fx-cursor: hand; -fx-padding: 4 10;" +
                                                "-fx-background-radius: 6;"));
                closeBtn.setOnMouseExited(e -> closeBtn.setStyle(
                                "-fx-background-color: transparent; -fx-text-fill: #94A3B8;" +
                                                "-fx-font-size: 16px; -fx-cursor: hand; -fx-padding: 4 10;"));

                header.getChildren().addAll(icon, title, spacerH, closeBtn);

                // Thin divider (Separator instead of bound Rectangle)
                Separator divider = new Separator();
                divider.setStyle("-fx-background-color: rgba(0,210,255,0.25); -fx-pref-height: 1;");

                // ── Scrollable content ────────────────────────────────────────────────
                VBox content = new VBox(14);
                content.setPadding(new Insets(18, 22, 20, 22));

                content.getChildren().addAll(
                                buildSection("🎯  Mục Tiêu",
                                                "Chiếm nhiều ô đất nhất có thể trên bảng 10×10 trước khi hết giờ.\n" +
                                                                "Người chơi kiểm soát nhiều ô hơn khi kết thúc sẽ là người chiến thắng!"),

                                buildSection("⚔️  Luật Chơi",
                                                "• Mỗi ván gồm 2 người chơi, cả hai di chuyển đồng thời theo lượt.\n" +
                                                                "• Mỗi lượt di chuyển quân sang ô liền kề (lên / xuống / trái / phải).\n"
                                                                +
                                                                "• Ô bạn đứng vào sẽ được tô màu của bạn.\n" +
                                                                "• Bạn có thể chọn \"Bỏ lượt\" (Skip) nếu không muốn di chuyển.\n"
                                                                +
                                                                "• Không thể di chuyển ra ngoài bảng hoặc vào ô không hợp lệ."),

                                buildSection("⏱️  Thời Gian",
                                                "• Mỗi ván đấu kéo dài 60 giây.\n" +
                                                                "• Đồng hồ đếm ngược hiển thị ở giữa màn hình.\n" +
                                                                "• Khi hết giờ, hệ thống tự động tính điểm và công bố kết quả."),

                                buildSection("🏅  Tính Điểm & Xếp Hạng",
                                                "• Thắng:  +3 điểm ELO\n" +
                                                                "• Hòa:   +1 điểm ELO\n" +
                                                                "• Thua:   0 điểm ELO\n" +
                                                                "• Điểm ELO tích lũy qua các ván, xếp hạng toàn server trên Leaderboard."),

                                buildSection("🚀  Thách Đấu",
                                                "• Tại sảnh chờ, click vào thẻ người chơi bất kỳ để gửi lời thách đấu.\n"
                                                                +
                                                                "• Đối thủ nhận thông báo và có thể chấp nhận hoặc từ chối.\n"
                                                                +
                                                                "• Khi cả hai đồng ý, ván đấu tự động bắt đầu."));

                ScrollPane scroll = new ScrollPane(content);
                scroll.setFitToWidth(true);
                scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;" +
                                "-fx-border-color: transparent;");
                scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
                scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                VBox.setVgrow(scroll, Priority.ALWAYS);

                // ── Footer ────────────────────────────────────────────────────────────
                HBox footer = new HBox();
                footer.setAlignment(Pos.CENTER);
                footer.setPadding(new Insets(10, 20, 18, 20));

                Button gotItBtn = new Button("Đã hiểu, bắt đầu chơi! 🎮");
                gotItBtn.setStyle(
                                "-fx-background-color: linear-gradient(to right, #00D2FF, #0077CC);" +
                                                "-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;" +
                                                "-fx-padding: 9 26; -fx-background-radius: 8; -fx-cursor: hand;");
                gotItBtn.setOnMouseEntered(e -> gotItBtn.setStyle(
                                "-fx-background-color: linear-gradient(to right, #33DDFF, #0099EE);" +
                                                "-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;" +
                                                "-fx-padding: 9 26; -fx-background-radius: 8; -fx-cursor: hand;"));
                gotItBtn.setOnMouseExited(e -> gotItBtn.setStyle(
                                "-fx-background-color: linear-gradient(to right, #00D2FF, #0077CC);" +
                                                "-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;" +
                                                "-fx-padding: 9 26; -fx-background-radius: 8; -fx-cursor: hand;"));
                gotItBtn.setPrefWidth(220);
                footer.getChildren().add(gotItBtn);

                dialog.getChildren().addAll(header, divider, scroll, footer);

                // ── Overlay wrapper ───────────────────────────────────────────────────
                StackPane overlay = new StackPane(backdrop, dialog);
                StackPane.setAlignment(dialog, Pos.CENTER);
                // Prevent the dialog from stretching full-width inside the StackPane
                dialog.setMaxWidth(700);

                // Close actions
                Runnable closeAction = () -> {
                        FadeTransition ft = new FadeTransition(Duration.millis(180), overlay);
                        ft.setFromValue(1);
                        ft.setToValue(0);
                        ft.setOnFinished(ev -> overlayContainer.getChildren().remove(overlay));
                        ft.play();
                };
                closeBtn.setOnAction(e -> closeAction.run());
                gotItBtn.setOnAction(e -> closeAction.run());
                backdrop.setOnMouseClicked(e -> closeAction.run());

                // ── Animate in ────────────────────────────────────────────────────────
                overlay.setOpacity(0);
                overlayContainer.getChildren().add(overlay);

                FadeTransition fadeIn = new FadeTransition(Duration.millis(220), overlay);
                fadeIn.setFromValue(0);
                fadeIn.setToValue(1);

                ScaleTransition scaleIn = new ScaleTransition(Duration.millis(220), dialog);
                scaleIn.setFromX(0.88);
                scaleIn.setFromY(0.88);
                scaleIn.setToX(1.0);
                scaleIn.setToY(1.0);

                fadeIn.play();
                scaleIn.play();
        }

        // ── Section builder ───────────────────────────────────────────────────────

        private static VBox buildSection(String heading, String body) {
                Label h = new Label(heading);
                h.setStyle(
                                "-fx-text-fill: #00D2FF; -fx-font-size: 13px; -fx-font-weight: bold;" +
                                                "-fx-font-family: 'Segoe UI';");

                Label b = new Label(body);
                b.setStyle(
                                "-fx-text-fill: #CBD5E1; -fx-font-size: 12.5px; -fx-font-family: 'Segoe UI';" +
                                                "-fx-line-spacing: 3;");
                b.setWrapText(true);
                b.setMaxWidth(Double.MAX_VALUE);

                VBox textBlock = new VBox(4, h, b);
                textBlock.setMaxWidth(Double.MAX_VALUE);

                // Left accent bar
                Region accentBar = new Region();
                accentBar.setPrefWidth(3);
                accentBar.setMinWidth(3);
                accentBar.setMaxWidth(3);
                accentBar.setStyle("-fx-background-color: #00D2FF; -fx-background-radius: 2;");

                HBox row = new HBox(12, accentBar, textBlock);
                row.setAlignment(Pos.TOP_LEFT);
                HBox.setHgrow(textBlock, Priority.ALWAYS);

                VBox section = new VBox(row);
                section.setPadding(new Insets(12, 14, 12, 14));
                section.setMaxWidth(Double.MAX_VALUE);
                section.setStyle(
                                "-fx-background-color: rgba(255,255,255,0.04);" +
                                                "-fx-background-radius: 10; -fx-border-color: rgba(0,210,255,0.1);" +
                                                "-fx-border-radius: 10; -fx-border-width: 1;");
                return section;
        }

        private static javafx.scene.effect.DropShadow createGlow() {
                javafx.scene.effect.DropShadow glow = new javafx.scene.effect.DropShadow();
                glow.setColor(Color.web("#00D2FF", 0.35));
                glow.setRadius(28);
                glow.setSpread(0.05);
                return glow;
        }
}

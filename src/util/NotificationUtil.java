package util;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Modern notification system for the Student Management System.
 * Provides toast notifications, alerts, and status messages.
 */
public class NotificationUtil {
    
    // Notification types with their colors
    public enum NotificationType {
        SUCCESS("#10B981", "✓"),
        ERROR("#EF4444", "✕"),
        WARNING("#F59E0B", "⚠"),
        INFO("#3B82F6", "ℹ");
        
        private final String color;
        private final String icon;
        
        NotificationType(String color, String icon) {
            this.color = color;
            this.icon = icon;
        }
        
        public String getColor() { return color; }
        public String getIcon() { return icon; }
    }
    
    /**
     * Show a toast notification that auto-dismisses after a duration.
     * 
     * @param stage The parent stage
     * @param message The notification message
     * @param type The notification type (SUCCESS, ERROR, WARNING, INFO)
     * @param durationSeconds How long to show the notification
     */
    public static void showToast(Stage stage, String message, NotificationType type, double durationSeconds) {
        if (stage == null || message == null || message.isEmpty()) return;
        
        Popup popup = new Popup();
        popup.setAutoFix(true);
        popup.setAutoHide(true);
        
        // Create notification content
        HBox content = createNotificationContent(message, type);
        popup.getContent().add(content);
        
        // Show the popup
        popup.show(stage);
        
        // Position at top center of the stage
        double x = stage.getX() + (stage.getWidth() - content.getWidth()) / 2;
        double y = stage.getY() + 20;
        popup.setX(x);
        popup.setY(y);
        
        // Auto-hide after duration
        Timeline timeline = new Timeline(new KeyFrame(
            Duration.seconds(durationSeconds),
            event -> {
                FadeTransition fadeOut = new FadeTransition(Duration.millis(300), content);
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.0);
                fadeOut.setOnFinished(e -> popup.hide());
                fadeOut.play();
            }
        ));
        timeline.play();
    }
    
    /**
     * Show a quick success toast (3 seconds).
     */
    public static void showSuccess(Stage stage, String message) {
        showToast(stage, message, NotificationType.SUCCESS, 3.0);
    }
    
    /**
     * Show a quick error toast (5 seconds).
     */
    public static void showError(Stage stage, String message) {
        showToast(stage, message, NotificationType.ERROR, 5.0);
    }
    
    /**
     * Show a quick warning toast (4 seconds).
     */
    public static void showWarning(Stage stage, String message) {
        showToast(stage, message, NotificationType.WARNING, 4.0);
    }
    
    /**
     * Show a quick info toast (3 seconds).
     */
    public static void showInfo(Stage stage, String message) {
        showToast(stage, message, NotificationType.INFO, 3.0);
    }
    
    /**
     * Show a notification overlay on a scene.
     * This is useful for scene-based notifications.
     */
    public static void showSceneNotification(Scene scene, String message, NotificationType type) {
        if (scene == null) return;
        
        StackPane root = (StackPane) scene.getRoot();
        if (root == null) return;
        
        // Create notification container
        HBox notification = createNotificationContent(message, type);
        notification.setMaxWidth(400);
        StackPane.setAlignment(notification, Pos.TOP_CENTER);
        StackPane.setMargin(notification, new Insets(20, 0, 0, 0));
        
        // Add to root
        root.getChildren().add(notification);
        
        // Animate in
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), notification);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
        
        // Auto-remove after delay
        Timeline timeline = new Timeline(new KeyFrame(
            Duration.seconds(3),
            event -> {
                FadeTransition fadeOut = new FadeTransition(Duration.millis(300), notification);
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.0);
                fadeOut.setOnFinished(e -> root.getChildren().remove(notification));
                fadeOut.play();
            }
        ));
        timeline.play();
    }
    
    /**
     * Create the notification content HBox.
     */
    private static HBox createNotificationContent(String message, NotificationType type) {
        HBox container = new HBox(12);
        container.setAlignment(Pos.CENTER_LEFT);
        container.setPadding(new Insets(16, 20, 16, 20));
        container.setStyle(String.format(
            "-fx-background-color: white; " +
            "-fx-background-radius: 8px; " +
            "-fx-border-radius: 8px; " +
            "-fx-border-width: 0 0 0 4px; " +
            "-fx-border-color: transparent transparent transparent %s; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 8, 0, 0, 4);",
            type.getColor()
        ));
        
        // Icon
        Label iconLabel = new Label(type.getIcon());
        iconLabel.setStyle(String.format(
            "-fx-font-size: 18px; -fx-text-fill: %s; -fx-font-weight: bold;",
            type.getColor()
        ));
        
        // Message
        Label messageLabel = new Label(message);
        messageLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #374151; -fx-font-weight: 500;");
        messageLabel.setWrapText(true);
        
        container.getChildren().addAll(iconLabel, messageLabel);
        
        // Set size constraints
        container.setMinWidth(300);
        container.setMaxWidth(500);
        
        return container;
    }
    
    /**
     * Show a persistent notification that requires manual dismissal.
     */
    public static void showPersistentNotification(Stage stage, String message, NotificationType type, Runnable onDismiss) {
        if (stage == null) return;
        
        Popup popup = new Popup();
        popup.setAutoFix(true);
        popup.setAutoHide(false);
        
        HBox content = createNotificationContent(message, type);
        
        // Add close button
        Label closeBtn = new Label("✕");
        closeBtn.setStyle("-fx-font-size: 16px; -fx-text-fill: #9CA3AF; -fx-cursor: hand;");
        closeBtn.setOnMouseClicked(e -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(200), content);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(ev -> {
                popup.hide();
                if (onDismiss != null) onDismiss.run();
            });
            fadeOut.play();
        });
        
        content.getChildren().add(closeBtn);
        
        popup.getContent().add(content);
        popup.show(stage);
        
        double x = stage.getX() + (stage.getWidth() - content.getWidth()) / 2;
        double y = stage.getY() + 20;
        popup.setX(x);
        popup.setY(y);
    }
    
    /**
     * Show multiple notifications stacked.
     */
    public static void showStackedNotification(Stage stage, String[] messages, NotificationType[] types) {
        if (stage == null || messages == null || types == null) return;
        if (messages.length != types.length) return;
        
        for (int i = 0; i < messages.length; i++) {
            final int index = i;
            javafx.application.Platform.runLater(() -> {
                javafx.animation.PauseTransition delay = 
                    new javafx.animation.PauseTransition(Duration.millis(index * 500));
                delay.setOnFinished(e -> showToast(stage, messages[index], types[index], 4.0));
                delay.play();
            });
        }
    }
    
    /**
     * Clear all notifications from the stage.
     * Note: This is a placeholder - JavaFX Popup doesn't have a global registry.
     */
    public static void clearAllNotifications() {
        // Popups auto-hide when clicking elsewhere
        // For manual clearing, you'd need to track active popups
    }
}

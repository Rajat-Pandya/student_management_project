package controller;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Region;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class Main extends Application {

    private static final double BASE_WIDTH = 1369.0;
    private static final double BASE_HEIGHT = 864.0;

    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
            Parent root = loader.load();

            if (root instanceof Region region) {
                if (region.getPrefWidth() <= 0) region.setPrefWidth(BASE_WIDTH);
                if (region.getPrefHeight() <= 0) region.setPrefHeight(BASE_HEIGHT);
            }

            ScrollPane wrapper = new ScrollPane(root);
            wrapper.setFitToWidth(true);
            wrapper.setFitToHeight(true);
            wrapper.setPannable(true);
            wrapper.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            wrapper.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

            Rectangle2D screen = Screen.getPrimary().getVisualBounds();
            double initW = Math.min(BASE_WIDTH, screen.getWidth() * 0.9);
            double initH = Math.min(BASE_HEIGHT, screen.getHeight() * 0.9);

            Scene scene = new Scene(wrapper, initW, initH);

            try {
                if (getClass().getResource("/css/styles.css") != null)
                    scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            } catch (Exception ignored) {}

            UIUtils.applyScalingToRoot(root, scene, BASE_WIDTH, BASE_HEIGHT);

            primaryStage.setScene(scene);
            primaryStage.setTitle("Student Management System");
            primaryStage.setMinWidth(600);
            primaryStage.setMinHeight(420);
            primaryStage.sizeToScene();
            primaryStage.setMaximized(true);
            primaryStage.show();

            primaryStage.setX((screen.getWidth() - primaryStage.getWidth()) / 2);
            primaryStage.setY((screen.getHeight() - primaryStage.getHeight()) / 2);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
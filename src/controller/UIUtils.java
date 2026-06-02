package controller;

import javafx.beans.binding.DoubleBinding;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Region;

/**
 * UIUtils - reusable scaling helpers (used to keep all windows responsive).
 *
 * This version avoids using BooleanBinding.then(...) and instead computes
 * the scale as the minimum of widthScale and heightScale via a DoubleBinding.
 */
public final class UIUtils {
    private UIUtils() {}

    public static Scene createScaledScene(Parent root, double baseW, double baseH, double initW, double initH) {
        // Only set preferred sizes when the root supports them (Region).
        if (root instanceof Region region) {
            if (region.getPrefWidth() <= 0) region.setPrefWidth(baseW);
            if (region.getPrefHeight() <= 0) region.setPrefHeight(baseH);
        }

        ScrollPane wrapper = new ScrollPane(root);
        wrapper.setFitToWidth(true);
        wrapper.setFitToHeight(true);
        wrapper.setPannable(true);
        wrapper.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        wrapper.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        double w = initW > 0 ? initW : baseW;
        double h = initH > 0 ? initH : baseH;

        Scene scene = new Scene(wrapper, w, h);

        applyScalingToRoot(root, scene, baseW, baseH);

        return scene;
    }

    public static void applyScalingToRoot(Parent root, Scene scene, double baseW, double baseH) {
        if (root instanceof Region region) {
            if (region.getPrefWidth() <= 0) region.setPrefWidth(baseW);
            if (region.getPrefHeight() <= 0) region.setPrefHeight(baseH);
        }

        // scale factors for width and height
        DoubleBinding widthScale = scene.widthProperty().divide(baseW);
        DoubleBinding heightScale = scene.heightProperty().divide(baseH);

        // scale = min(widthScale, heightScale)
        DoubleBinding scale = new DoubleBinding() {
            { super.bind(widthScale, heightScale); }
            @Override
            protected double computeValue() {
                double w = widthScale.get();
                double h = heightScale.get();
                return Math.min(w, h);
            }
        };

        // clamp scale to [MIN .. 1.0]
        final double MIN = 0.55;
        DoubleBinding clamped = new DoubleBinding() {
            { super.bind(scale); }
            @Override
            protected double computeValue() {
                double s = scale.get();
                if (s > 1.0) s = 1.0;
                if (s < MIN) s = MIN;
                return s;
            }
        };

        root.scaleXProperty().bind(clamped);
        root.scaleYProperty().bind(clamped);
    }
}

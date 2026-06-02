package controller;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;

/**
 * Faculty dashboard controller - CSS-only nav highlighting.
 */
public class FacultyDashboardController {

    @FXML private StackPane contentArea;
    @FXML private Label lblWelcome;

    @FXML private Button btnHome;
    @FXML private Button btnStudentsAttendance;
    @FXML private Button btnAssignMarks;
    @FXML private Button btnAssignGrades;
    @FXML private Button btnLogout;

    @FXML private Button btnViewMarksheet;
    @FXML private Button btnViewAttendance;

    private Button activeButton;

    private String facultyUsername;
    private String facultyDepartment;
    private boolean initialized = false;

    /**
     * Called from other controllers (login) to inject faculty context.
     */
    public void setFacultyInfo(String username, String department) {
        this.facultyUsername = username;
        this.facultyDepartment = department;
        if (lblWelcome != null && username != null) lblWelcome.setText("Welcome, " + username);
        if (initialized) showHome();
    }

    @FXML
    private void initialize() {
        initialized = true;
        if (lblWelcome != null && facultyUsername != null) lblWelcome.setText("Welcome, " + facultyUsername);

        Platform.runLater(() -> {
            ensureNavClass(btnHome);
            ensureNavClass(btnStudentsAttendance);
            ensureNavClass(btnAssignMarks);
            ensureNavClass(btnAssignGrades);
            ensureNavClass(btnViewMarksheet);
            ensureNavClass(btnViewAttendance);

            setActiveButton(btnHome);
            showHome();
        });
    }

    private void ensureNavClass(Button b) {
        if (b == null) return;
        if (!b.getStyleClass().contains("button")) b.getStyleClass().add("button");
        if (!b.getStyleClass().contains("nav-button")) b.getStyleClass().add("nav-button");
    }

    private void setActiveButton(Button button) {
        Platform.runLater(() -> {
            if (activeButton != null) activeButton.getStyleClass().remove("nav-button-active");
            activeButton = button;
            if (activeButton != null) {
                ensureNavClass(activeButton);
                activeButton.setDisable(false);
                if (!activeButton.getStyleClass().contains("nav-button-active")) {
                    activeButton.getStyleClass().add("nav-button-active");
                }
            }
        });
    }

    private void applyTransition(Parent root) {
        if (root == null) return;
        FadeTransition ft = new FadeTransition(Duration.millis(180), root);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();
    }

    /**
     * Loads an FXML from the classpath. Tries with and without leading slash.
     * If the loaded controller exposes setFacultyInfo(String,String) it will be invoked.
     */
    private Parent loadFxml(String path) throws Exception {
        String[] tries = new String[] { path, path.startsWith("/") ? path.substring(1) : "/" + path };
        Exception last = null;
        for (String p : tries) {
            try {
                URL res = getClass().getResource(p);
                if (res == null) continue;
                FXMLLoader loader = new FXMLLoader(res);
                Parent root = loader.load();

                // attempt to inject faculty info
                Object ctrl = loader.getController();
                if (ctrl != null) {
                    try {
                        Method m = ctrl.getClass().getMethod("setFacultyInfo", String.class, String.class);
                        if (m != null) {
                            try { m.invoke(ctrl, facultyUsername, facultyDepartment); }
                            catch (IllegalAccessException | InvocationTargetException iae) { iae.printStackTrace(); }
                        }
                    } catch (NoSuchMethodException nsme) {
                        // ignore - controller doesn't accept faculty info
                    }
                }
                return root;
            } catch (Exception ex) {
                last = ex;
            }
        }
        if (last != null) throw last;
        throw new IllegalArgumentException("Resource not found: " + path);
    }

    /* ------------------ Navigation handlers ------------------ */

    @FXML
    private void showHome() {
        try {
            Parent root = loadFxml("/fxml/FacultyHome.fxml");
            setActiveButton(btnHome);
            contentArea.getChildren().setAll(root);
            applyTransition(root);
            Platform.runLater(() -> {
                if (contentArea.getScene() != null && contentArea.getScene().getWindow() instanceof Stage) {
                    ((Stage) contentArea.getScene().getWindow()).sizeToScene();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            showErrorWithStack("Could not load Faculty Home", e.getMessage(), e);
        }
    }

    @FXML
    private void showStudentsAttendance() {
        try {
            Parent root = loadFxml("/fxml/FacultyStudents.fxml");
            setActiveButton(btnStudentsAttendance);
            contentArea.getChildren().setAll(root);
            applyTransition(root);
        } catch (Exception e) {
            e.printStackTrace();
            showErrorWithStack("Could not load Students view", e.getMessage(), e);
        }
    }

    @FXML
    private void showAssignMarks() {
        try {
            Parent root = loadFxml("/fxml/AssignMarks.fxml");
            setActiveButton(btnAssignMarks);
            contentArea.getChildren().setAll(root);
            applyTransition(root);
        } catch (Exception e) {
            e.printStackTrace();
            showErrorWithStack("Could not load Assign Marks", e.getMessage(), e);
        }
    }

    @FXML
    private void showAssignGrades() {
        try {
            Parent root = loadFxml("/fxml/AssignGrades.fxml");
            setActiveButton(btnAssignGrades);
            contentArea.getChildren().setAll(root);
            applyTransition(root);
        } catch (Exception e) {
            e.printStackTrace();
            showErrorWithStack("Could not load Assign Grades", e.getMessage(), e);
        }
    }

    @FXML
    private void showMarksheet() {
        try {
            Parent root = loadFxml("/fxml/ViewMarksheet.fxml");
            setActiveButton(btnViewMarksheet);
            contentArea.getChildren().setAll(root);
            applyTransition(root);
        } catch (Exception e) {
            e.printStackTrace();
            showErrorWithStack("Could not load View Marksheeet", e.getMessage(), e);
        }
    }

    @FXML
    private void showAttendance() {
        try {
            Parent root = loadFxml("/fxml/ViewAttendance.fxml");
            setActiveButton(btnViewAttendance);
            contentArea.getChildren().setAll(root);
            applyTransition(root);
        } catch (Exception e) {
            e.printStackTrace();
            showErrorWithStack("Could not load View Attendance", e.getMessage(), e);
        }
    }

    /**
     * Logout — attempts to replace the existing window content with Login view (reuses loadFxml).
     */
    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            Window window = null;
            if (event != null && event.getSource() instanceof Node src) {
                if (src.getScene() != null) window = src.getScene().getWindow();
            }
            if (window == null && contentArea.getScene() != null) window = contentArea.getScene().getWindow();

            Parent loginRoot = loadFxml("/fxml/Login.fxml");
            if (window instanceof Stage stage) {
                stage.setTitle("Login");
                if (stage.getScene() != null) stage.getScene().setRoot(loginRoot);
                else stage.setScene(new Scene(loginRoot));
                Platform.runLater(stage::sizeToScene);
                stage.setResizable(true);
            } else {
                Stage newStage = new Stage();
                newStage.setScene(new Scene(loginRoot));
                newStage.setTitle("Login");
                newStage.show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            showErrorWithStack("Logout failed", e.getMessage(), e);
        }
    }

    /* ------------------ Quick-action & utility handlers ------------------ */

    @FXML
    private void onChangePassword(ActionEvent event) {
        showInfo("Change Password", "Change password dialog not implemented yet.");
    }

    @FXML
    private void onHelp(ActionEvent event) {
        showInfo("Help", "Open documentation or show help dialog.");
    }

    @FXML
    private void onAddStudent(ActionEvent event) {
        showInfo("Add Student", "This will open the Add Student dialog (not implemented).");
    }

    @FXML
    private void onUploadMarks(ActionEvent event) {
        showInfo("Upload Marks", "This will open the Upload Marks dialog (not implemented).");
    }

    @FXML
    private void onExportReport(ActionEvent event) {
        showInfo("Export Report", "Exporting summary report (not implemented).");
    }

    /* ------------------ Error / info helpers ------------------ */

    private void showErrorWithStack(String title, String message, Throwable ex) {
        System.err.println("[FacultyDashboard] " + title + ": " + message);
        if (ex != null) ex.printStackTrace();
        StringWriter sw = new StringWriter();
        if (ex != null) ex.printStackTrace(new PrintWriter(sw));
        String exceptionText = sw.toString();
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(title);
        alert.setContentText(message);
        if (!exceptionText.isBlank()) {
            TextArea textArea = new TextArea(exceptionText);
            textArea.setEditable(false);
            DialogPane dp = alert.getDialogPane();
            dp.setExpandableContent(textArea);
        }
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, message);
        a.setTitle(title);
        a.setHeaderText(null);
        a.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        a.showAndWait();
    }
}

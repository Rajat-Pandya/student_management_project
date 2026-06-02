package controller;

import dao.UserDAO;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.URL;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * AdminController - toggles nav-button-active class only (CSS handles visuals).
 * Implements Create User (calls UserDAO.createUser) and Backup DB CSV export.
 */
public class AdminController {

    @FXML private StackPane contentArea;

    @FXML private Button btnHome;
    @FXML private Button btnManageStudents;
    @FXML private Button btnManageFaculty;
    @FXML private Button btnManageSubjects;
    @FXML private Button btnAssignSubjects;

    private Button activeButton;

    private static final double BASE_WIDTH = 1369.0;
    private static final double BASE_HEIGHT = 864.0;

    @FXML
    private void initialize() {
        Platform.runLater(() -> {
            ensureNavClass(btnHome);
            ensureNavClass(btnManageStudents);
            ensureNavClass(btnManageFaculty);
            ensureNavClass(btnManageSubjects);
            ensureNavClass(btnAssignSubjects);

            setActiveButton(btnHome);
            showDashboard();
        });
    }

    private void ensureNavClass(Button b) {
        if (b == null) return;
        if (!b.getStyleClass().contains("button")) b.getStyleClass().add("button");
        if (!b.getStyleClass().contains("nav-button")) b.getStyleClass().add("nav-button");
    }

    private void setActiveButton(Button button) {
        Platform.runLater(() -> {
            if (activeButton != null) {
                activeButton.getStyleClass().remove("nav-button-active");
            }
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
        FadeTransition ft = new FadeTransition(Duration.millis(280), root);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();
    }

    private void setContentAndBind(Parent root) {
        if (root == null) return;

        if (root instanceof Region region) {
            try { region.prefWidthProperty().unbind(); } catch (Exception ignored) {}
            try { region.prefHeightProperty().unbind(); } catch (Exception ignored) {}
            region.prefWidthProperty().bind(contentArea.widthProperty());
            region.prefHeightProperty().bind(contentArea.heightProperty());
        }

        contentArea.getChildren().setAll(root);
        applyTransition(root);

        Stage stage = contentArea.getScene() != null ? (Stage) contentArea.getScene().getWindow() : null;
        if (stage != null) {
            try { stage.setMaximized(true); } catch (Exception ignored) {}
            try { stage.sizeToScene(); } catch (Exception ignored) {}
        }
    }

    private Parent loadFxml(String path) throws Exception {
        String[] tries = new String[] { path, path.startsWith("/") ? path.substring(1) : "/" + path };
        Exception last = null;
        for (String p : tries) {
            try {
                URL res = getClass().getResource(p);
                if (res == null) continue;
                return new FXMLLoader(res).load();
            } catch (Exception ex) {
                last = ex;
            }
        }
        if (last != null) throw last;
        throw new IllegalArgumentException("Resource not found: " + path);
    }

    @FXML
    private void showDashboard() {
        try {
            Parent root = loadFxml("/fxml/AdminHome.fxml");
            setActiveButton(btnHome);
            setContentAndBind(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void showStudents() {
        try {
            Parent root = loadFxml("/fxml/ManageStudents.fxml");
            setActiveButton(btnManageStudents);
            setContentAndBind(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void showFaculty() {
        try {
            Parent root = loadFxml("/fxml/ManageFaculty.fxml");
            setActiveButton(btnManageFaculty);
            setContentAndBind(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void showManageSubjects() {
        try {
            Parent root = loadFxml("/fxml/ManageSubjects.fxml");
            setActiveButton(btnManageSubjects);
            setContentAndBind(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void showAssignSubjects() {
        try {
            Parent root = loadFxml("/fxml/AssignSubjects.fxml");
            setActiveButton(btnAssignSubjects);
            setContentAndBind(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout() {
        try {
            Parent loginRoot = loadFxml("/fxml/Login.fxml");
            Stage stage = (Stage) contentArea.getScene().getWindow();

            if (stage == null) {
                if (contentArea.getScene() != null) {
                    contentArea.getScene().setRoot(loginRoot);
                }
                return;
            }

            Scene scaled = UIUtils.createScaledScene(loginRoot, BASE_WIDTH, BASE_HEIGHT,
                    Math.min(BASE_WIDTH, stage.getWidth()), Math.min(BASE_HEIGHT, stage.getHeight()));

            try {
                if (stage.getScene() != null && stage.getScene().getStylesheets() != null) {
                    scaled.getStylesheets().addAll(stage.getScene().getStylesheets());
                }
            } catch (Exception ignored) {}

            stage.setTitle("Login");
            stage.setScene(scaled);
            stage.setResizable(true);

            Platform.runLater(() -> {
                try { stage.setMaximized(true); } catch (Exception ignored) {}
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ----------------- Create User (calls UserDAO directly) ----------------- */

    @FXML
    private void onCreateUser() {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.initOwner(contentArea.getScene().getWindow());
        dlg.initModality(Modality.APPLICATION_MODAL);
        dlg.setTitle("Create User");

        Label lUser = new Label("Username:");
        TextField tfUser = new TextField();
        tfUser.setPromptText("username");

        Label lPass = new Label("Password:");
        PasswordField pf = new PasswordField();
        pf.setPromptText("password");

        // NOTE: role removed from UI — every created user will be an admin
        Label info = new Label("Account type: admin");
        info.setStyle("-fx-font-weight: 600; -fx-text-fill: #7A1217;");

        GridPane g = new GridPane();
        g.setHgap(8);
        g.setVgap(8);
        g.add(lUser, 0, 0);
        g.add(tfUser, 1, 0);
        g.add(lPass, 0, 1);
        g.add(pf, 1, 1);
        g.add(info, 1, 2);
        g.setMinWidth(360);

        dlg.getDialogPane().setContent(g);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // validate before allowing OK to proceed
        Button okBtn = (Button) dlg.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            String u = tfUser.getText().trim();
            String p = pf.getText();
            if (u.isEmpty() || p.isEmpty()) {
                Alert a = new Alert(Alert.AlertType.WARNING, "Username and password are required.", ButtonType.OK);
                a.initOwner(dlg.getOwner());
                a.showAndWait();
                ev.consume();
                return;
            }
            // optional: simple username whitespace check
            if (u.contains(" ")) {
                Alert a = new Alert(Alert.AlertType.WARNING, "Username cannot contain spaces.", ButtonType.OK);
                a.initOwner(dlg.getOwner());
                a.showAndWait();
                ev.consume();
            }
        });

        // auto-focus username
        Platform.runLater(tfUser::requestFocus);

        dlg.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                String username = tfUser.getText().trim();
                String password = pf.getText();
                String role = "admin"; // fixed

                try {
                    boolean created = UserDAO.createUser(username, password, role);
                    if (created) {
                        Alert a = new Alert(Alert.AlertType.INFORMATION, "Admin account created: " + username, ButtonType.OK);
                        a.initOwner(contentArea.getScene().getWindow());
                        a.showAndWait();
                    } else {
                        Alert a = new Alert(Alert.AlertType.WARNING, "Username already exists: " + username, ButtonType.OK);
                        a.initOwner(contentArea.getScene().getWindow());
                        a.showAndWait();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Alert a = new Alert(Alert.AlertType.ERROR, "Failed to create admin: " + ex.getMessage(), ButtonType.OK);
                    a.initOwner(contentArea.getScene().getWindow());
                    a.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                    a.showAndWait();
                }
            }
        });
    }

    /* ----------------- Backup DB (CSV dump) ----------------- */

    @FXML
    private void onBackupDB() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select folder to save DB backup CSVs");
        Stage st = (Stage) (contentArea.getScene() != null ? contentArea.getScene().getWindow() : null);
        File dir = chooser.showDialog(st);
        if (dir == null) return;

        String[] tablesToDump = new String[] {"students", "faculty", "subject", "marks", "attendance", "users"};

        List<String> written = new ArrayList<>();
        List<String> failed = new ArrayList<>();

        try (Connection conn = util.DatabaseConnection.getConnection()) {
            for (String tbl : tablesToDump) {
                String sql = "SELECT * FROM " + tbl;
                try (PreparedStatement ps = conn.prepareStatement(sql);
                     ResultSet rs = ps.executeQuery()) {

                    File out = new File(dir, tbl + ".csv");
                    try (BufferedWriter bw = new BufferedWriter(new FileWriter(out))) {
                        ResultSetMetaData md = rs.getMetaData();
                        int cols = md.getColumnCount();

                        StringJoiner hdr = new StringJoiner(",");
                        for (int i = 1; i <= cols; i++) hdr.add('"' + md.getColumnName(i) + '"');
                        bw.write(hdr.toString());
                        bw.newLine();

                        while (rs.next()) {
                            StringJoiner row = new StringJoiner(",");
                            for (int i = 1; i <= cols; i++) {
                                Object obj = rs.getObject(i);
                                String s = obj == null ? "" : String.valueOf(obj).replace("\"", "\"\"");
                                row.add('"' + s + '"');
                            }
                            bw.write(row.toString());
                            bw.newLine();
                        }
                        bw.flush();
                        written.add(out.getName());
                    } catch (Exception ioex) {
                        failed.add(tbl + " (io error: " + ioex.getMessage() + ")");
                    }
                } catch (SQLException sqle) {
                    failed.add(tbl + " (sql error: " + sqle.getMessage() + ")");
                }
            }
        } catch (Exception e) {
            Alert a = new Alert(Alert.AlertType.ERROR, "Backup failed: " + e.getMessage(), ButtonType.OK);
            a.initOwner(st);
            a.showAndWait();
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Backup completed.\n\n");
        if (!written.isEmpty()) {
            sb.append("Files written:\n");
            for (String w : written) sb.append(" - ").append(w).append("\n");
        }
        if (!failed.isEmpty()) {
            sb.append("\nFailed:\n");
            for (String f : failed) sb.append(" - ").append(f).append("\n");
        }

        Alert a = new Alert(Alert.AlertType.INFORMATION, sb.toString(), ButtonType.OK);
        a.setHeaderText("Backup result");
        a.initOwner(st);
        a.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        a.showAndWait();
    }
}

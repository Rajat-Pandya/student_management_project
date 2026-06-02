package controller;

import util.DatabaseConnection;
import util.Session;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.*;

/**
 * LoginController - robust + defensive.
 * - Handles InvocationTargetException when injecting faculty info into loaded dashboard controller.
 * - Keeps logo loading and responsive 1:2 layout bindings.
 */
public class LoginController {

    // FXML injections
    @FXML private HBox rootHBox;
    @FXML private StackPane logoPane;
    @FXML private ImageView logoImage;
    @FXML private StackPane loginArea;
    @FXML private VBox loginCard;
    @FXML private Label lblMiddleTitle;

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblStatus;
    @FXML private Button btnLogin; // used to get current Stage
    

    // bcrypt desired cost
    private static final int BCRYPT_COST = 12;

    @FXML
    private void initialize() {
        // Run layout-specific bindings after scene setup
        Platform.runLater(() -> {
            try {
                bindOneToTwoSplit();
                bindLogoSize();
                setupSceneListeners();
                loadLogoSafely();
            } catch (Exception e) {
                System.err.println("[Login] initialize error: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    // ---------------- Responsive helpers ----------------
    private void bindOneToTwoSplit() {
        if (rootHBox == null || logoPane == null || loginArea == null) return;
        logoPane.prefWidthProperty().bind(rootHBox.widthProperty().multiply(1.0 / 3.0));
        loginArea.prefWidthProperty().bind(rootHBox.widthProperty().multiply(2.0 / 3.0));
        logoPane.minWidthProperty().bind(rootHBox.widthProperty().multiply(0.18));
        loginArea.minWidthProperty().bind(rootHBox.widthProperty().multiply(0.45));
    }

    private void bindLogoSize() {
        if (logoImage == null || logoPane == null) return;
        logoImage.fitWidthProperty().bind(logoPane.widthProperty().multiply(0.6));
        logoImage.fitHeightProperty().bind(logoPane.heightProperty().multiply(0.6));
    }

    @SuppressWarnings("unused")
	private void setupSceneListeners() {
        if (rootHBox == null || lblMiddleTitle == null) return;
        rootHBox.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) return;
            Scene scene = newScene;
            scene.widthProperty().addListener((o, ov, nv) -> {
                double w = nv.doubleValue();
                double size = Math.max(16, Math.min(44, w * 0.025));
                lblMiddleTitle.setStyle("-fx-font-size: " + (int) size + "px;");
            });
            double w = scene.getWidth() > 0 ? scene.getWidth() : 1000;
            double size = Math.max(16, Math.min(44, w * 0.025));
            lblMiddleTitle.setStyle("-fx-font-size: " + (int) size + "px;");
        });
    }

    // ---------------- Logo loading ----------------
    private void loadLogoSafely() {
        if (logoImage == null) {
            System.out.println("[Login] logoImage ImageView is null (FXML injection failed).");
            return;
        }

String[] candidates = new String[] {
                "/images/download.jpg",
                "/images/logo.jpg",
                "/images/logo.png"
        };

        Image loaded = null;
        for (String path : candidates) {
            try (InputStream is = getClass().getResourceAsStream(path)) {
                if (is != null) {
                    loaded = new Image(is);
                    System.out.println("[Login] Loaded logo from: " + path);
                    break;
                } else {
                    System.out.println("[Login] Logo not found at: " + path);
                }
            } catch (Exception e) {
                System.err.println("[Login] Error loading logo at " + path + " : " + e.getMessage());
            }
        }

        if (loaded != null) {
            logoImage.setImage(loaded);
        } else {
            System.out.println("[Login] Logo not found on classpath. Put it under resources/images/ (e.g. logo.jpg).");
        }
    }

    // ---------------- Authentication ----------------
    @FXML
    private void handleLogin(ActionEvent event) {
        final String username = txtUsername == null ? null : txtUsername.getText();
        final String password = txtPassword == null ? null : txtPassword.getText();

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            setStatusMessage("Please enter username and password.");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) {
                setStatusMessage("Database connection failed.");
                return;
            }

            if (tryAuthenticateFromTable(conn, "users", username, password, event)) return;
            if (tryAuthenticateFromTable(conn, "faculty", username, password, event)) return;

            setStatusMessage("Invalid username or password.");
        } catch (SQLException ex) {
            ex.printStackTrace();
            setStatusMessage("Login failed: " + ex.getMessage());
        } catch (IOException io) {
            io.printStackTrace();
            setStatusMessage("Failed to load dashboard: " + io.getMessage());
        }
    }

    private boolean tryAuthenticateFromTable(Connection conn,
                                             String tableName,
                                             String providedUsername,
                                             String providedPlain,
                                             ActionEvent event) throws SQLException, IOException {
        String sqlByUsername = "SELECT * FROM " + tableName + " WHERE username = ? LIMIT 1";
        try (PreparedStatement pst = conn.prepareStatement(sqlByUsername)) {
            pst.setString(1, providedUsername);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return handleAuthenticatedRow(conn, rs, tableName, providedUsername, providedPlain, event);
                }
            }
        }

        String sqlByEmail = "SELECT * FROM " + tableName + " WHERE email = ? LIMIT 1";
        try (PreparedStatement pst2 = conn.prepareStatement(sqlByEmail)) {
            pst2.setString(1, providedUsername);
            try (ResultSet rs2 = pst2.executeQuery()) {
                if (rs2.next()) {
                    return handleAuthenticatedRow(conn, rs2, tableName, providedUsername, providedPlain, event);
                }
            }
        } catch (SQLException sqle) {
            System.err.println("Email lookup not available on table '" + tableName + "': " + sqle.getMessage());
        }

        return false;
    }

    private boolean handleAuthenticatedRow(Connection conn,
                                           ResultSet rs,
                                           String tableName,
                                           String providedUsername,
                                           String providedPlain,
                                           ActionEvent event) throws SQLException, IOException {
        ResultSetMetaData md = rs.getMetaData();

        int id = hasColumn(md, "id") ? rs.getInt("id") : -1;
        String dbUsername = hasColumn(md, "username") ? rs.getString("username") : (hasColumn(md, "email") ? rs.getString("email") : providedUsername);
        String storedPassword = hasColumn(md, "password") ? rs.getString("password") : null;
        String role = hasColumn(md, "role") ? rs.getString("role") : (tableName.equalsIgnoreCase("faculty") ? "faculty" : "user");
        String dept = hasColumn(md, "department") ? rs.getString("department") : null;
        String email = hasColumn(md, "email") ? rs.getString("email") : null;
        String name = hasColumn(md, "name") ? rs.getString("name") : null;

        if (storedPassword == null || storedPassword.isBlank()) {
            return false;
        }

        if (!verifyAndPossiblyRehash(conn, tableName, "id", id, storedPassword, providedPlain)) {
            return false;
        }

        String displayName = (name != null && !name.isBlank()) ? name : (dbUsername != null ? dbUsername : (email != null ? email : providedUsername));

        if ("faculty".equalsIgnoreCase(role) || tableName.equalsIgnoreCase("faculty")) {
            Session.setFaculty(id, dbUsername, dept);
            Session.setDisplayName(displayName);

            // load faculty dashboard and inject faculty info (safe reflection)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/FacultyDashboard.fxml"));
            Parent root = loader.load();
            Object ctrl = loader.getController();
            if (ctrl != null) {
                try {
                    Method m = ctrl.getClass().getMethod("setFacultyInfo", String.class, String.class);
                    if (m != null) {
                        try {
                            m.invoke(ctrl, dbUsername, dept);
                        } catch (IllegalAccessException iae) {
                            System.err.println("[Login] IllegalAccessException invoking setFacultyInfo on " + ctrl.getClass().getName());
                            iae.printStackTrace();
                        } catch (InvocationTargetException ite) {
                            System.err.println("[Login] Controller method setFacultyInfo threw an exception:");
                            if (ite.getCause() != null) {
                                ite.getCause().printStackTrace();
                            } else {
                                ite.printStackTrace();
                            }
                        }
                    }
                } catch (NoSuchMethodException nsme) {
                    // Controller doesn't accept setFacultyInfo - ignore.
                } catch (SecurityException se) {
                    System.err.println("[Login] SecurityException when trying to access setFacultyInfo: " + se.getMessage());
                    se.printStackTrace();
                }
            }

            swapSceneSafely(root, "Faculty Dashboard");
            return true;
        } else {
            Session.setUser(id, dbUsername, role != null ? role : "admin");
            Session.setDisplayName(displayName);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AdminDashboard.fxml"));
            Parent root = loader.load();
            swapSceneSafely(root, "Admin Dashboard");
            return true;
        }
    }

    private boolean hasColumn(ResultSetMetaData md, String columnName) throws SQLException {
        for (int i = 1; i <= md.getColumnCount(); i++) {
            if (columnName.equalsIgnoreCase(md.getColumnName(i))) return true;
            if (columnName.equalsIgnoreCase(md.getColumnLabel(i))) return true;
        }
        return false;
    }

    private boolean verifyAndPossiblyRehash(Connection conn,
                                            String table,
                                            String idColumn,
                                            int idValue,
                                            String storedPassword,
                                            String providedPlain) throws SQLException {
        if (storedPassword == null) return false;

        boolean looksLikeBcrypt = storedPassword.startsWith("$2a$") ||
                storedPassword.startsWith("$2b$") ||
                storedPassword.startsWith("$2y$");

        if (looksLikeBcrypt) {
            try {
                if (BCrypt.checkpw(providedPlain, storedPassword)) {
                    int currentCost = parseBcryptCost(storedPassword);
                    if (currentCost <= 0 || currentCost != BCRYPT_COST) {
                        rehashAndUpdate(conn, table, idColumn, idValue, providedPlain);
                    }
                    return true;
                } else {
                    return false;
                }
            } catch (IllegalArgumentException iae) {
                iae.printStackTrace();
                return false;
            }
        } else {
            if (providedPlain.equals(storedPassword)) {
                rehashAndUpdate(conn, table, idColumn, idValue, providedPlain);
                return true;
            } else {
                return false;
            }
        }
    }

    private void rehashAndUpdate(Connection conn, String table, String idColumn, int idValue, String plain) throws SQLException {
        if (!"users".equalsIgnoreCase(table) && !"faculty".equalsIgnoreCase(table)) {
            throw new SQLException("Invalid table for password update: " + table);
        }

        String newHash = BCrypt.hashpw(plain, BCrypt.gensalt(BCRYPT_COST));
        String updateSql = "UPDATE " + table + " SET password = ? WHERE " + idColumn + " = ?";

        try (PreparedStatement upd = conn.prepareStatement(updateSql)) {
            upd.setString(1, newHash);
            upd.setInt(2, idValue);
            int changed = upd.executeUpdate();
            if (changed > 0) {
                System.out.println("Re-hashed password and updated " + table + " id=" + idValue);
            } else {
                System.err.println("Failed to update password hash for " + table + " id=" + idValue);
            }
        }
    }

    private int parseBcryptCost(String bcryptHash) {
        if (bcryptHash == null) return -1;
        try {
            String[] parts = bcryptHash.split("\\$");
            if (parts.length >= 3) {
                return Integer.parseInt(parts[2]);
            }
        } catch (Exception ignored) {}
        return -1;
    }

    // ---------------- UI helpers ----------------
    @FXML
    private void setStatus(ActionEvent event) {
        String message = "Status updated.";
        if (event != null && event.getSource() instanceof Button btn) {
            Object data = btn.getUserData();
            if (data != null) {
                message = String.valueOf(data);
            } else if (btn.getText() != null && !btn.getText().isBlank()) {
                message = btn.getText();
            }
        }
        setStatusMessage(message);

        if (event != null && event.getSource() instanceof Button btn) {
            String btnText = btn.getText() != null ? btn.getText().toLowerCase() : "";
            if (btnText.contains("clear")) {
                if (txtUsername != null) txtUsername.clear();
                if (txtPassword != null) txtPassword.clear();
                if (txtUsername != null) txtUsername.requestFocus();
            }
        }
    }

    private void setStatusMessage(String msg) {
        if (lblStatus != null) {
            lblStatus.setText(msg != null ? msg : "");
        }
    }

    /**
     * Swap scene root safely: if btnLogin is not injected, attempt to find Stage via any node.
     */
    private void swapSceneSafely(Parent root, String title) {
        if (root == null) return;

        Stage stage = null;
        try {
            if (btnLogin != null && btnLogin.getScene() != null) {
                stage = (Stage) btnLogin.getScene().getWindow();
            }
        } catch (Exception ignored) { stage = null; }

        if (stage == null) {
            // fallback: try other nodes
            if (root.getScene() != null) {
                stage = (Stage) root.getScene().getWindow();
            } else if (loginArea != null && loginArea.getScene() != null) {
                stage = (Stage) loginArea.getScene().getWindow();
            }
        }

        if (stage != null) {
            stage.setTitle(title);
            if (stage.getScene() != null) {
                stage.getScene().setRoot(root);
            } else {
                stage.setScene(new Scene(root));
            }
            stage.sizeToScene();
            stage.setResizable(true);
        } else {
            // Last resort: open a new stage
            Stage newStage = new Stage();
            newStage.setTitle(title);
            newStage.setScene(new Scene(root));
            newStage.show();
        }
    }
}


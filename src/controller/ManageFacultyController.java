package controller;

import util.DatabaseConnection;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import org.mindrot.jbcrypt.BCrypt;
import model.FacultyModel;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ManageFacultyController — updated to safely update TableView items to avoid
 * JavaFX selection/list-change races that caused IndexOutOfBoundsException.
 */
public class ManageFacultyController {

    @FXML private VBox departmentField;

    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField phoneField;
    @FXML private ComboBox<String> departmentCombo; // used for filtering and add/update

    @FXML private TableView<FacultyModel> facultyTable;
    @FXML private TableColumn<FacultyModel, Integer> idCol;
    @FXML private TableColumn<FacultyModel, String> nameCol;
    @FXML private TableColumn<FacultyModel, String> emailCol;
    @FXML private TableColumn<FacultyModel, String> phoneCol;
    @FXML private TableColumn<FacultyModel, String> departmentCol;
    @FXML private TableColumn<FacultyModel, String> roleCol;
    
    
    @FXML private Button btnAdd;
    @FXML private Button btnUpdate;
    @FXML private Button btnDelete;
    @FXML private Button btnClear;

    // Keep a single ObservableList instance and reuse it via setAll().
    private final ObservableList<FacultyModel> facultyList = FXCollections.observableArrayList();

    // Guard flag while updating to block user interactions
    private volatile boolean updating = false;

    @SuppressWarnings({ "unused", "deprecation" })
    @FXML
    public void initialize() {
        try {
            // Defensive: ensure columns exist before wiring
            if (idCol != null) idCol.setCellValueFactory(data -> data.getValue().idProperty().asObject());
            if (nameCol != null) nameCol.setCellValueFactory(data -> data.getValue().nameProperty());
            if (emailCol != null) emailCol.setCellValueFactory(data -> data.getValue().emailProperty());
            if (phoneCol != null) phoneCol.setCellValueFactory(data -> data.getValue().phoneProperty());
            if (departmentCol != null) departmentCol.setCellValueFactory(data -> data.getValue().departmentProperty());
            if (roleCol != null) roleCol.setCellValueFactory(data -> data.getValue().roleProperty());

            if (facultyTable != null) {
                // bind the single ObservableList instance to the table (never replace)
                facultyTable.setItems(facultyList);

                // Use constrained resize policy so columns expand/shrink with table width
                facultyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

                // Prevent mouse presses while we are updating rows or if list empty (avoids selection race)
                facultyTable.addEventFilter(MouseEvent.MOUSE_PRESSED, ev -> {
                    if (updating || facultyList.isEmpty() || facultyTable.isDisabled()) {
                        ev.consume();
                    }
                });

                // Ensure single selection for stability
                try {
                    facultyTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
                    facultyTable.getSelectionModel().setCellSelectionEnabled(false);
                } catch (Exception ignored) {}

                // Responsive column widths: proportions (sum = 1.0)
                Platform.runLater(() -> {
                    if (facultyTable.getColumns().isEmpty()) return;

                    TableColumn<FacultyModel, ?> cId = idCol;
                    TableColumn<FacultyModel, ?> cName = nameCol;
                    TableColumn<FacultyModel, ?> cEmail = emailCol;
                    TableColumn<FacultyModel, ?> cPhone = phoneCol;
                    TableColumn<FacultyModel, ?> cDept = departmentCol;
                    TableColumn<FacultyModel, ?> cRole = roleCol;

                    facultyTable.widthProperty().addListener((obs, oldW, newW) -> {
                        double w = newW.doubleValue() - 2; // small safety offset
                        if (w <= 0) return;
                        if (cId != null)    cId.setPrefWidth(Math.max(50, w * 0.08));
                        if (cName != null)  cName.setPrefWidth(Math.max(120, w * 0.24));
                        if (cEmail != null) cEmail.setPrefWidth(Math.max(160, w * 0.28));
                        if (cPhone != null) cPhone.setPrefWidth(Math.max(100, w * 0.16));
                        if (cDept != null)  cDept.setPrefWidth(Math.max(110, w * 0.16));
                        if (cRole != null)  cRole.setPrefWidth(Math.max(80, w * 0.08));
                    });

                    // initial layout
                    double w0 = facultyTable.getWidth() > 0 ? facultyTable.getWidth() : 900;
                    facultyTable.setPrefWidth(w0);
                });
            }

            // Fill department choices
            if (departmentCombo != null) {
                departmentCombo.setItems(FXCollections.observableArrayList("CSE", "Civil", "Mechanical", "Electrical"));
            }

            // Listener for filtering table by department
            if (departmentCombo != null) {
                departmentCombo.valueProperty().addListener((obs, oldVal, newVal) -> loadFaculty());
            }

            // Table row -> fill form (listener should not mutate the list)
            if (facultyTable != null) {
                facultyTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
                    if (n != null) {
                        if (nameField != null) nameField.setText(n.getName());
                        if (emailField != null) emailField.setText(n.getEmail());
                        if (phoneField != null) phoneField.setText(n.getPhone());
                        if (departmentCombo != null) departmentCombo.setValue(n.getDepartment()); // keeps form + filter in sync
                        if (passwordField != null) passwordField.clear();
                    }
                });
            }

            // Make input fields responsive to the departmentField width (if departmentField present)
            Platform.runLater(() -> {
                if (departmentField != null) {
                    double minFieldWidth = 220;
                    departmentField.widthProperty().addListener((obs, oldW, newW) -> {
                        double base = newW.doubleValue() - 24;
                        double pref = Math.max(minFieldWidth, base * 0.95);
                        if (nameField != null) nameField.setPrefWidth(pref);
                        if (emailField != null) emailField.setPrefWidth(pref);
                        if (passwordField != null) passwordField.setPrefWidth(pref);
                        if (phoneField != null) phoneField.setPrefWidth(pref);
                        if (departmentCombo != null) departmentCombo.setPrefWidth(Math.max(160, pref));
                    });
                } else {
                    // fallback: bind to table width / window size (so it still grows)
                    if (facultyTable != null) {
                        facultyTable.widthProperty().addListener((obs, oldW, newW) -> {
                            double base = newW.doubleValue() * 0.25;
                            double pref = Math.max(220, base);
                            if (nameField != null) nameField.setPrefWidth(pref);
                            if (emailField != null) emailField.setPrefWidth(pref);
                            if (passwordField != null) passwordField.setPrefWidth(pref);
                            if (phoneField != null) phoneField.setPrefWidth(pref);
                            if (departmentCombo != null) departmentCombo.setPrefWidth(Math.max(160, pref));
                        });
                    }
                }
            });

            // Load initial data (all faculty)
            loadFaculty();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * loadFaculty: builds a fresh list off-FX-thread (synchronously here) then safely replaces the table rows.
     */
    @FXML
    private void loadFaculty() {
        // Build new temporary list then replace via safeSetRows(...)
        List<FacultyModel> newList = new ArrayList<>();

        String selectedDept = departmentCombo == null ? null : departmentCombo.getValue();
        String sql;

        if (selectedDept == null || selectedDept.isBlank()) {
            sql = "SELECT id, name, email, username, password, phone, department, role FROM faculty ORDER BY id DESC";
        } else {
            sql = "SELECT id, name, email, username, password, phone, department, role FROM faculty WHERE department = ? ORDER BY id DESC";
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            if (selectedDept != null && !selectedDept.isBlank()) {
                pst.setString(1, selectedDept);
            }

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    FacultyModel fm = new FacultyModel(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("email"),
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("phone"),
                            rs.getString("department"),
                            rs.getString("role")
                    );
                    newList.add(fm);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database error", "Unable to load faculty: " + e.getMessage());
        }

        // Replace table rows safely
        safeSetRows(newList);
    }

    @FXML
    private void addFaculty() {
        final String sql = "INSERT INTO faculty (name, email, username, password, phone, department, role) VALUES (?, ?, ?, ?, ?, ?, ?)";
        String name = nameField == null ? null : nameField.getText();
        String email = emailField == null ? null : emailField.getText();
        String username = (email == null || email.isBlank()) ? null : email;
        String plain = passwordField == null ? null : passwordField.getText();
        String phone = phoneField == null ? null : phoneField.getText();
        String department = departmentCombo == null ? null : departmentCombo.getValue();
        String role = "faculty";

        if (name == null || name.isBlank() || email == null || email.isBlank() || department == null) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Name, Email, and Department are required.");
            return;
        }

        String hashed = null;
        if (plain != null && !plain.isBlank()) {
            hashed = BCrypt.hashpw(plain, BCrypt.gensalt(12));
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setString(1, name);
            pst.setString(2, email);
            pst.setString(3, username);
            pst.setString(4, hashed);
            pst.setString(5, phone);
            pst.setString(6, department);
            pst.setString(7, role);

            pst.executeUpdate();

            loadFaculty();
            clearFields();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Faculty added successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database error", "Unable to add faculty: " + e.getMessage());
        }
    }

    @FXML
    private void updateFaculty() {
        FacultyModel sel = facultyTable == null ? null : facultyTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showAlert(Alert.AlertType.WARNING, "No selection", "Please select a faculty member to update.");
            return;
        }

        final String sql = "UPDATE faculty SET name=?, email=?, username=?, password=?, phone=?, department=?, role=? WHERE id=?";

        String name = nameField == null ? sel.getName() : nameField.getText();
        String email = emailField == null ? sel.getEmail() : emailField.getText();
        String username = (email == null || email.isBlank()) ? sel.getUsername() : email;
        String plain = passwordField == null ? null : passwordField.getText();
        String phone = phoneField == null ? sel.getPhone() : phoneField.getText();
        String department = departmentCombo == null ? sel.getDepartment() : departmentCombo.getValue();
        String role = sel.getRole();
        if (role == null || role.isBlank()) role = "faculty";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setString(1, name);
            pst.setString(2, email);
            pst.setString(3, username);

            if (plain != null && !plain.isBlank()) {
                String hashed = BCrypt.hashpw(plain, BCrypt.gensalt(12));
                pst.setString(4, hashed);
            } else {
                pst.setString(4, sel.getPassword());
            }

            pst.setString(5, phone);
            pst.setString(6, department);
            pst.setString(7, role);
            pst.setInt(8, sel.getId());

            pst.executeUpdate();

            loadFaculty();
            clearFields();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Faculty updated successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database error", "Unable to update faculty: " + e.getMessage());
        }
    }

    @FXML
    private void deleteFaculty() {
        FacultyModel sel = facultyTable == null ? null : facultyTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showAlert(Alert.AlertType.WARNING, "No selection", "Please select a faculty member to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete selected faculty?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement pst = conn.prepareStatement("DELETE FROM faculty WHERE id = ?")) {

                    pst.setInt(1, sel.getId());
                    pst.executeUpdate();

                    loadFaculty();
                    clearFields();
                    showAlert(Alert.AlertType.INFORMATION, "Deleted", "Faculty deleted successfully.");
                } catch (SQLException e) {
                    e.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Database error", "Unable to delete faculty: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void clearFields() {
        if (nameField != null) nameField.clear();
        if (emailField != null) emailField.clear();
        if (passwordField != null) passwordField.clear();
        if (phoneField != null) phoneField.clear();
        if (departmentCombo != null) departmentCombo.setValue(null); // resets both form + filter
        if (facultyTable != null) {
            try { facultyTable.getSelectionModel().clearSelection(); } catch (Exception ignored) {}
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Platform.runLater(() -> {
            Alert a = new Alert(type);
            a.setTitle(title);
            a.setHeaderText(null);
            a.setContentText(message);
            a.showAndWait();
        });
    }

    /**
     * Safely replace the rows displayed in the table.
     * Use this instead of facultyList.clear() / facultyTable.setItems(newList).
     */
    private void safeSetRows(List<FacultyModel> newRows) {
        // set guard early so event filter can see it immediately
        updating = true;

        // schedule replacement on FX thread
        Platform.runLater(() -> {
            boolean wasDisabled = facultyTable != null && facultyTable.isDisabled();
            try {
                if (facultyTable != null) facultyTable.setDisable(true);
                try { if (facultyTable != null) facultyTable.getSelectionModel().clearSelection(); } catch (Exception ignored) {}

                // reuse same ObservableList instance
                facultyList.setAll(newRows);

                if (facultyTable != null) facultyTable.refresh();
            } finally {
                if (facultyTable != null) facultyTable.setDisable(wasDisabled);
                updating = false;
            }
        });
    }

    public static List<FacultyModel> fetchAllFaculty() {
        List<FacultyModel> out = new ArrayList<>();
        String sql = "SELECT id, name, email, username, password, phone, department, role FROM faculty ORDER BY id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {
                FacultyModel fm = new FacultyModel(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("phone"),
                        rs.getString("department"),
                        rs.getString("role")
                );
                out.add(fm);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return out;
    }
}

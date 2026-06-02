package controller;

import dao.SubjectDAO;
import model.Subject;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.*;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.util.converter.IntegerStringConverter;
import javafx.scene.input.MouseEvent;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * ManageSubjectsController (responsive + safe table updates)
 *
 * - Adds safeSetRows(...) + updating guard to avoid JavaFX selection/list-change races.
 * - Defensive null checks so this controller is robust if some controls are not injected.
 */
public class ManageSubjectsController {

    @FXML private TableView<Subject> tblSubjects;
    @FXML private TableColumn<Subject, Integer> colId;
    @FXML private TableColumn<Subject, String> colCode;
    @FXML private TableColumn<Subject, String> colName;
    @FXML private TableColumn<Subject, Integer> colSem;
    @FXML private TableColumn<Subject, String> colDept;

    @FXML private ComboBox<Integer> cmbFilterSem; // semester filter for table
    @FXML private ComboBox<Integer> cmbAddSem;    // semester for add form
    @FXML private TextField txtCode;
    @FXML private TextField txtName;
    @FXML private TextField txtDepartment;

    @FXML private Label lblStatus;

    @FXML private Button btnAdd;
    @FXML private Button btnDelete;
    @FXML private Button btnClear;

    private final SubjectDAO subjectDAO = new SubjectDAO();

    // single ObservableList instance reused via setAll(...) — do NOT replace this instance
    private final ObservableList<Subject> subjects = FXCollections.observableArrayList();

    // guard used while updating the table rows to avoid selection races
    private volatile boolean updating = false;

    @SuppressWarnings({ "unused", "deprecation" })
    @FXML
    private void initialize() {
        // populate combo boxes 1..8
        ObservableList<Integer> sems = FXCollections.observableArrayList(IntStream.rangeClosed(1, 8).boxed().toList());
        if (cmbFilterSem != null) cmbFilterSem.setItems(sems);
        if (cmbAddSem != null) cmbAddSem.setItems(sems);

        // default selection for filter
        if (cmbFilterSem != null && !sems.isEmpty()) cmbFilterSem.getSelectionModel().selectFirst();

        // Table configuration (defensive guards)
        if (tblSubjects != null) {
            tblSubjects.setEditable(true);
            // Use constrained resize policy so columns share available width
            tblSubjects.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

            // Prevent mouse presses while we are updating rows (avoids selection race)
            tblSubjects.addEventFilter(MouseEvent.MOUSE_PRESSED, ev -> {
                if (updating || subjects.isEmpty() || tblSubjects.isDisabled()) {
                    ev.consume();
                }
            });
            // Keep a single ObservableList instance bound to the table
            tblSubjects.setItems(subjects);
        }

        if (colId != null)
            colId.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getId()).asObject());
        if (colCode != null)
            colCode.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCode()));
        if (colName != null)
            colName.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getName()));
        if (colSem != null)
            colSem.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getSemester()).asObject());
        if (colDept != null)
            colDept.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDepartment()));

        // Make code/name/department editable via text cell
        if (colCode != null) {
            colCode.setCellFactory(TextFieldTableCell.forTableColumn());
            colCode.setOnEditCommit(evt -> {
                Subject s = evt.getRowValue();
                if (s != null) {
                    s.setCode(evt.getNewValue());
                    persistEdit(s);
                }
            });
        }

        if (colName != null) {
            colName.setCellFactory(TextFieldTableCell.forTableColumn());
            colName.setOnEditCommit(evt -> {
                Subject s = evt.getRowValue();
                if (s != null) {
                    s.setName(evt.getNewValue());
                    persistEdit(s);
                }
            });
        }

        if (colDept != null) {
            colDept.setCellFactory(TextFieldTableCell.forTableColumn());
            colDept.setOnEditCommit(evt -> {
                Subject s = evt.getRowValue();
                if (s != null) {
                    s.setDepartment(evt.getNewValue());
                    persistEdit(s);
                }
            });
        }

        // Semester editable via ComboBox inside the table
        if (colSem != null) {
            colSem.setCellFactory(ComboBoxTableCell.forTableColumn(new IntegerStringConverter(), sems));
            colSem.setOnEditCommit(evt -> {
                Subject s = evt.getRowValue();
                Integer newSem = evt.getNewValue();
                if (s != null && newSem != null) {
                    s.setSemester(newSem);
                    persistEdit(s);
                }
            });
        }

        // load initial subjects for selected semester
        if (cmbFilterSem != null) {
            cmbFilterSem.valueProperty().addListener((obs, oldv, newv) -> loadSubjects());
        }

        // Responsive bindings (run later when layout is ready)
        Platform.runLater(this::applyResponsiveBindings);

        // initial load
        loadSubjects();
    }

    /**
     * Apply responsive bindings for table columns and form fields.
     * Keeps proportions but respects sensible minimum widths.
     */
    @SuppressWarnings("unused")
    private void applyResponsiveBindings() {
        if (tblSubjects == null) return;

        // Column proportions (sum roughly = 1.0)
        // id: 8%, code: 20%, name: 36%, sem: 12%, dept: 24%
        final double P_ID = 0.08;
        final double P_CODE = 0.20;
        final double P_NAME = 0.36;
        final double P_SEM = 0.12;
        final double P_DEPT = 0.24;

        // Minimum widths to avoid collapse
        final double MIN_ID = 40;
        final double MIN_CODE = 100;
        final double MIN_NAME = 160;
        final double MIN_SEM = 70;
        final double MIN_DEPT = 120;

        tblSubjects.widthProperty().addListener((obs, oldW, newW) -> {
            double total = newW.doubleValue() - 2; // little offset
            if (total <= 0) return;

            if (colId != null)   colId.setPrefWidth(Math.max(MIN_ID, total * P_ID));
            if (colCode != null) colCode.setPrefWidth(Math.max(MIN_CODE, total * P_CODE));
            if (colName != null) colName.setPrefWidth(Math.max(MIN_NAME, total * P_NAME));
            if (colSem != null)  colSem.setPrefWidth(Math.max(MIN_SEM, total * P_SEM));
            if (colDept != null) colDept.setPrefWidth(Math.max(MIN_DEPT, total * P_DEPT));
        });

        // Also set an initial width preference so layout is correct on first show
        double initial = tblSubjects.getWidth() > 0 ? tblSubjects.getWidth() : 900;
        if (colId != null)   colId.setPrefWidth(Math.max(MIN_ID, initial * P_ID));
        if (colCode != null) colCode.setPrefWidth(Math.max(MIN_CODE, initial * P_CODE));
        if (colName != null) colName.setPrefWidth(Math.max(MIN_NAME, initial * P_NAME));
        if (colSem != null)  colSem.setPrefWidth(Math.max(MIN_SEM, initial * P_SEM));
        if (colDept != null) colDept.setPrefWidth(Math.max(MIN_DEPT, initial * P_DEPT));

        // Bind form input widths to the table width so the add form scales with available area
        double minField = 220;
        tblSubjects.widthProperty().addListener((obs, oldW, newW) -> {
            double base = newW.doubleValue() * 0.30; // use ~30% of table width for form fields
            double pref = Math.max(minField, base);
            if (txtCode != null) txtCode.setPrefWidth(pref);
            if (txtName != null) txtName.setPrefWidth(pref);
            if (txtDepartment != null) txtDepartment.setPrefWidth(pref);
            if (cmbAddSem != null) cmbAddSem.setPrefWidth(Math.max(100, pref * 0.5));
        });

        // Initial form pref widths
        double base = initial * 0.30;
        double pref = Math.max(minField, base);
        if (txtCode != null) txtCode.setPrefWidth(pref);
        if (txtName != null) txtName.setPrefWidth(pref);
        if (txtDepartment != null) txtDepartment.setPrefWidth(pref);
        if (cmbAddSem != null) cmbAddSem.setPrefWidth(Math.max(100, pref * 0.5));
    }

    // Load subjects depending on filter selection (if none selected, load all)
    private void loadSubjects() {
        // Build a temp list then replace via safeSetRows(...) to avoid selection races
        List<Subject> list;
        try {
            if (cmbFilterSem != null && cmbFilterSem.getValue() != null) {
                list = subjectDAO.findBySemester(cmbFilterSem.getValue());
            } else {
                list = subjectDAO.findAll();
            }
        } catch (Exception e) {
            e.printStackTrace();
            setStatus("Error loading subjects: " + e.getMessage());
            return;
        }

        if (list == null) {
            setStatus("Loaded 0 subject(s).");
            safeSetRows(List.of());
        } else {
            safeSetRows(list);
            setStatus("Loaded " + list.size() + " subject(s).");
        }
    }

    // Persist an edited subject to DB; refresh table so filter rules are respected
    private void persistEdit(Subject s) {
        boolean ok = false;
        try {
            ok = subjectDAO.update(s);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (ok) {
            setStatus("Updated subject ID " + s.getId());
            // if the semester changed and doesn't match current filter, reload will remove the row from view
            loadSubjects();
        } else {
            setStatus("Failed to update subject ID " + s.getId());
            // reload from db to revert unsaved changes
            loadSubjects();
        }
    }

    @FXML
    private void onAddSubject() {
        String code = txtCode == null ? "" : txtCode.getText().trim();
        String name = txtName == null ? "" : txtName.getText().trim();
        Integer sem = (cmbAddSem == null) ? null : cmbAddSem.getValue();
        String dept = txtDepartment == null ? null : txtDepartment.getText().trim();

        if (code.isEmpty() || name.isEmpty() || sem == null) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Please fill Code, Name and select Semester.");
            return;
        }

        Subject s = new Subject(code, name, sem, dept);
        boolean ok = false;
        try {
            ok = subjectDAO.insert(s);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (ok) {
            setStatus("Added subject.");
            onClearForm();
            loadSubjects();
        } else {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to add subject.");
        }
    }

    @FXML
    private void onDeleteSubject() {
        Subject sel = tblSubjects == null ? null : tblSubjects.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showAlert(Alert.AlertType.WARNING, "Select", "Please select a subject to delete.");
            return;
        }

        Alert conf = new Alert(Alert.AlertType.CONFIRMATION, "Delete subject: " + sel.getName() + " ?", ButtonType.OK, ButtonType.CANCEL);
        conf.setHeaderText("Confirm delete");
        Optional<ButtonType> res = conf.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.OK) return;

        boolean ok = false;
        try {
            ok = subjectDAO.delete(sel.getId());
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (ok) {
            setStatus("Deleted subject ID " + sel.getId());
            loadSubjects();
        } else {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete subject.");
        }
    }

    @FXML
    private void onClearForm() {
        if (txtCode != null) txtCode.clear();
        if (txtName != null) txtName.clear();
        if (cmbAddSem != null) cmbAddSem.getSelectionModel().clearSelection();
        if (txtDepartment != null) txtDepartment.clear();
        if (tblSubjects != null) {
            try { tblSubjects.getSelectionModel().clearSelection(); } catch (Exception ignored) {}
        }
    }

    @FXML
    private void onRefresh() {
        loadSubjects();
    }

    private void setStatus(String text) {
        if (lblStatus != null) lblStatus.setText(text);
    }

    private void showAlert(Alert.AlertType t, String header, String body) {
        Alert a = new Alert(t, body, ButtonType.OK);
        a.setHeaderText(header);
        a.showAndWait();
    }

    /**
     * Safely replace the rows displayed in the table to avoid JavaFX selection/list-change races.
     * Use this instead of subjects.clear(); subjects.addAll(...).
     */
    private void safeSetRows(List<Subject> newRows) {
        // set guard early so event filter can see it immediately
        updating = true;

        Platform.runLater(() -> {
            boolean wasDisabled = tblSubjects != null && tblSubjects.isDisabled();
            try {
                if (tblSubjects != null) tblSubjects.setDisable(true);
                try { if (tblSubjects != null) tblSubjects.getSelectionModel().clearSelection(); } catch (Exception ignored) {}

                // reuse same ObservableList instance
                subjects.setAll(newRows == null ? List.of() : newRows);

                if (tblSubjects != null) tblSubjects.refresh();
            } finally {
                if (tblSubjects != null) tblSubjects.setDisable(wasDisabled);
                updating = false;
            }
        });
    }
}

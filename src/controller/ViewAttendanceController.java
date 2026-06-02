package controller;

import util.DatabaseConnection;
import model.Subject;
import dao.FacultySubjectDAO;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Region;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * ViewAttendanceController — robust: detects attendance-date column name at runtime
 * and supports All/Present/Absent filters (right-side buttons). The right-side
 * Semester combo was intentionally removed.
 */
public class ViewAttendanceController {

    @FXML private ComboBox<String> comboSemester;     // left-side semester (keeps existing left filters)
    @FXML private ComboBox<String> comboCourse;
    @FXML private ComboBox<Subject> comboSubject;
    @FXML private DatePicker datePicker;
    @FXML private Button btnLoad;
    @FXML private Button btnRefreshSubjects;

    @FXML private Label lblTotal;
    @FXML private Label lblPresent;
    @FXML private Label lblAbsent;

    @FXML private TableView<StudentRow> tblStudents;
    @FXML private TableColumn<StudentRow, Integer> colId;
    @FXML private TableColumn<StudentRow, String> colName;
    @FXML private TableColumn<StudentRow, String> colRoll;
    @FXML private TableColumn<StudentRow, String> colCourse;
    @FXML private TableColumn<StudentRow, String> colSemester;
    @FXML private TableColumn<StudentRow, String> colStatus;

    // master list of rows as loaded from DB for a particular (semester+course+subject+date)
    private final ObservableList<StudentRow> masterRows = FXCollections.observableArrayList();
    // filtered view (what table shows) - updated by filter methods
    private final ObservableList<StudentRow> filteredRows = FXCollections.observableArrayList();

    // optional faculty context
    private int facultyId = -1;

    // candidate names for date column in attendance table
    private static final String[] DATE_COL_CANDIDATES = {
            "att_date", "attendance_date", "date", "attdate", "att_dt", "attday", "att_on"
    };

    // current filter state: "ALL", "PRESENT", "ABSENT"
    private String currentFilter = "ALL";

    @SuppressWarnings({ "unused", "deprecation" })
	@FXML
    private void initialize() {
        try {
            // populate semester and course combos
            if (comboSemester != null) comboSemester.getItems().addAll("1","2","3","4","5","6","7","8");
            if (comboCourse != null) comboCourse.getItems().addAll("CSE","Civil","Electrical","Mechanical");

            // default date
            if (datePicker != null) datePicker.setValue(LocalDate.now());

            // table columns
            if (colId != null) colId.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getId()));
            if (colName != null) colName.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getName()));
            if (colRoll != null) colRoll.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getRoll()));
            if (colCourse != null) colCourse.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getCourse()));
            if (colSemester != null) colSemester.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getSemester()));

            if (colStatus != null) {
                colStatus.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getStatus()));
                colStatus.setCellFactory(col -> new TableCell<>() {
                    private final Label lbl = new Label();
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setGraphic(null);
                            setText(null);
                        } else {
                            lbl.setText(item);
                            lbl.setMinWidth(60);
                            lbl.setStyle("-fx-padding:6 10; -fx-background-radius:8;");
                            if (item.equalsIgnoreCase("P") || item.equalsIgnoreCase("Present")) {
                                lbl.setStyle(lbl.getStyle() + " -fx-background-color: rgba(40,167,69,0.12); -fx-text-fill:#28a745; -fx-font-weight:600;");
                            } else {
                                lbl.setStyle(lbl.getStyle() + " -fx-background-color: rgba(220,53,69,0.06); -fx-text-fill:#dc3545; -fx-font-weight:600;");
                            }
                            setGraphic(lbl);
                        }
                    }
                });
            }

            if (tblStudents != null) {
                tblStudents.setItems(filteredRows); // table shows filteredRows
                tblStudents.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            }

            // wire refresh subjects button (also supported by FXML onAction)
            if (btnRefreshSubjects != null) btnRefreshSubjects.setOnAction(e -> loadSubjectsForFaculty());

            // attempt to auto-load subjects after UI shown
            Platform.runLater(this::loadSubjectsForFaculty);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Optional: set faculty context to filter subjects by faculty.
     */
    public void setFacultyContext(int facultyId) {
        this.facultyId = facultyId;
        loadSubjectsForFaculty();
    }

    /** wired from FXML */
    @FXML
    private void handleRefreshSubjects() {
        loadSubjectsForFaculty();
    }

    /** wired from FXML */
    @FXML
    private void handleLoad() {
        String sem = comboSemester == null ? null : comboSemester.getSelectionModel().getSelectedItem();
        String course = comboCourse == null ? null : comboCourse.getSelectionModel().getSelectedItem();
        Subject subj = comboSubject == null ? null : comboSubject.getSelectionModel().getSelectedItem();
        LocalDate date = datePicker == null ? LocalDate.now() : datePicker.getValue();
        if (sem == null || sem.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Missing Semester", "Please select a semester.");
            return;
        }
        if (course == null || course.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Missing Course", "Please select a course.");
            return;
        }
        if (subj == null) {
            showAlert(Alert.AlertType.WARNING, "Missing Subject", "Please select a subject.");
            return;
        }
        if (date == null) date = LocalDate.now();

        try {
            String dateColumn = detectAttendanceDateColumn();
            if (dateColumn == null) {
                showAlert(Alert.AlertType.ERROR, "Schema error",
                        "Could not find any recognized date column in the attendance table. " +
                                "Check your schema or modify DATE_COL_CANDIDATES.");
                return;
            }
            loadAttendanceFor(dateColumn, subj.getId(), sem, course, date);
        } catch (Throwable t) {
            t.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Load failed", "An error occurred: " + t.getMessage());
        }
    }

    /**
     * Populate subjects combo using FacultySubjectDAO if possible.
     */
    private void loadSubjectsForFaculty() {
        try {
            int fid = facultyId;
            if (fid <= 0) {
                try { fid = util.Session.getFacultyId(); } catch (Throwable ignored) {}
            }
            if (comboSubject == null) return;

            comboSubject.getItems().clear();
            if (fid <= 0) return;

            List<Subject> subjects = FacultySubjectDAO.findSubjectsByFaculty(fid);
            if (subjects == null || subjects.isEmpty()) return;
            comboSubject.getItems().addAll(subjects);
            comboSubject.getSelectionModel().selectFirst();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * Detect which column in `attendance` is the date column.
     * Returns the first match from DATE_COL_CANDIDATES that exists.
     */
    private String detectAttendanceDateColumn() {
        String table = "attendance"; // adjust if your table name differs
        String schema = null; // will use connection.getCatalog()

        try (Connection conn = DatabaseConnection.getConnection()) {
            schema = conn.getCatalog();
            for (String candidate : DATE_COL_CANDIDATES) {
                String q = "SELECT COUNT(*) FROM information_schema.columns " +
                        "WHERE table_schema = ? AND table_name = ? AND column_name = ?";
                try (PreparedStatement ps = conn.prepareStatement(q)) {
                    ps.setString(1, schema);
                    ps.setString(2, table);
                    ps.setString(3, candidate);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            return candidate; // found
                        }
                    }
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return null;
    }

    /**
     * Query DB to get students for semester+course, left join attendance on subject+date.
     *
     * @param dateColumn name of the date column in attendance table (already validated)
     */
    private void loadAttendanceFor(String dateColumn, int subjectId, String semester, String course, LocalDate date) {
        masterRows.clear();
        filteredRows.clear();

        String sql =
                "SELECT s.id, s.name, s.roll_number, s.course, s.semester, " +
                        "       a.status " +
                        "FROM students s " +
                        "LEFT JOIN attendance a ON a.student_id = s.id AND a.subject_id = ? AND a." + dateColumn + " = ? " +
                        "WHERE s.semester = ? AND s.course = ? " +
                        "ORDER BY s.name";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, subjectId);
            ps.setDate(2, Date.valueOf(date));
            ps.setString(3, semester);
            ps.setString(4, course);

            int total = 0, present = 0, absent = 0;
            try (ResultSet rs = ps.executeQuery()) {
                List<StudentRow> list = new ArrayList<>();
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String name = rs.getString("name");
                    String roll = rs.getString("roll_number");
                    String courseVal = rs.getString("course");
                    String semVal = rs.getString("semester");
                    String status = rs.getString("status"); // may be null
                    String finalStatus;
                    if (status == null) {
                        finalStatus = "Absent";
                    } else if (status.equalsIgnoreCase("P") || status.equalsIgnoreCase("Present")) {
                        finalStatus = "Present";
                    } else {
                        finalStatus = "Absent";
                    }

                    if (finalStatus.equalsIgnoreCase("Present")) present++;
                    else absent++;
                    total++;

                    list.add(new StudentRow(id, name, roll, courseVal, semVal, finalStatus));
                }

                masterRows.addAll(list);

                // apply current filter to populate filteredRows and update UI
                applyFilter(currentFilter);

                final int tTotal = total;
                final int tPresent = present;
                final int tAbsent = absent;

                Platform.runLater(() -> {
                    if (lblTotal != null) lblTotal.setText(String.valueOf(tTotal));
                    if (lblPresent != null) lblPresent.setText(String.valueOf(tPresent));
                    if (lblAbsent != null) lblAbsent.setText(String.valueOf(tAbsent));
                    if (tblStudents != null) tblStudents.refresh();
                });
            }
        } catch (Throwable t) {
            t.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Load failed", "Could not load attendance: " + t.getMessage());
        }
    }

    /** Apply requested filter to masterRows -> filteredRows and refresh table. */
    private void applyFilter(String filter) {
        currentFilter = (filter == null) ? "ALL" : filter.toUpperCase();
        filteredRows.clear();

        switch (currentFilter) {
            case "PRESENT" -> masterRows.stream().filter(r -> r.getStatus().equalsIgnoreCase("Present")).forEach(filteredRows::add);
            case "ABSENT"  -> masterRows.stream().filter(r -> !r.getStatus().equalsIgnoreCase("Present")).forEach(filteredRows::add);
            default        -> filteredRows.addAll(masterRows);
        }

        Platform.runLater(() -> {
            if (tblStudents != null) {
                tblStudents.refresh();
                // keep selection cleared to avoid confusion
                try { tblStudents.getSelectionModel().clearSelection(); } catch (Exception ignored) {}
            }
        });
    }

    // FXML-bound filter handlers
    @FXML
    private void filterAll() { applyFilter("ALL"); }

    @FXML
    private void filterPresent() { applyFilter("PRESENT"); }

    @FXML
    private void filterAbsent() { applyFilter("ABSENT"); }

    private void showAlert(Alert.AlertType type, String header, String content) {
        Platform.runLater(() -> {
            Alert a = new Alert(type, content, ButtonType.OK);
            a.setHeaderText(header);
            a.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
            a.showAndWait();
        });
    }

    // Row model
    public static class StudentRow {
        private final int id;
        private final String name;
        private final String roll;
        private final String course;
        private final String semester;
        private final String status;

        public StudentRow(int id, String name, String roll, String course, String semester, String status) {
            this.id = id;
            this.name = name == null ? "" : name;
            this.roll = roll == null ? "" : roll;
            this.course = course == null ? "" : course;
            this.semester = semester == null ? "" : semester;
            this.status = status == null ? "Absent" : status;
        }

        public int getId() { return id; }
        public String getName() { return name; }
        public String getRoll() { return roll; }
        public String getCourse() { return course; }
        public String getSemester() { return semester; }
        public String getStatus() { return status; }
    }
}

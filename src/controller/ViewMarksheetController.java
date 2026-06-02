package controller;

import util.DatabaseConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.print.PrinterJob;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.control.TableCell;
import javafx.stage.Stage;

import java.sql.*;
import java.util.Objects;

/**
 * Controller for application/views/ViewMarksheet.fxml
 * Supports choosing a semester and a student from dropdowns and loading their marksheet.
 */
public class ViewMarksheetController {

    // root pane (used for printing/export)
    @FXML private BorderPane rootPane;

    // selectors
    @FXML private ComboBox<String> cmbSemester;
    @FXML private ComboBox<StudentItem> cmbStudent;
    @FXML private Label lblLoadingHint;

    // Student info labels
    @FXML private Label lblStudentName;
    @FXML private Label lblRoll;
    @FXML private Label lblCourse;
    @FXML private Label lblSemester;
    @FXML private Label lblAttendance;

    @FXML private Label lblSubTitle;

    // Summary labels
    @FXML private Label lblTotal;
    @FXML private Label lblAverage;
    @FXML private Label lblPercentage;

    // Table + columns
    @FXML private TableView<MarkRow> tblMarks;
    @FXML private TableColumn<MarkRow, Integer> colSno;
    @FXML private TableColumn<MarkRow, String> colSubject;
    @FXML private TableColumn<MarkRow, String> colCode;
    @FXML private TableColumn<MarkRow, Integer> colMarks;
    @FXML private TableColumn<MarkRow, String> colGrade;
    @FXML private TableColumn<MarkRow, String> colFaculty;
    @FXML private TableColumn<MarkRow, Integer> colSemester;

    private final ObservableList<MarkRow> marksData = FXCollections.observableArrayList();

    // currently loaded student id
    private int studentId = -1;

    @SuppressWarnings("unused")
	@FXML
    public void initialize() {
        // wire columns to MarkRow getters
        colSno.setCellValueFactory(new PropertyValueFactory<>("sno"));
        colSubject.setCellValueFactory(new PropertyValueFactory<>("subject"));
        colCode.setCellValueFactory(new PropertyValueFactory<>("code"));
        colMarks.setCellValueFactory(new PropertyValueFactory<>("marks"));
        colGrade.setCellValueFactory(new PropertyValueFactory<>("grade"));
        colFaculty.setCellValueFactory(new PropertyValueFactory<>("facultyName"));
        colSemester.setCellValueFactory(new PropertyValueFactory<>("semester"));

        // Serial number: visible index + 1 so it respects sort/filter
        colSno.setCellFactory(col -> new TableCell<MarkRow, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else {
                    int idx = getIndex();
                    if (idx >= 0 && idx < getTableView().getItems().size()) {
                        setText(String.valueOf(idx + 1));
                    } else setText("");
                }
            }
        });
        colSno.setSortable(false);

        tblMarks.setItems(marksData);

        // populate semester combobox
        if (cmbSemester != null) {
            cmbSemester.getItems().clear();
            cmbSemester.getItems().add("All");
            for (int i = 1; i <= 8; i++) cmbSemester.getItems().add(String.valueOf(i));
            cmbSemester.getSelectionModel().selectFirst();
            cmbSemester.valueProperty().addListener((obs, oldV, newV) -> {
                // reload students whenever semester changes
                loadStudentSelector();
            });
        }

        // populate student selector
        if (cmbStudent != null) {
            loadStudentSelector();
            // double-click to refresh student list
            cmbStudent.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2) loadStudentSelector();
            });
        }
    }

    /**
     * Populate student dropdown with id,name,roll from DB filtered by semester (if not "All").
     */
    private void loadStudentSelector() {
        if (cmbStudent == null) return;

        cmbStudent.getItems().clear();
        lblLoadingHint.setText("Loading...");

        String semSel = (cmbSemester == null) ? "All" : cmbSemester.getSelectionModel().getSelectedItem();
        boolean filterBySem = semSel != null && !semSel.equalsIgnoreCase("All");
        Integer semInt = null;
        if (filterBySem) {
            try { semInt = Integer.parseInt(semSel); } catch (NumberFormatException ignored) { semInt = null; }
        }

        String sql = "SELECT id, name, roll_number FROM students";
        if (semInt != null) sql += " WHERE semester = ?";
        sql += " ORDER BY name";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (semInt != null) ps.setInt(1, semInt);

            try (ResultSet rs = ps.executeQuery()) {
                boolean any = false;
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String name = rs.getString("name");
                    String roll = rs.getString("roll_number");
                    cmbStudent.getItems().add(new StudentItem(id, name, roll));
                    any = true;
                }
                if (!any) lblLoadingHint.setText("No students found for selected semester.");
                else lblLoadingHint.setText("");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            lblLoadingHint.setText("Failed to load students: " + ex.getMessage());
        }
    }

    /**
     * Load selected student when Load button clicked.
     */
    @FXML
    private void handleLoad() {
        StudentItem si = cmbStudent == null ? null : cmbStudent.getSelectionModel().getSelectedItem();
        if (si == null) {
            showError("Please select a student from the dropdown.");
            return;
        }
        loadStudent(si.getId());
    }

    /**
     * Load and display marksheet for an existing student id.
     */
    public void loadStudent(int studentId) {
        this.studentId = studentId;
        loadStudentInfo();
        loadMarks();
        computeSummary();
    }

    private void loadStudentInfo() {
        String sql = "SELECT id, name, roll_number, course, semester, attendance FROM students WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String name = rs.getString("name");
                    String roll = rs.getString("roll_number");
                    String course = rs.getString("course");
                    String semester = rs.getString("semester");
                    Integer attendance = rs.getObject("attendance") == null ? null : rs.getInt("attendance");

                    lblStudentName.setText(name != null ? name : "Unknown");
                    lblRoll.setText("Roll: " + (roll != null ? roll : "-"));
                    lblCourse.setText("Course: " + (course != null ? course : "-"));
                    lblSemester.setText("Semester: " + (semester != null ? semester : "-"));
                    lblAttendance.setText("Attendance: " + (attendance == null ? "-" : attendance + "%"));
                } else {
                    lblStudentName.setText("Student not found (id: " + studentId + ")");
                    lblRoll.setText("");
                    lblCourse.setText("");
                    lblSemester.setText("");
                    lblAttendance.setText("");
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            showError("Failed to load student info: " + ex.getMessage());
        }
    }

    private void loadMarks() {
        marksData.clear();

        String sql =
                "SELECT m.id AS mark_id, sub.name AS subject_name, sub.code AS subject_code, m.marks, m.grade, f.name AS faculty_name, m.semester " +
                "FROM marks m " +
                "JOIN subject sub ON m.subject_id = sub.id " +
                "LEFT JOIN faculty f ON m.faculty_id = f.id " +
                "WHERE m.student_id = ? " +
                "ORDER BY m.semester, sub.name";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Integer marks = rs.getObject("marks") == null ? null : rs.getInt("marks");
                    Integer sem = rs.getObject("semester") == null ? null : rs.getInt("semester");

                    MarkRow row = new MarkRow(
                            null, // sno handled by cell factory
                            rs.getString("subject_name"),
                            rs.getString("subject_code"),
                            marks,
                            rs.getString("grade"),
                            rs.getString("faculty_name"),
                            sem
                    );
                    marksData.add(row);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            showError("Failed to load marks: " + ex.getMessage());
        }
    }

    private void computeSummary() {
        int total = 0;
        int count = 0;
        for (MarkRow r : marksData) {
            if (r.getMarks() != null) {
                total += r.getMarks();
                count++;
            }
        }
        lblTotal.setText("Total: " + total);
        double avg = count == 0 ? 0.0 : (double) total / count;
        lblAverage.setText(String.format("Average: %.2f", avg));
        double percentage = (count == 0) ? 0.0 : (total / (double) (count * 100)) * 100.0;
        lblPercentage.setText(String.format("Percentage: %.2f%%", percentage));
    }

    /* ---------- FXML event handlers ---------- */

    @FXML
    private void onRefresh() {
        if (studentId <= 0) {
            showError("No student selected to refresh.");
            return;
        }
        loadStudent(studentId);
    }

    @FXML
    private void onExportPdf() {
        try {
            Stage stage = (Stage) Objects.requireNonNull(lblStudentName.getScene()).getWindow();
            PrinterJob job = PrinterJob.createPrinterJob();
            if (job == null) {
                showError("Cannot create a printer job on this system.");
                return;
            }

            boolean proceed = job.showPrintDialog(stage);
            if (!proceed) return;

            boolean printed = job.printPage(rootPane);
            if (printed) {
                job.endJob();
                showInfo("Export finished. If you chose a PDF printer, the PDF file was created.");
            } else {
                showError("Failed to print the marksheet. Try a different printer or check printer settings.");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Export failed: " + ex.getMessage());
        }
    }

    /* ---------- Helpers ---------- */

    private void showError(String message) {
        Alert a = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        a.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        a.showAndWait();
    }

    private void showInfo(String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        a.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        a.showAndWait();
    }

    /* ---------- Row model ---------- */

    public static class MarkRow {
        private Integer sno;
        private String subject;
        private String code;
        private Integer marks;
        private String grade;
        private String facultyName;
        private Integer semester;

        public MarkRow(Integer sno, String subject, String code, Integer marks, String grade, String facultyName, Integer semester) {
            this.sno = sno;
            this.subject = subject;
            this.code = code;
            this.marks = marks;
            this.grade = grade;
            this.facultyName = facultyName;
            this.semester = semester;
        }

        public Integer getSno() { return sno; }
        public String getSubject() { return subject; }
        public String getCode() { return code; }
        public Integer getMarks() { return marks; }
        public String getGrade() { return grade; }
        public String getFacultyName() { return facultyName; }
        public Integer getSemester() { return semester; }

        public void setSno(Integer sno) { this.sno = sno; }
        public void setSubject(String subject) { this.subject = subject; }
        public void setCode(String code) { this.code = code; }
        public void setMarks(Integer marks) { this.marks = marks; }
        public void setGrade(String grade) { this.grade = grade; }
        public void setFacultyName(String facultyName) { this.facultyName = facultyName; }
        public void setSemester(Integer semester) { this.semester = semester; }
    }

    /* ---------- Combo item model for students ---------- */
    public static class StudentItem {
        private final int id;
        private final String name;
        private final String roll;

        public StudentItem(int id, String name, String roll) {
            this.id = id;
            this.name = name == null ? "" : name;
            this.roll = roll == null ? "" : roll;
        }

        public int getId() { return id; }

        @Override
        public String toString() {
            if (!roll.isBlank()) return name + " (" + roll + ")";
            return name;
        }
    }
}

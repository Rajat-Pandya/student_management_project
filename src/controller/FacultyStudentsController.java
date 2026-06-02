package controller;

import dao.AttendanceDAO;
import dao.FacultySubjectDAO;
import dao.FacultyDAO;
import dao.StudentDAO;
import model.Subject;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.DateCell;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * FacultyStudentsController - fixed layout nudges so table rows are visible,
 * plus robust popup sequencing and table-stretch hints.
 */
public class FacultyStudentsController {

    @FXML private ComboBox<String> comboSemester;
    @FXML private ComboBox<String> comboCourse;
    @FXML private DatePicker datePicker;
    @FXML private TextField txtSearch;

    @FXML private TableView<StudentRow> tableStudents;
    @FXML private TableColumn<StudentRow, Integer> colId;
    @FXML private TableColumn<StudentRow, String> colName;
    @FXML private TableColumn<StudentRow, String> colRoll;
    @FXML private TableColumn<StudentRow, String> colCourse;
    @FXML private TableColumn<StudentRow, String> colSemester;
    @FXML private TableColumn<StudentRow, Boolean> colToday;
    @FXML private TableColumn<StudentRow, String> colGrade;

    @FXML private Button btnSave;
    @FXML private Button btnSelectAll;
    @FXML private Button btnDeselectAll;
    @FXML private Button btnLoad;
    @FXML private Button btnChangeSubject;

    @FXML private Button btnSearch;
    @FXML private Button btnReset;

    @FXML private Label lblSelectedSubject;

    private int facultyId = -1;
    @SuppressWarnings("unused")
    private String facultyUsername;
    @SuppressWarnings("unused")
    private String facultyDepartment;

    private Subject selectedSubject = null;
    private final ObservableList<StudentRow> rows = FXCollections.observableArrayList();

    // Guard used to prevent user interaction while we are updating rows (queued with Platform.runLater)
    private volatile boolean updating = false;

    @SuppressWarnings({ "unused", "deprecation" })
    @FXML
    private void initialize() {
        try {
            // DatePicker default to today and disable future dates
            if (datePicker != null) {
                datePicker.setValue(LocalDate.now());
                datePicker.setDayCellFactory(picker -> new DateCell() {
                    @Override
                    public void updateItem(LocalDate date, boolean empty) {
                        super.updateItem(date, empty);
                        if (date == null) return;
                        if (date.isAfter(LocalDate.now())) {
                            setDisable(true);
                            setStyle("-fx-background-color: #f5f5f5;");
                        }
                    }
                });
                datePicker.valueProperty().addListener((obs, oldV, newV) -> {
                    if (newV != null) handleLoadAttendance();
                });
            }

            // Column bindings (defensive)
            if (colId != null) colId.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getStudentId()));
            if (colName != null) colName.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getStudentName()));
            if (colRoll != null) colRoll.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getStudentRoll()));
            if (colCourse != null) colCourse.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getCourse()));
            if (colSemester != null) colSemester.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getSemester()));
            if (colGrade != null) colGrade.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getGradeSafe()));

            if (colToday != null) {
                colToday.setCellValueFactory(cell -> cell.getValue().presentProperty());
                colToday.setCellFactory(CheckBoxTableCell.forTableColumn(colToday));
            }

            // Table initial setup
            if (tableStudents != null) {
                tableStudents.setEditable(true);
                tableStudents.setItems(rows);

                // Hints for stretching and visibility
                try {
                    VBox.setVgrow(tableStudents, Priority.ALWAYS);
                } catch (Exception ignored) { /* if not in a VBox, ignore */ }
                tableStudents.setPrefHeight(Double.MAX_VALUE);
                tableStudents.setMaxHeight(Double.MAX_VALUE);
                tableStudents.setMinHeight(180);
                tableStudents.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

                // Prevent mouse presses while updating
                tableStudents.addEventFilter(MouseEvent.MOUSE_PRESSED, ev -> {
                    if (updating || tableStudents.isDisabled()) {
                        ev.consume();
                    }
                });

                try {
                    tableStudents.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
                    tableStudents.getSelectionModel().setCellSelectionEnabled(false);
                } catch (Exception ignored) {}
            }

            // Fill combo defaults
            if (comboSemester != null) comboSemester.getItems().addAll("1","2","3","4","5","6","7","8");
            if (comboCourse != null) comboCourse.getItems().addAll("CSE","Civil","Electrical","Mechanical");

            if (btnChangeSubject != null) {
                btnChangeSubject.setOnAction(e -> handleChangeSubject());
                btnChangeSubject.setDisable(false);
            }

            if (lblSelectedSubject != null) lblSelectedSubject.setText("Subject: (none)");

            updateButtonsStateForDate(LocalDate.now());

            // populate table on initialize so UI shows students immediately
            loadStudents();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /** Called by parent to set faculty context. */
    public void setFacultyInfo(String username, String department) {
        this.facultyUsername = username;
        this.facultyDepartment = department;
        try {
            if (username != null) this.facultyId = FacultyDAO.findIdByUsername(username);
        } catch (Throwable t) {
            try { this.facultyId = util.Session.getFacultyId(); } catch (Throwable ignored) {}
        }
    }

    // ---------------- UI handlers ----------------

    @FXML
    private void handleSearch() {
        loadStudents();
        handleLoadAttendance();
    }

    @FXML
    private void handleReset() {
        if (comboSemester != null) comboSemester.getSelectionModel().clearSelection();
        if (comboCourse != null) comboCourse.getSelectionModel().clearSelection();
        if (txtSearch != null) txtSearch.clear();
        if (datePicker != null) datePicker.setValue(LocalDate.now());
        selectedSubject = null;
        if (lblSelectedSubject != null) lblSelectedSubject.setText("Subject: (none)");

        safeSetRows(Collections.emptyList());
    }

    @FXML
    private void handleSelectAll() {
        if (!isSelectedDateEditable()) return;
        for (StudentRow r : rows) safeSetPresent(r, true);
        Platform.runLater(() -> { if (tableStudents != null) tableStudents.refresh(); });
    }

    @FXML
    private void handleDeselectAll() {
        if (!isSelectedDateEditable()) return;
        for (StudentRow r : rows) safeSetPresent(r, false);
        Platform.runLater(() -> { if (tableStudents != null) tableStudents.refresh(); });
    }

    @FXML
    private void handleSaveAttendance() {
        final LocalDate selected = (datePicker == null || datePicker.getValue() == null) ? LocalDate.now() : datePicker.getValue();

        if (!selected.equals(LocalDate.now())) {
            showAlert(Alert.AlertType.WARNING, "Save not allowed", "You can only save attendance for today.");
            return;
        }

        Subject subject = getOrAskForSubject(true);
        if (subject == null) return;

        if (rows.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "No students", "No students to save attendance for.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Save attendance for " + selected + " for subject " + safeSubjectName(subject) + "?",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Confirm Save");
        Optional<ButtonType> resp = confirm.showAndWait();
        if (resp.isEmpty() || resp.get() != ButtonType.YES) return;

        int success = 0, fail = 0;
        for (StudentRow r : rows) {
            try {
                String status = r.isPresent() ? "P" : "A";
                AttendanceDAO.upsert(r.getStudentId(), subject.getId(), selected, status, facultyId > 0 ? facultyId : null);
                success++;
            } catch (Throwable t) {
                t.printStackTrace();
                fail++;
            }
        }

        showAlert(Alert.AlertType.INFORMATION, "Save complete", "Saved: " + success + ", Failed: " + fail);
        handleLoadAttendance();
    }

    @FXML
    private void handleLoadAttendance() {
        final LocalDate date = (datePicker == null || datePicker.getValue() == null) ? LocalDate.now() : datePicker.getValue();

        // If rows are not loaded yet, request loadStudents() and always continue after FX thread updates
        if (rows.isEmpty()) {
            loadStudents();
            Platform.runLater(() -> continueLoadAttendance(date));
            return;
        }

        // rows already present -> proceed immediately
        continueLoadAttendance(date);
    }

    private void continueLoadAttendance(LocalDate date) {
        // Uncomment for debug:
        // System.out.println("DEBUG rows.size()=" + rows.size());
        // System.out.println("DEBUG tableItems=" + (tableStudents == null ? "null" : (tableStudents.getItems() == null ? "null" : tableStudents.getItems().size())));

        // Ensure the TableView actually has visible items (robust check)
        if (tableStudents == null || tableStudents.getItems() == null || tableStudents.getItems().isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "No students", "No students to display for selected filters.");
            return;
        }

        Subject subject = getOrAskForSubject(false);
        if (subject == null) return;

        Map<Integer,String> attendanceMap;
        try {
            attendanceMap = AttendanceDAO.getAttendanceMap(subject.getId(), date);
        } catch (Throwable t) {
            t.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Load failed", "Could not load attendance: " + t.getMessage());
            return;
        }

        final boolean readOnly = date.isBefore(LocalDate.now());

        // safely set present values
        for (StudentRow r : rows) {
            String s = attendanceMap.get(r.getStudentId());
            boolean presentValue = "P".equalsIgnoreCase(s) || "Present".equalsIgnoreCase(s);
            safeSetPresent(r, presentValue);
        }

        Platform.runLater(() -> {
            if (tableStudents != null) tableStudents.setEditable(!readOnly);
            updateButtonsStateForDate(date);
            if (tableStudents != null) {
                tableStudents.refresh();

                // nudge layout so rows actually paint (fixes cases where header shows but rows area looks empty)
                try {
                    tableStudents.setVisible(false);
                    tableStudents.applyCss();
                    tableStudents.layout();
                    tableStudents.setVisible(true);
                    tableStudents.requestLayout();
                    if (tableStudents.getParent() instanceof Region p) {
                        p.requestLayout();
                        p.applyCss();
                    }
                } catch (Exception ignored) {}
            }
        });
    }

    @FXML
    private void handleChangeSubject() {
        Subject s = presentSubjectChooser();
        if (s != null) {
            selectedSubject = s;
            if (lblSelectedSubject != null) lblSelectedSubject.setText("Subject: " + safeSubjectName(s));
            loadStudents();
            handleLoadAttendance();
        }
    }

    // ---------------- Helpers ----------------

    private Subject getOrAskForSubject(boolean forceAsk) {
        if (selectedSubject != null && !forceAsk) return selectedSubject;

        int fid = this.facultyId;
        if (fid <= 0) {
            try { fid = util.Session.getFacultyId(); } catch (Throwable ignored) {}
        }
        if (fid <= 0) {
            showAlert(Alert.AlertType.ERROR, "No Faculty ID", "Faculty ID not set. Ensure login passes faculty info.");
            return null;
        }

        List<Subject> subjects;
        try {
            subjects = FacultySubjectDAO.findSubjectsByFaculty(fid);
        } catch (Throwable t) {
            t.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Load Subjects Failed", "Could not load subjects.");
            return null;
        }

        List<Subject> filtered = filterSubjectsByFilters(subjects);

        if (filtered.size() == 1) {
            selectedSubject = filtered.get(0);
            if (lblSelectedSubject != null) lblSelectedSubject.setText("Subject: " + safeSubjectName(selectedSubject));
            return selectedSubject;
        }

        return presentSubjectChooser();
    }

    private Subject presentSubjectChooser() {
        int fid = this.facultyId;
        if (fid <= 0) {
            try { fid = util.Session.getFacultyId(); } catch (Throwable ignored) {}
        }
        if (fid <= 0) {
            showAlert(Alert.AlertType.ERROR, "No Faculty ID", "Faculty ID not set. Ensure login passes faculty info.");
            return null;
        }

        List<Subject> subjects;
        try {
            subjects = FacultySubjectDAO.findSubjectsByFaculty(fid);
        } catch (Throwable t) {
            t.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Load Subjects Failed", "Could not load subjects assigned to this faculty.");
            return null;
        }

        if (subjects == null || subjects.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "No Subjects", "No subjects are assigned to this faculty.");
            return null;
        }

        List<Subject> filtered = filterSubjectsByFilters(subjects);
        if (filtered.isEmpty()) filtered = subjects;

        if (filtered.size() == 1) {
            Subject s = filtered.get(0);
            if (lblSelectedSubject != null) lblSelectedSubject.setText("Subject: " + safeSubjectName(s));
            return s;
        }

        ChoiceDialog<Subject> dialog = new ChoiceDialog<>(filtered.get(0), filtered);
        dialog.setTitle("Choose Subject");
        dialog.setHeaderText("Select subject for which attendance will be saved/loaded");
        dialog.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        Optional<Subject> opt = dialog.showAndWait();
        if (opt.isPresent()) {
            Subject s = opt.get();
            if (lblSelectedSubject != null) lblSelectedSubject.setText("Subject: " + safeSubjectName(s));
            return s;
        }
        return null;
    }

    private List<Subject> filterSubjectsByFilters(List<Subject> subjects) {
        if (subjects == null) return Collections.emptyList();
        String sem = (comboSemester == null) ? null : comboSemester.getSelectionModel().getSelectedItem();
        String course = (comboCourse == null) ? null : comboCourse.getSelectionModel().getSelectedItem();

        final String semFinal = (sem == null || sem.isBlank()) ? null : sem;
        final String courseFinal = (course == null || course.isBlank()) ? null : course;

        return subjects.stream().filter(s -> {
            boolean ok = true;
            try {
                if (semFinal != null) ok = ok && semFinal.equalsIgnoreCase(getSubjectSemester(s));
            } catch (Throwable ignored) {}
            try {
                if (courseFinal != null) ok = ok && courseFinal.equalsIgnoreCase(getSubjectCourse(s));
            } catch (Throwable ignored) {}
            return ok;
        }).collect(Collectors.toList());
    }

    private boolean isSelectedDateEditable() {
        LocalDate d = (datePicker == null) ? LocalDate.now() : datePicker.getValue();
        if (d == null) d = LocalDate.now();
        return d.equals(LocalDate.now());
    }

    private void updateButtonsStateForDate(LocalDate date) {
        boolean editable = (date != null && date.equals(LocalDate.now()));
        if (btnSave != null) btnSave.setDisable(!editable);
        if (btnSelectAll != null) btnSelectAll.setDisable(!editable);
        if (btnDeselectAll != null) btnDeselectAll.setDisable(!editable);
        if (btnLoad != null) btnLoad.setDisable(false);
        if (btnChangeSubject != null) btnChangeSubject.setDisable(false);
    }

    private void loadStudents() {
        String sem = (comboSemester == null) ? null : comboSemester.getSelectionModel().getSelectedItem();
        String course = (comboCourse == null) ? null : comboCourse.getSelectionModel().getSelectedItem();
        String search = (txtSearch == null) ? "" : txtSearch.getText().trim();

        Integer semInt = null;
        try { if (sem != null && !sem.isBlank()) semInt = Integer.parseInt(sem); } catch (NumberFormatException ignored) {}

        List<?> studentModels;
        try {
            studentModels = StudentDAO.findByFilters(semInt, course, search);
        } catch (Throwable t) {
            t.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Load Error", "Could not load students.");
            return;
        }

        // Build new list off-FX and apply with safeSetRows
        List<StudentRow> newRows = new ArrayList<>();
        for (Object obj : studentModels) {
            try {
                int id = (int) obj.getClass().getMethod("getId").invoke(obj);
                String name = String.valueOf(obj.getClass().getMethod("getName").invoke(obj));
                String roll = String.valueOf(obj.getClass().getMethod("getRoll").invoke(obj));
                String c = String.valueOf(obj.getClass().getMethod("getCourse").invoke(obj));
                Object semVal = obj.getClass().getMethod("getSemester").invoke(obj);
                String semStr = semVal == null ? "" : String.valueOf(semVal);

                newRows.add(new StudentRow(id, name, roll, c, semStr, null, false));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        safeSetRows(newRows);
    }

    private void showAlert(Alert.AlertType type, String title, String text) {
        Platform.runLater(() -> {
            Alert a = new Alert(type, text, ButtonType.OK);
            a.setHeaderText(title);
            a.showAndWait();
        });
    }

    // ---------------- Reflection helpers for Subject ----------------

    private String getSubjectCourse(Subject s) {
        if (s == null) return "";
        String[] candidates = {"getCourse", "getDepartment", "getDept", "getProgram", "getBranch", "getCourseName"};
        for (String mName : candidates) {
            try {
                Method m = s.getClass().getMethod(mName);
                Object val = m.invoke(s);
                if (val != null) return String.valueOf(val);
            } catch (Exception ignored) {}
        }
        return "";
    }

    private String getSubjectSemester(Subject s) {
        if (s == null) return "";
        String[] candidates = {"getSemester", "getSem", "getSemesterNo", "getTerm"};
        for (String mName : candidates) {
            try {
                Method m = s.getClass().getMethod(mName);
                Object val = m.invoke(s);
                if (val != null) return String.valueOf(val);
            } catch (Exception ignored) {}
        }
        return "";
    }

    private String safeSubjectName(Subject s) {
        if (s == null) return "(none)";
        try {
            Method m = s.getClass().getMethod("getName");
            Object val = m.invoke(s);
            if (val != null) return String.valueOf(val);
        } catch (Exception ignored) {}
        try { return s.toString(); } catch (Throwable ignored) {}
        return "(subject)";
    }

    // ---------------- SAFE UI UPDATE HELPERS ----------------

    /**
     * Replace the items in the table safely:
     * - sets updating=true to block mouse presses
     * - clears selection first
     * - keeps same ObservableList instance (rows.setAll)
     * - performs modifications on the FX thread and then clears updating
     */
    private void safeSetRows(List<StudentRow> newRows) {
        // immediate guard so event filter sees updating==true if user clicks before runLater executes
        updating = true;

        if (tableStudents == null) {
            // ensure rows are set even if table isn't injected yet (important during early initialize)
            rows.setAll(newRows);
            updating = false;
            return;
        }

        Platform.runLater(() -> {
            boolean wasDisabled = tableStudents.isDisabled();
            try {
                tableStudents.setDisable(true);
                try { tableStudents.getSelectionModel().clearSelection(); } catch (Exception ignored) {}
                rows.setAll(newRows);
                tableStudents.refresh();
            } finally {
                tableStudents.setDisable(wasDisabled);
                // allow interaction again
                updating = false;
            }

            // After rows applied, nudge layout so UI shows the rows
            Platform.runLater(() -> {
                try {
                    // ensure table size constraints allow it to grow
                    tableStudents.setMinHeight(Math.min(180, tableStudents.getMinHeight() <= 0 ? 180 : tableStudents.getMinHeight()));
                    tableStudents.setMaxHeight(Double.MAX_VALUE);
                    tableStudents.setPrefHeight(Double.MAX_VALUE);

                    // tiny invisible toggle + layout calls to force repaint in tricky layouts
                    tableStudents.setVisible(false);
                    tableStudents.applyCss();
                    tableStudents.layout();
                    tableStudents.setVisible(true);
                    tableStudents.requestLayout();

                    if (tableStudents.getParent() instanceof Region pr) {
                        pr.requestLayout();
                        pr.applyCss();
                    }
                } catch (Exception ignored) {}
            });
        });
    }

    private void safeSetPresent(StudentRow r, boolean presentValue) {
        if (r == null) return;
        if (Platform.isFxApplicationThread()) {
            r.setPresent(presentValue);
        } else {
            Platform.runLater(() -> r.setPresent(presentValue));
        }
    }

    // ---------------- Row model ----------------

    public static class StudentRow {
        private final Integer studentId;
        private final String studentName;
        private final String studentRoll;
        private final String course;
        private final String semester;
        private final String grade; // optional
        private final BooleanProperty present = new SimpleBooleanProperty(false);

        public StudentRow(int studentId, String studentName, String studentRoll, String course, String semester, String grade, boolean present) {
            this.studentId = studentId;
            this.studentName = studentName;
            this.studentRoll = studentRoll;
            this.course = course;
            this.semester = semester;
            this.grade = grade;
            this.present.set(present);
        }

        public Integer getStudentId() { return studentId; }
        public String getStudentName() { return studentName; }
        public String getStudentRoll() { return studentRoll; }
        public String getCourse() { return course; }
        public String getSemester() { return semester; }
        public String getGradeSafe() { return grade == null ? "" : grade; }

        public boolean isPresent() { return present.get(); }
        public BooleanProperty presentProperty() { return present; }
        public void setPresent(boolean p) { present.set(p); }
    }

    // Expose table getter for external checks (dashboard or other controllers)
    public TableView<StudentRow> getTableStudents() {
        return tableStudents;
    }
}

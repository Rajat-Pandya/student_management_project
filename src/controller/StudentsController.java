package controller;

import dao.StudentDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import model.Student;
import java.util.List;
import java.util.stream.Collectors;

public class StudentsController {

    @FXML private TableView<Student> tableStudents;
    @FXML private TableColumn<Student, Integer> colId;
    @FXML private TableColumn<Student, String> colName;
    @FXML private TableColumn<Student, String> colRoll;
    @FXML private TableColumn<Student, String> colEmail;
    @FXML private TableColumn<Student, String> colPhone;
    @FXML private TableColumn<Student, String> colCourse;
    @FXML private TableColumn<Student, String> colSemester;

    @FXML private TextField txtSearch;
    @FXML private TextField txtName;
    @FXML private TextField txtRoll;
    @FXML private TextField txtEmail;
    @FXML private TextField txtPhone;
    @FXML private ComboBox<String> cmbCourse;
    @FXML private ComboBox<String> cmbSemester;

    @FXML private Button btnAdd;
    @FXML private Button btnUpdate;
    @FXML private Button btnDelete;
    @FXML private Button btnClear;
    @FXML private Button btnRefresh;

    @FXML private Label lblTotalStudents;
    @FXML private Label lblTotalCourses;

    private final ObservableList<Student> studentList = FXCollections.observableArrayList();
    private final StudentDAO studentDAO = new StudentDAO();

    @FXML
    private void initialize() {
        setupTableColumns();
        setupComboBoxes();
        setupSearch();
        loadStudents();
    }

    @SuppressWarnings({ "deprecation", "unused" })
	private void setupTableColumns() {
        if (colId != null) {
            colId.setCellValueFactory(new PropertyValueFactory<>("id"));
            colId.setSortType(TableColumn.SortType.DESCENDING);
        }
        if (colName != null) {
            colName.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        }
        if (colRoll != null) {
            colRoll.setCellValueFactory(cellData -> cellData.getValue().rollProperty());
        }
        if (colEmail != null) {
            colEmail.setCellValueFactory(cellData -> cellData.getValue().emailProperty());
        }
        if (colPhone != null) {
            colPhone.setCellValueFactory(cellData -> cellData.getValue().phoneProperty());
        }
        if (colCourse != null) {
            colCourse.setCellValueFactory(cellData -> cellData.getValue().courseProperty());
        }
        if (colSemester != null) {
            colSemester.setCellValueFactory(cellData -> cellData.getValue().semesterProperty());
        }

        if (tableStudents != null) {
            tableStudents.setItems(studentList);
            tableStudents.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            tableStudents.setEditable(false);
            tableStudents.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
            tableStudents.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) populateForm(newVal);
            });
        }
    }

    @SuppressWarnings("unused")
	private void setupComboBoxes() {
        ObservableList<String> courses = FXCollections.observableArrayList(
            "CSE", "ECE", "Civil", "Mechanical", "IT", "EEE"
        );
        ObservableList<String> semesters = FXCollections.observableArrayList(
            "1", "2", "3", "4", "5", "6", "7", "8"
        );

        if (cmbCourse != null) {
            cmbCourse.setItems(courses);
            cmbCourse.setValue("CSE");
        }
        if (cmbSemester != null) {
            cmbSemester.setItems(semesters);
            cmbSemester.setValue("1");
            cmbSemester.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) loadStudents();
            });
        }
    }

    @SuppressWarnings("unused")
	private void setupSearch() {
        if (txtSearch != null) {
            txtSearch.textProperty().addListener((obs, oldVal, newVal) -> {
                filterStudents(newVal);
            });
        }
    }

    @FXML
    private void loadStudents() {
        studentList.clear();
        String semester = cmbSemester != null ? cmbSemester.getValue() : "1";
        if (semester == null || semester.isBlank()) return;

        try {
            List<Student> students = studentDAO.findBySemester(semester);
            if (students != null) {
                studentList.addAll(students);
            }
            updateDashboard();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load students: " + e.getMessage());
        }
    }

    private void updateDashboard() {
        int totalStudents = studentDAO.countAll();
        if (lblTotalStudents != null) {
            lblTotalStudents.setText(String.valueOf(totalStudents));
        }
    }

    private void filterStudents(String query) {
        if (query == null || query.isBlank()) {
            loadStudents();
            return;
        }
        String lowerQuery = query.toLowerCase();
        List<Student> filtered = studentList.stream()
            .filter(s -> String.valueOf(s.getId()).contains(query)
                     || s.getName().toLowerCase().contains(lowerQuery)
                     || s.getRoll().toLowerCase().contains(lowerQuery)
                     || s.getEmail().toLowerCase().contains(lowerQuery))
            .collect(Collectors.toList());
        studentList.setAll(filtered);
    }

    private void populateForm(Student student) {
        if (txtName != null) txtName.setText(student.getName());
        if (txtRoll != null) txtRoll.setText(student.getRoll());
        if (txtEmail != null) txtEmail.setText(student.getEmail());
        if (txtPhone != null) txtPhone.setText(student.getPhone());
        if (cmbCourse != null) cmbCourse.setValue(student.getCourse());
        if (cmbSemester != null) cmbSemester.setValue(student.getSemester());
    }

    @FXML
    private void handleAdd(ActionEvent event) {
        if (!validateInput()) return;
        if (isDuplicateRoll(null)) return;

        try {
            Student student = createStudentFromForm();
            studentDAO.insert(student);
            loadStudents();
            clearFields();
            showSuccess("Student added successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to add student: " + e.getMessage());
        }
    }

    @FXML
    private void handleUpdate(ActionEvent event) {
        Student selected = tableStudents != null ? tableStudents.getSelectionModel().getSelectedItem() : null;
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a student to update.");
            return;
        }
        if (!validateInput()) return;
        if (isDuplicateRoll(selected.getId())) return;

        try {
            Student student = createStudentFromForm();
            student.setId(selected.getId());
            studentDAO.update(student);
            loadStudents();
            clearFields();
            showSuccess("Student updated successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to update student: " + e.getMessage());
        }
    }

    private boolean isDuplicateRoll(Integer excludeId) {
        String roll = txtRoll != null ? txtRoll.getText() : null;
        if (roll == null || roll.isBlank()) return false;

        try {
            Student existing = studentDAO.findByRoll(roll);
            if (existing != null) {
                if (excludeId == null || excludeId != existing.getId()) {
                    showAlert(Alert.AlertType.WARNING, "Duplicate",
                        "Roll number '" + roll + "' already exists for student: " + existing.getName());
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @FXML
    private void handleDelete(ActionEvent event) {
        Student selected = tableStudents != null ? tableStudents.getSelectionModel().getSelectedItem() : null;
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a student to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete Student");
        confirm.setContentText("Are you sure you want to delete " + selected.getName() + " (" + selected.getRoll() + ")?\nThis action cannot be undone.");
        confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
        
        if (confirm.getOwner() != null) {
            confirm.initOwner(confirm.getOwner());
        }
        
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    studentDAO.delete(selected.getId());
                    loadStudents();
                    clearFields();
                    showSuccess("Student deleted successfully.");
                } catch (Exception e) {
                    e.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete student: " + e.getMessage());
                }
            }
        });
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        if (tableStudents != null && tableStudents.getScene() != null) {
            alert.initOwner(tableStudents.getScene().getWindow());
        }
        alert.showAndWait();
    }

    @FXML
    private void handleClear(ActionEvent event) {
        clearFields();
        showSuccess("Form cleared.");
    }

    @FXML
    private void handleRefresh(ActionEvent event) {
        loadStudents();
        showSuccess("Data refreshed.");
    }

    @FXML
    private void handleSearch(ActionEvent event) {
        filterStudents(txtSearch != null ? txtSearch.getText() : "");
        if (txtSearch != null && !txtSearch.getText().isBlank()) {
            showSuccess("Found " + studentList.size() + " result(s).");
        }
    }

    private boolean validateInput() {
        String name = txtName != null ? txtName.getText() : null;
        String roll = txtRoll != null ? txtRoll.getText() : null;
        String email = txtEmail != null ? txtEmail.getText() : null;
        String course = cmbCourse != null ? cmbCourse.getValue() : null;

        if (name == null || name.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Name is required.");
            return false;
        }
        if (roll == null || roll.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Roll number is required.");
            return false;
        }
        if (email != null && !email.isBlank() && !isValidEmail(email)) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Invalid email format.");
            return false;
        }
        if (course == null || course.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Course is required.");
            return false;
        }
        return true;
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) return true;
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }

    private Student createStudentFromForm() {
        return new Student(
            0,
            txtRoll != null ? txtRoll.getText() : "",
            txtName != null ? txtName.getText() : "",
            cmbCourse != null ? cmbCourse.getValue() : "CSE",
            txtEmail != null ? txtEmail.getText() : "",
            txtPhone != null ? txtPhone.getText() : "",
            cmbCourse != null ? cmbCourse.getValue() : "",
            cmbSemester != null ? cmbSemester.getValue() : "1",
            0
        );
    }

    private void clearFields() {
        if (txtName != null) txtName.clear();
        if (txtRoll != null) txtRoll.clear();
        if (txtEmail != null) txtEmail.clear();
        if (txtPhone != null) txtPhone.clear();
        if (cmbCourse != null) cmbCourse.setValue("CSE");
        if (cmbSemester != null) cmbSemester.setValue("1");
        if (tableStudents != null) tableStudents.getSelectionModel().clearSelection();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        if (tableStudents != null && tableStudents.getScene() != null) {
            alert.initOwner(tableStudents.getScene().getWindow());
        }
        alert.showAndWait();
    }
}
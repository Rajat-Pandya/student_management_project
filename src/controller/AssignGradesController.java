package controller;

import dao.FacultySubjectDAO;
import dao.MarksDAO;
import model.Subject;
import util.Session;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;

import java.util.List;

/**
 * AssignGradesController - allows grading students for a selected subject
 * assigned to the logged faculty. - Adds ID column wiring - Uses a FilteredList
 * for grade filtering - Defensive null checks and safe UI updates
 */
public class AssignGradesController {

	@FXML
	private ComboBox<Subject> cmbSubjects;
	@FXML
	private TableView<MarksDAO.StudentMark> tblStudents;

	@FXML
	private TableColumn<MarksDAO.StudentMark, Integer> colSId; // ID column
	@FXML
	private TableColumn<MarksDAO.StudentMark, String> colSName;
	@FXML
	private TableColumn<MarksDAO.StudentMark, String> colSRoll;
	@FXML
	private TableColumn<MarksDAO.StudentMark, String> colGrade;

	@FXML
	private Button btnSaveGrades;
	@FXML
	private Button btnAutoGrade;
	@FXML
	private Button btnRefresh;
	@FXML
	private Button btnApplyGradeFilter;
	@FXML
	private Button btnClearGradeFilter;

	// Grade filter ComboBox (in FXML)
	@FXML
	private ComboBox<String> cmbGradeFilter;

	private final FacultySubjectDAO facultySubjectDAO = new FacultySubjectDAO();
	private final MarksDAO marksDAO = new MarksDAO();

	private final ObservableList<MarksDAO.StudentMark> students = FXCollections.observableArrayList();
	@SuppressWarnings("unused")
	private final FilteredList<MarksDAO.StudentMark> filteredStudents = new FilteredList<>(students, sm -> true);

	private final ObservableList<String> gradeOptions = FXCollections.observableArrayList("A+", "A", "B+", "B", "C",
			"D", "F");

	@SuppressWarnings("unused")
	@FXML
	private void initialize() {
		// --- Combo box visuals (subjects) ---
		if (cmbSubjects != null) {
			cmbSubjects.setCellFactory(cb -> new ListCell<>() {
				@Override
				protected void updateItem(Subject item, boolean empty) {
					super.updateItem(item, empty);
					setText(empty || item == null ? null : (item.getCode() + " - " + item.getName()));
				}
			});
			cmbSubjects.setButtonCell(new ListCell<>() {
				@Override
				protected void updateItem(Subject item, boolean empty) {
					super.updateItem(item, empty);
					setText(empty || item == null ? null : (item.getCode() + " - " + item.getName()));
				}
			});
			cmbSubjects.valueProperty().addListener((obs, oldv, newv) -> {
				if (newv == null) {
					students.clear();
				} else {
					loadStudentsForSubject(newv);
				}
			});
		}

		// --- Table column wiring ---
		if (colSId != null) {
			colSId.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().studentId).asObject());
		}
		if (colSName != null) {
			colSName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().studentName));
		}
		if (colSRoll != null) {
			colSRoll.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().roll));
		}
		if (colGrade != null) {
			colGrade.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().grade));
			colGrade.setCellFactory(ComboBoxTableCell.forTableColumn(gradeOptions));
			colGrade.setOnEditCommit(evt -> {
				MarksDAO.StudentMark sm = evt.getRowValue();
				if (sm != null) {
					sm.grade = evt.getNewValue();
				}
			});
		}

		// --- Table setup ---
		if (tblStudents != null) {
			tblStudents.setEditable(true);
			tblStudents.setItems(filteredStudents);
		}

		// --- Grade filter combobox setup ---
		if (cmbGradeFilter != null) {
			cmbGradeFilter.setItems(FXCollections.observableArrayList(gradeOptions));
			cmbGradeFilter.setPromptText("All grades");
		}

		// --- Buttons wiring ---
		if (btnAutoGrade != null)
			btnAutoGrade.setOnAction(e -> onAutoGrade());
		if (btnSaveGrades != null)
			btnSaveGrades.setOnAction(e -> onSaveGrades());
		if (btnRefresh != null)
			btnRefresh.setOnAction(e -> onRefresh());

		// Load subjects for the logged-in faculty (do in background to avoid blocking
		// UI)
		Platform.runLater(this::loadSubjectsForFaculty);
	}

	/**
	 * Load subjects assigned to current faculty and select first if any.
	 */
	private void loadSubjectsForFaculty() {
		students.clear();
		int facultyId = Session.getCurrentFacultyId();
		if (facultyId <= 0) {
			showAlert(Alert.AlertType.ERROR, "Not logged in", "Faculty not identified.");
			return;
		}

		List<Subject> list;
		try {
			@SuppressWarnings("static-access")
			List<Subject> tmp = facultySubjectDAO.findSubjectsByFaculty(facultyId);
			list = tmp;
		} catch (Exception e) {
			e.printStackTrace();
			showAlert(Alert.AlertType.ERROR, "Load Error", "Could not load assigned subjects.");
			return;
		}

		Platform.runLater(() -> {
			if (cmbSubjects != null) {
				if (list == null || list.isEmpty()) {
					cmbSubjects.setItems(FXCollections.observableArrayList());
				} else {
					cmbSubjects.setItems(FXCollections.observableArrayList(list));
					// select first to trigger student load automatically
					if (cmbSubjects.getValue() == null)
						cmbSubjects.getSelectionModel().selectFirst();
				}
			}
		});
	}

	/**
	 * Load students+grades for the provided subject.
	 */
	private void loadStudentsForSubject(Subject subject) {
		if (subject == null) {
			students.clear();
			return;
		}

		List<MarksDAO.StudentMark> list;
		try {
			list = marksDAO.findMarksForSubjectAndSemester(subject.getId(), subject.getSemester());
		} catch (Exception e) {
			e.printStackTrace();
			showAlert(Alert.AlertType.ERROR, "DB Error", "Could not load students for subject.");
			return;
		}

		Platform.runLater(() -> {
			students.clear();
			if (list != null && !list.isEmpty())
				students.addAll(list);
			if (tblStudents != null)
				tblStudents.refresh();
		});
	}

	@FXML
	private void onAutoGrade() {
		for (MarksDAO.StudentMark sm : students) {
			if (sm.marks == null)
				continue;
			sm.grade = gradeFromMarks(sm.marks);
		}
		if (tblStudents != null)
			tblStudents.refresh();
	}

	@FXML
	private void onRefresh() {
		loadSubjectsForFaculty();
		Subject s = (cmbSubjects == null) ? null : cmbSubjects.getValue();
		if (s != null)
			loadStudentsForSubject(s);
	}

	@FXML
	private void onSaveGrades() {
		Subject s = (cmbSubjects == null) ? null : cmbSubjects.getValue();
		if (s == null) {
			showAlert(Alert.AlertType.WARNING, "Select Subject", "Please select a subject.");
			return;
		}
		int facultyId = Session.getCurrentFacultyId();
		if (facultyId <= 0) {
			showAlert(Alert.AlertType.ERROR, "Not logged in", "Faculty not identified.");
			return;
		}

		int subjectId = s.getId();
		int sem = s.getSemester();
		boolean okAll = true;
		for (MarksDAO.StudentMark sm : students) {
			boolean ok = marksDAO.upsertMark(sm.studentId, subjectId, sem, facultyId, sm.marks, sm.grade);
			if (!ok)
				okAll = false;
		}

		if (okAll)
			showAlert(Alert.AlertType.INFORMATION, "Saved", "Grades saved successfully.");
		else
			showAlert(Alert.AlertType.WARNING, "Partial", "Some grades failed to save.");

		// reload to reflect canonical data
		loadStudentsForSubject(s);
	}

	// ----- Grade filtering -----
	@SuppressWarnings("unused")
	@FXML
	private void onApplyGradeFilter() {
		if (cmbGradeFilter == null)
			return;
		String selectedGrade = cmbGradeFilter.getValue();
		if (selectedGrade == null || selectedGrade.isBlank()) {
			filteredStudents.setPredicate(sm -> true);
			return;
		}
		filteredStudents
				.setPredicate(sm -> sm != null && sm.grade != null && sm.grade.equalsIgnoreCase(selectedGrade.trim()));
	}

	@SuppressWarnings("unused")
	@FXML
	private void onClearGradeFilter() {
		if (cmbGradeFilter != null)
			cmbGradeFilter.getSelectionModel().clearSelection();
		filteredStudents.setPredicate(sm -> true);
	}

	// ----- Utility -----
	private String gradeFromMarks(int marks) {
		if (marks >= 90)
			return "A+";
		if (marks >= 80)
			return "A";
		if (marks >= 70)
			return "B+";
		if (marks >= 60)
			return "B";
		if (marks >= 50)
			return "C";
		if (marks >= 40)
			return "D";
		return "F";
	}

	private void showAlert(Alert.AlertType type, String header, String msg) {
		// always run on FX thread
		Platform.runLater(() -> {
			Alert a = new Alert(type, msg, ButtonType.OK);
			a.setHeaderText(header);
			a.showAndWait();
		});
	}
}

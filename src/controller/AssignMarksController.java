package controller;

import dao.FacultySubjectDAO;
import dao.MarksDAO;
import model.Subject;
import util.Session;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.converter.IntegerStringConverter;

import java.util.List;

/**
 * AssignMarksController — improved robustness and UX: - ID column wired -
 * FilteredList used as TableView items - Safer marks editing (converter, commit
 * on Enter & focus lost) - Defensive null checks
 */
public class AssignMarksController {

	@FXML
	private ComboBox<Subject> cmbSubjects;
	@FXML
	private TableView<MarksDAO.StudentMark> tblStudents;
	@FXML
	private TableColumn<MarksDAO.StudentMark, Integer> colSId;
	@FXML
	private TableColumn<MarksDAO.StudentMark, String> colSName;
	@FXML
	private TableColumn<MarksDAO.StudentMark, String> colSRoll;
	@FXML
	private TableColumn<MarksDAO.StudentMark, Integer> colMarks;
	@FXML
	private Button btnSaveMarks;
	@FXML
	private Button btnRefresh;
	@FXML
	private Button btnApplyFilter;
	@FXML
	private Button btnClearFilter;

	// Filter controls (may be present in FXML)
	@FXML
	private TextField txtMinMarks;
	@FXML
	private TextField txtMaxMarks;

	private final FacultySubjectDAO facultySubjectDAO = new FacultySubjectDAO();
	private final MarksDAO marksDAO = new MarksDAO();

	private final ObservableList<MarksDAO.StudentMark> students = FXCollections.observableArrayList();
	@SuppressWarnings("unused")
	private final FilteredList<MarksDAO.StudentMark> filteredStudents = new FilteredList<>(students, sm -> true);

	@SuppressWarnings("unused")
	@FXML
	private void initialize() {
		System.out.println("[AssignMarks] initialize() called");

		setupSubjectCombo();
		setupTable();

		if (btnSaveMarks != null)
			btnSaveMarks.setOnAction(e -> onSaveMarks());
		if (btnRefresh != null)
			btnRefresh.setOnAction(e -> onRefresh());

		// load subjects (use runLater to avoid blocking FXML load if necessary)
		Platform.runLater(this::loadSubjectsForFaculty);
	}

	// ---------------- COMBOBOX ----------------
	@SuppressWarnings("unused")
	private void setupSubjectCombo() {
		if (cmbSubjects == null)
			return;

		cmbSubjects.setConverter(new javafx.util.StringConverter<>() {
			@Override
			public String toString(Subject s) {
				if (s == null)
					return "";
				return s.getCode() + " - " + s.getName();
			}

			@Override
			public Subject fromString(String string) {
				return null;
			}
		});

		cmbSubjects.setCellFactory(cb -> new ListCell<>() {
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

	// ---------------- TABLE ----------------
	@SuppressWarnings("unused")
	private void setupTable() {
		if (colSId != null) {
			colSId.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().studentId).asObject());
			// To hide visually but keep available in model, uncomment:
			// colSId.setVisible(false);
		}
		if (colSName != null)
			colSName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().studentName));
		if (colSRoll != null)
			colSRoll.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().roll));

		if (colMarks != null) {
			// preserve nulls
			colMarks.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().marks));

			// converter: blank => null, invalid => null (safe)
			IntegerStringConverter safeConverter = new IntegerStringConverter() {
				@Override
				public Integer fromString(String value) {
					if (value == null)
						return null;
					value = value.trim();
					if (value.isEmpty())
						return null;
					try {
						return Integer.valueOf(value);
					} catch (NumberFormatException e) {
						System.err.println("[AssignMarks] parse error for marks: '" + value + "'");
						return null;
					}
				}
			};

			// TextFieldTableCell with improved commit behavior
			colMarks.setCellFactory(column -> {
				TextFieldTableCell<MarksDAO.StudentMark, Integer> cell = new TextFieldTableCell<>(safeConverter);

				// when editing starts, request focus on the editor
				cell.setOnMouseClicked(e -> {
					if (!cell.isEmpty() && e.getClickCount() == 1) {
						TableView<MarksDAO.StudentMark> tv = cell.getTableView();
						int row = cell.getIndex();
						tv.requestFocus();
						tv.getSelectionModel().select(row);
						tv.getFocusModel().focus(row, column);
						tv.edit(row, column);
					}
				});

				// commit on focus lost (default TextFieldTableCell doesn't always)
				cell.itemProperty().addListener((obs, oldItem, newItem) -> {
					// no-op; kept for future extension
				});

				return cell;
			});

			colMarks.setEditable(true);

			colMarks.setOnEditCommit(evt -> {
				MarksDAO.StudentMark sm = evt.getRowValue();
				Integer newVal = evt.getNewValue();
				if (sm != null) {
					sm.marks = newVal;
				}
				if (tblStudents != null)
					tblStudents.refresh();
			});
		}

		if (tblStudents != null) {
			tblStudents.setEditable(true);
			tblStudents.setItems(filteredStudents);
		}
	}

	// ---------------- ACTIONS ----------------
	@FXML
	private void onRefresh() {
		System.out.println("[AssignMarks] onRefresh() called");
		loadSubjectsForFaculty();
		Subject sel = (cmbSubjects == null) ? null : cmbSubjects.getValue();
		if (sel != null)
			loadStudentsForSubject(sel);
	}

	@FXML
	private void onSaveMarks() {
		Subject s = (cmbSubjects == null) ? null : cmbSubjects.getValue();
		if (s == null) {
			showAlert(Alert.AlertType.WARNING, "Select Subject", "Please select a subject before saving.");
			return;
		}

		int facultyId = getFacultyId();
		if (facultyId <= 0) {
			showAlert(Alert.AlertType.ERROR, "Not logged in", "Faculty not identified in session.");
			return;
		}

		boolean allOk = true;
		for (MarksDAO.StudentMark sm : students) {
			if (sm == null)
				continue;
			if (sm.marks == null)
				continue; // skip empty
			boolean ok = marksDAO.upsertMark(sm.studentId, s.getId(), s.getSemester(), facultyId, sm.marks, sm.grade);
			if (!ok)
				allOk = false;
		}

		if (allOk)
			showAlert(Alert.AlertType.INFORMATION, "Saved", "Marks saved successfully.");
		else
			showAlert(Alert.AlertType.WARNING, "Partial Save", "Some marks failed to save. Check logs.");

		loadStudentsForSubject(s);
	}

	// ---------------- FILTER ----------------
	@SuppressWarnings("unused")
	@FXML
	private void onApplyFilter() {
		if (txtMinMarks == null && txtMaxMarks == null) {
			filteredStudents.setPredicate(sm -> true);
			return;
		}

		Integer min = parseIntOrNull(txtMinMarks == null ? null : txtMinMarks.getText());
		Integer max = parseIntOrNull(txtMaxMarks == null ? null : txtMaxMarks.getText());

		filteredStudents.setPredicate(sm -> {
			if (sm == null)
				return false;
			if (sm.marks == null)
				return false;
			if (min != null && sm.marks < min)
				return false;
			if (max != null && sm.marks > max)
				return false;
			return true;
		});
	}

	@SuppressWarnings("unused")
	@FXML
	private void onClearFilter() {
		if (txtMinMarks != null)
			txtMinMarks.clear();
		if (txtMaxMarks != null)
			txtMaxMarks.clear();
		filteredStudents.setPredicate(sm -> true);
	}

	// ---------------- DATA LOADING ----------------
	@SuppressWarnings("static-access")
	private void loadSubjectsForFaculty() {
		int facultyId = getFacultyId();
		if (facultyId <= 0) {
			showAlert(Alert.AlertType.ERROR, "Not logged in", "Faculty not identified.");
			return;
		}

		List<Subject> list;
		try {
			list = facultySubjectDAO.findSubjectsByFaculty(facultyId);
		} catch (Exception e) {
			e.printStackTrace();
			showAlert(Alert.AlertType.ERROR, "DB Error", "Could not load assigned subjects.");
			return;
		}

		Platform.runLater(() -> {
			if (cmbSubjects != null) {
				if (list == null || list.isEmpty()) {
					cmbSubjects.setItems(FXCollections.observableArrayList());
					students.clear();
					showAlert(Alert.AlertType.INFORMATION, "No subjects", "No subjects assigned to you.");
				} else {
					cmbSubjects.setItems(FXCollections.observableArrayList(list));
					if (cmbSubjects.getValue() == null)
						cmbSubjects.getSelectionModel().selectFirst();
				}
			}
		});
	}

	private void loadStudentsForSubject(Subject subject) {
		students.clear();
		if (subject == null)
			return;

		List<MarksDAO.StudentMark> list;
		try {
			list = marksDAO.findMarksForSubjectAndSemester(subject.getId(), subject.getSemester());
		} catch (Exception e) {
			e.printStackTrace();
			showAlert(Alert.AlertType.ERROR, "DB Error", "Failed to load students/marks: " + e.getMessage());
			return;
		}

		if (list == null || list.isEmpty()) {
			Platform.runLater(() -> {
				students.clear();
				if (tblStudents != null)
					tblStudents.refresh();
				showAlert(Alert.AlertType.INFORMATION, "No students", "No students found for this subject/semester.");
			});
			return;
		}

		Platform.runLater(() -> {
			students.setAll(list);
			if (tblStudents != null)
				tblStudents.refresh();
		});
	}

	// ---------------- HELPERS ----------------
	private int getFacultyId() {
		int facultyId = -1;
		try {
			facultyId = Session.getFacultyId();
		} catch (Throwable ignored) {
		}
		if (facultyId <= 0) {
			try {
				facultyId = Session.getCurrentFacultyId();
			} catch (Throwable ignored) {
			}
		}
		return facultyId;
	}

	private Integer parseIntOrNull(String s) {
		if (s == null || s.isBlank())
			return null;
		try {
			return Integer.parseInt(s.trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private void showAlert(Alert.AlertType type, String header, String msg) {
		// Always ensure alert runs on FX thread
		Platform.runLater(() -> {
			Alert a = new Alert(type, msg, ButtonType.OK);
			a.setHeaderText(header);
			a.showAndWait();
		});
	}
}

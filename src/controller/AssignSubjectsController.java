package controller;

import dao.FacultyDAO;
import dao.FacultySubjectDAO;
import dao.SubjectDAO;
import model.FacultyModel;
import model.FacultySubjectAssignment;
import model.Subject;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Region;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * Controller for AssignSubjects.fxml
 *
 * Notes: - Defensive against missing fx:id injections - Safe table wiring
 * (colId present and optional hide) - Keeps your original semantics (instance
 * DAOs, same DAO method names)
 */
public class AssignSubjectsController {

	@FXML
	private ComboBox<FacultyModel> cmbFaculty;
	@FXML
	private ComboBox<String> cmbSemester;
	@FXML
	private ComboBox<Subject> cmbSubject;

	@FXML
	private TableView<FacultySubjectAssignment> tblAssignments;
	@FXML
	private TableColumn<FacultySubjectAssignment, Integer> colId;
	@FXML
	private TableColumn<FacultySubjectAssignment, String> colFaculty;
	@FXML
	private TableColumn<FacultySubjectAssignment, String> colSubject;
	@FXML
	private TableColumn<FacultySubjectAssignment, Integer> colSem;
	@FXML
	private TableColumn<FacultySubjectAssignment, LocalDate> colAssignedAt;
	@FXML
	private TableColumn<FacultySubjectAssignment, FacultySubjectAssignment> colAction;

	@FXML
	private Button btnAssign;
	@FXML
	private Button btnRefreshList;

	private final ObservableList<FacultyModel> facultyList = FXCollections
			.observableArrayList();
	private final ObservableList<Subject> subjectList = FXCollections.observableArrayList();
	private final ObservableList<FacultySubjectAssignment> assignments = FXCollections.observableArrayList();

	// Instance DAOs
	private final FacultyDAO facultyDAO = new FacultyDAO();
	private final SubjectDAO subjectDAO = new SubjectDAO();
	private final FacultySubjectDAO facultySubjectDAO = new FacultySubjectDAO();

	@SuppressWarnings("unused")
	@FXML
	private void initialize() {
		// populate semesters safely
		if (cmbSemester != null) {
			cmbSemester.getItems().clear();
			IntStream.rangeClosed(1, 8).mapToObj(String::valueOf).forEach(s -> cmbSemester.getItems().add(s));
		}

		// configure table columns defensively
		if (colId != null) {
			colId.setCellValueFactory(
					cell -> new ReadOnlyObjectWrapper<>(cell.getValue() == null ? null : cell.getValue().getId()));
			// optionally hide id column visually: uncomment next line
			// colId.setVisible(false);
		}
		if (colFaculty != null) {
			colFaculty.setCellValueFactory(new PropertyValueFactory<>("facultyName"));
		}
		if (colSubject != null) {
			colSubject.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue() == null ? ""
					: (cell.getValue().getSubjectCode() + " - " + cell.getValue().getSubjectName())));
		}
		if (colSem != null) {
			colSem.setCellValueFactory(new PropertyValueFactory<>("semester"));
		}
		if (colAssignedAt != null) {
			colAssignedAt.setCellValueFactory(new PropertyValueFactory<>("assignedAt"));
		}

		// action column: Remove button
		if (colAction != null) {
			colAction.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
			colAction.setCellFactory(col -> new TableCell<>() {
				private final Button btn = new Button("Remove");
				{
					btn.getStyleClass().add("btn-danger");
					btn.setOnAction(e -> {
						FacultySubjectAssignment item = getTableRow().getItem();
						if (item != null)
							handleRemoveAssignment(item);
					});
				}

				@Override
				protected void updateItem(FacultySubjectAssignment item, boolean empty) {
					super.updateItem(item, empty);
					setGraphic((empty || item == null) ? null : btn);
				}
			});
		}

		// wire table items
		if (tblAssignments != null) {
			tblAssignments.setItems(assignments);
		}

		// set up combo box renderers and converters
		setupFacultyCombo();
		setupSubjectCombo();

		// load data (ensure runs on fx thread)
		Platform.runLater(() -> {
			loadFaculties();
			loadSubjects();
			loadAssignments();
		});

		// when semester changes, filter subjects shown
		if (cmbSemester != null) {
			cmbSemester.valueProperty().addListener((obs, oldv, newv) -> filterSubjectsBySemester(newv));
		}
	}

	@SuppressWarnings("unused")
	private void setupFacultyCombo() {
		if (cmbFaculty == null)
			return;

		cmbFaculty.setItems(facultyList);

		cmbFaculty.setCellFactory(cb -> new ListCell<>() {
			@Override
			protected void updateItem(FacultyModel item, boolean empty) {
				super.updateItem(item, empty);
				setText((empty || item == null) ? null : readableFacultyText(item));
			}
		});

		cmbFaculty.setButtonCell(new ListCell<>() {
			@Override
			protected void updateItem(FacultyModel item, boolean empty) {
				super.updateItem(item, empty);
				setText((empty || item == null) ? null : readableFacultyText(item));
			}
		});

		cmbFaculty.setConverter(new StringConverter<>() {
			@Override
			public String toString(FacultyModel fm) {
				return (fm == null) ? "" : readableFacultyText(fm);
			}

			@Override
			public FacultyModel fromString(String string) {
				return null; // not used
			}
		});
	}

	@SuppressWarnings("unused")
	private void setupSubjectCombo() {
		if (cmbSubject == null)
			return;

		cmbSubject.setItems(subjectList);

		cmbSubject.setCellFactory(cb -> new ListCell<>() {
			@Override
			protected void updateItem(Subject item, boolean empty) {
				super.updateItem(item, empty);
				setText((empty || item == null) ? null : (item.getCode() + " - " + item.getName()));
			}
		});

		cmbSubject.setButtonCell(new ListCell<>() {
			@Override
			protected void updateItem(Subject item, boolean empty) {
				super.updateItem(item, empty);
				setText((empty || item == null) ? null : (item.getCode() + " - " + item.getName()));
			}
		});

		cmbSubject.setConverter(new StringConverter<>() {
			@Override
			public String toString(Subject subject) {
				return (subject == null) ? "" : (subject.getCode() + " - " + subject.getName());
			}

			@Override
			public Subject fromString(String string) {
				return null;
			}
		});
	}

	private void filterSubjectsBySemester(String semStr) {
		if (cmbSubject == null)
			return;
		if (semStr == null || semStr.isBlank()) {
			cmbSubject.setItems(subjectList);
			return;
		}
		int sem;
		try {
			sem = Integer.parseInt(semStr);
		} catch (NumberFormatException ex) {
			cmbSubject.setItems(subjectList);
			return;
		}
		ObservableList<Subject> filtered = FXCollections.observableArrayList();
		for (Subject s : subjectList) {
			if (s != null && s.getSemester() == sem)
				filtered.add(s);
		}
		cmbSubject.setItems(filtered);
	}

	/**
	 * Loads faculty list and sets cell/rendering so ComboBox shows names (not
	 * toString()).
	 */
	private void loadFaculties() {
		facultyList.clear();
		try {
			@SuppressWarnings("static-access")
			List<FacultyModel> list = facultyDAO.findAll();
			if (list != null && !list.isEmpty()) {
				facultyList.addAll(list);
			} else {
				System.out.println("FacultyDAO.findAll() returned empty list or null");
			}
		} catch (Exception e) {
			e.printStackTrace();
			showAlert(Alert.AlertType.ERROR, "Load Error", "Could not load faculty list: " + e.getMessage());
		}

		if (facultyList.isEmpty()) {
			if (cmbFaculty != null)
				cmbFaculty.setPromptText("No faculty available");
		} else {
			if (cmbFaculty != null && cmbFaculty.getValue() == null)
				cmbFaculty.getSelectionModel().selectFirst();
		}
	}

	private String readableFacultyText(FacultyModel item) {
		if (item == null)
			return "";
		String name = null;
		try {
			name = item.getName();
		} catch (Throwable ignored) {
		}
		String email = null;
		try {
			email = item.getEmail();
		} catch (Throwable ignored) {
		}
		String dept = null;
		try {
			dept = item.getDepartment();
		} catch (Throwable ignored) {
		}
		String base = (name != null && !name.isBlank()) ? name
				: (email != null && !email.isBlank() ? email : "ID:" + item.getId());
		if (dept != null && !dept.isBlank())
			base = base + " (" + dept + ")";
		return base;
	}

	private void loadSubjects() {
		subjectList.clear();
		try {
			List<Subject> list = subjectDAO.findAll();
			if (list != null)
				subjectList.addAll(list);
		} catch (Exception e) {
			e.printStackTrace();
			showAlert(Alert.AlertType.ERROR, "Load Error", "Could not load subjects.");
		}

		if (cmbSubject != null && cmbSubject.getItems().isEmpty()) {
			// keep prompt text meaningful
			cmbSubject.setPromptText("No subjects available");
		}
	}

	private void loadAssignments() {
		assignments.clear();
		try {
			@SuppressWarnings("static-access")
			List<FacultySubjectAssignment> list = facultySubjectDAO.findAllAssignments();
			if (list != null)
				assignments.addAll(list);
		} catch (Exception e) {
			e.printStackTrace();
			showAlert(Alert.AlertType.ERROR, "Load Error", "Could not load assignments.");
		}
	}

	@FXML
	private void handleAssign() {
		if (cmbFaculty == null || cmbSubject == null || cmbSemester == null) {
			showAlert(Alert.AlertType.ERROR, "UI Error", "Required controls are not available.");
			return;
		}

		FacultyModel faculty = cmbFaculty.getSelectionModel().getSelectedItem();
		Subject subject = cmbSubject.getSelectionModel().getSelectedItem();
		String semStr = cmbSemester.getSelectionModel().getSelectedItem();

		if (faculty == null) {
			showAlert(Alert.AlertType.WARNING, "Validation", "Please select a faculty.");
			return;
		}
		if (subject == null) {
			showAlert(Alert.AlertType.WARNING, "Validation", "Please select a subject.");
			return;
		}
		if (semStr == null || semStr.isBlank()) {
			showAlert(Alert.AlertType.WARNING, "Validation", "Please select a semester.");
			return;
		}

		int semester;
		try {
			semester = Integer.parseInt(semStr);
		} catch (NumberFormatException ex) {
			showAlert(Alert.AlertType.WARNING, "Validation", "Invalid semester.");
			return;
		}

		try {
			@SuppressWarnings("static-access")
			boolean ok = facultySubjectDAO.assignSubjectToFaculty(faculty.getId(), subject.getId(), semester);
			if (ok) {
				showAlert(Alert.AlertType.INFORMATION, "Assigned", "Subject assigned successfully.");
				loadAssignments();
			} else {
				showAlert(Alert.AlertType.WARNING, "Not Assigned", "This assignment already exists.");
			}
		} catch (Exception e) {
			e.printStackTrace();
			showAlert(Alert.AlertType.ERROR, "Error", "Failed to assign subject: " + e.getMessage());
		}
	}

	private void handleRemoveAssignment(FacultySubjectAssignment assignment) {
		if (assignment == null)
			return;
		Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
				"Remove assignment for " + assignment.getSubjectCode() + " from " + assignment.getFacultyName() + "?",
				ButtonType.YES, ButtonType.NO);
		confirm.setHeaderText(null);
		Optional<ButtonType> res = confirm.showAndWait();
		if (res.isPresent() && res.get() == ButtonType.YES) {
			try {
				@SuppressWarnings("static-access")
				boolean ok = facultySubjectDAO.removeAssignment(assignment.getId());
				if (ok) {
					showAlert(Alert.AlertType.INFORMATION, "Removed", "Assignment removed.");
					loadAssignments();
				} else {
					showAlert(Alert.AlertType.ERROR, "Failed", "Could not remove assignment.");
				}
			} catch (Exception e) {
				e.printStackTrace();
				showAlert(Alert.AlertType.ERROR, "Error", "Failed to remove assignment: " + e.getMessage());
			}
		}
	}

	@FXML
	private void handleRefresh() {
		Platform.runLater(() -> {
			loadFaculties();
			loadSubjects();
			loadAssignments();
		});
	}

	private void showAlert(Alert.AlertType type, String title, String text) {
		Platform.runLater(() -> {
			Alert a = new Alert(type, text, ButtonType.OK);
			a.setHeaderText(title);
			a.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
			a.showAndWait();
		});
	}
}

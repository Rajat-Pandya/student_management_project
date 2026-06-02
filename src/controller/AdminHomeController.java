package controller;

import dao.AttendanceDAO;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ResourceBundle;

/**
 * AdminHomeController - background-loads statistics and updates UI.
 */
public class AdminHomeController implements Initializable {

	@FXML
	private Label lblTotalStudents;
	@FXML
	private Label lblTotalFaculty;
	@FXML
	private Label lblAvgAttendance;

	@FXML
	private Label lblWelcome;
	@FXML
	private Label lblDepartment;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		System.out.println("[AdminHome] initialize()");
		loadStats();
	}

	/**
	 * Public method to (re)load stats. Offloads heavy DB work to background threads
	 * and updates UI safely.
	 */
	public void loadStats() {
		loadTotalStudents();
		loadTotalFaculty();
		loadAvgAttendance();
	}

	private void loadTotalStudents() {
		// run DB query off the FX thread
		new Thread(() -> {
			final String query = "SELECT COUNT(*) AS total FROM students";
			int total = 0;
			try (Connection conn = util.DatabaseConnection.getConnection();
					PreparedStatement pst = conn.prepareStatement(query);
					ResultSet rs = pst.executeQuery()) {

				total = (rs.next()) ? rs.getInt("total") : 0;
				System.out.println("[AdminHome] total students = " + total);
			} catch (Exception e) {
				e.printStackTrace();
				total = 0;
			}

			final int finalTotal = total;
			Platform.runLater(() -> {
				if (lblTotalStudents != null)
					lblTotalStudents.setText(String.valueOf(finalTotal));
			});
		}, "AdminHome-loadTotalStudents").start();
	}

	private void loadTotalFaculty() {
		new Thread(() -> {
			final String query = "SELECT COUNT(*) AS total FROM faculty";
			int total = 0;
			try (Connection conn = util.DatabaseConnection.getConnection();
					PreparedStatement pst = conn.prepareStatement(query);
					ResultSet rs = pst.executeQuery()) {

				total = (rs.next()) ? rs.getInt("total") : 0;
				System.out.println("[AdminHome] total faculty = " + total);
			} catch (Exception e) {
				e.printStackTrace();
				total = 0;
			}

			final int finalTotal = total;
			Platform.runLater(() -> {
				if (lblTotalFaculty != null)
					lblTotalFaculty.setText(String.valueOf(finalTotal));
			});
		}, "AdminHome-loadTotalFaculty").start();
	}

	private void loadAvgAttendance() {
		new Thread(() -> {
			Double avg = null;
			try {
				// AttendanceDAO.computeAverageAttendance(null, PER_STUDENT) is expected to be
				// thread-safe
				avg = AttendanceDAO.computeAverageAttendance(null, AttendanceDAO.AttendanceMode.PER_STUDENT);
				System.out.println("[AdminHome] avg attendance computed = " + avg);
			} catch (Exception e) {
				e.printStackTrace();
				avg = null;
			}

			final Double finalAvg = avg;
			Platform.runLater(() -> {
				if (lblAvgAttendance != null) {
					if (finalAvg == null)
						lblAvgAttendance.setText("N/A");
					else
						lblAvgAttendance.setText(String.format("%.1f%%", finalAvg));
				}
			});
		}, "AdminHome-loadAvgAttendance").start();
	}

	/**
	 * Called by the parent controller when faculty info is known.
	 */
	public void setFacultyInfo(String username, String department) {
		Platform.runLater(() -> {
			if (lblWelcome != null && username != null)
				lblWelcome.setText("Welcome, " + username);
			if (lblDepartment != null && department != null)
				lblDepartment.setText(department);
		});

		// refresh stats for any context change
		loadStats();
	}
}

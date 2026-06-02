package controller;

import dao.AttendanceDAO;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * FacultyHomeController - uses AttendanceDAO with DB schema matching your dump.
 * - Uses students.course as the department field.
 * - Uses attendance_logs preferred, falls back to attendance.
 */
public class FacultyHomeController {

    @FXML private Label lblWelcome;
    @FXML private Label lblDepartment;
    @FXML private Label lblTotalStudents;
    @FXML private Label lblAvgAttendance;

    @SuppressWarnings("unused")
	private String facultyUsername;
    private String facultyDepartment;

    /**
     * Called by FacultyDashboardController to pass in faculty identity.
     */
    public void setFacultyInfo(String username, String department) {
        this.facultyUsername = username;
        this.facultyDepartment = department;

        if (lblWelcome != null && username != null) {
            Platform.runLater(() -> lblWelcome.setText("Welcome, " + username));
        }
        if (lblDepartment != null) {
            Platform.runLater(() -> lblDepartment.setText(department == null ? "N/A" : department));
        }

        refreshStats();
    }

    /**
     * Reload student count and average attendance for the assigned department.
     */
    public void refreshStats() {
        loadTotalStudents(this.facultyDepartment);
        loadAvgAttendance(this.facultyDepartment);
    }

    private void loadTotalStudents(String department) {
        System.out.println("[FacultyHome] loadTotalStudents department=" + department);
        if (department == null || department.trim().isEmpty()) {
            if (lblTotalStudents != null) {
                Platform.runLater(() -> lblTotalStudents.setText("N/A"));
            }
            return;
        }

        final String query = "SELECT COUNT(*) AS total FROM students WHERE course = ?";
        try (Connection conn = util.DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(query)) {

            pst.setString(1, department);
            try (ResultSet rs = pst.executeQuery()) {
                int total = (rs.next()) ? rs.getInt("total") : 0;
                if (lblTotalStudents != null) {
                    Platform.runLater(() -> lblTotalStudents.setText(String.valueOf(total)));
                }
                System.out.println("[FacultyHome] total students = " + total);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (lblTotalStudents != null) {
                Platform.runLater(() -> lblTotalStudents.setText("0"));
            }
        }
    }

    private void loadAvgAttendance(String department) {
        System.out.println("[FacultyHome] loadAvgAttendance department=" + department);
        Double avg = AttendanceDAO.computeAverageAttendance(department, AttendanceDAO.AttendanceMode.PER_STUDENT);
        if (avg == null) {
            if (lblAvgAttendance != null) {
                Platform.runLater(() -> lblAvgAttendance.setText("N/A"));
            }
        } else {
            if (lblAvgAttendance != null) {
                String formatted = String.format("%.1f%%", avg);
                Platform.runLater(() -> lblAvgAttendance.setText(formatted));
            }
            System.out.println("[FacultyHome] avg attendance = " + avg);
        }
    }
}

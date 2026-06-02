package dao;

import util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AttendanceDAO
 *
 * - findStatus(...) returns stored attendance status for a student/subject/date (or null).
 * - upsert(...) inserts or updates an attendance row; subjectId may be null.
 * - getAttendanceMap(...) returns Map<studentId, status> for a subject+date.
 * - computeAverageAttendance(...) computes average attendance preferring attendance_logs then attendance.
 *
 * Notes:
 * - Requires attendance table with columns: student_id, subject_id, attendance_date, status, recorded_by, recorded_at
 * - For upsert to work cleanly the table should have a UNIQUE(student_id, subject_id, attendance_date) constraint.
 * - Database connectivity is handled via DatabaseConnection.getConnection().
 */
public final class AttendanceDAO {

    private AttendanceDAO() { /* utility */ }

    /**
     * Return status (string) stored for given student/subject/date, or null if none.
     */
    public static String findStatus(int studentId, int subjectId, LocalDate date) {
        String sql = "SELECT status FROM attendance WHERE student_id = ? AND subject_id = ? AND attendance_date = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, subjectId);
            ps.setDate(3, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("status");
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Unified upsert method: subjectId may be nullable (Integer).
     * Callers can pass either primitive int or Integer (auto-boxing will apply).
     *
     * Uses MySQL-style INSERT ... ON DUPLICATE KEY UPDATE. Ensure unique constraint:
     *   UNIQUE(student_id, subject_id, attendance_date)
     */
    public static void upsert(int studentId, Integer subjectId, LocalDate date, String status, Integer recordedBy) {
        String sql = "INSERT INTO attendance (student_id, subject_id, attendance_date, status, recorded_by, recorded_at) " +
                     "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP) " +
                     "ON DUPLICATE KEY UPDATE status = VALUES(status), recorded_by = VALUES(recorded_by), recorded_at = CURRENT_TIMESTAMP";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            if (subjectId != null) ps.setInt(2, subjectId); else ps.setNull(2, Types.INTEGER);
            ps.setDate(3, Date.valueOf(date));
            ps.setString(4, status);
            if (recordedBy != null) ps.setInt(5, recordedBy); else ps.setNull(5, Types.INTEGER);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // ----------------------------- getAttendanceMap -----------------------------

    /**
     * Returns a map studentId -> status for the given subject and date.
     * Uses LinkedHashMap to preserve DB iteration order (helpful for UI).
     *
     * @param subjectId subject id (not null)
     * @param date      attendance date
     * @return map of studentId to status (empty map if none)
     */
    public static Map<Integer, String> getAttendanceMap(int subjectId, LocalDate date) {
        String sql = "SELECT student_id, status FROM attendance WHERE subject_id = ? AND attendance_date = ?";
        Map<Integer, String> map = new LinkedHashMap<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, subjectId);
            ps.setDate(2, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getInt("student_id"), rs.getString("status"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return map;
    }

    // ----------- Aggregate attendance computation (unchanged logic, defensive) -----------

    public enum AttendanceMode { PER_STUDENT, GLOBAL }

    /**
     * Compute average attendance. Tries attendance_logs first, then attendance table.
     *
     * @param department  department string (matches students.course), null for all
     * @param mode        PER_STUDENT (student-weighted) or GLOBAL (record-weighted)
     * @param minSessions minimum sessions per student for PER_STUDENT mode (0 = include all)
     * @return average percent or null if no usable data
     */
    public static Double computeAverageAttendance(String department, AttendanceMode mode, int minSessions) {
        if (mode == null) mode = AttendanceMode.PER_STUDENT;
        final boolean useDept = (department != null && !department.trim().isEmpty());

        try {
            Double v = computeUsingAttendanceLogs(department, mode, minSessions, useDept);
            if (v != null) return v;
        } catch (SQLException e) {
            System.err.println("[AttendanceDAO] attendance_logs attempt failed: " + e.getMessage());
        }

        try {
            Double v = computeUsingAttendanceTable(department, mode, minSessions, useDept);
            if (v != null) return v;
        } catch (SQLException e) {
            System.err.println("[AttendanceDAO] attendance table attempt failed: " + e.getMessage());
        }

        return null;
    }

    private static Double computeUsingAttendanceLogs(String department, AttendanceMode mode, int minSessions, boolean useDept) throws SQLException {
        String perStudentSql =
            "SELECT ROUND(AVG(student_pct),2) AS avg_att FROM ( " +
            "  SELECT a.student_id, 100.0 * SUM(CASE WHEN a.status = 'Present' THEN 1 ELSE 0 END) / COUNT(*) AS student_pct, COUNT(*) AS sessions_count " +
            "  FROM attendance_logs a " +
            "  JOIN students s ON s.id = a.student_id " +
            (useDept ? "  WHERE s.course = ? " : "") +
            "  GROUP BY a.student_id " +
            (minSessions > 0 ? " HAVING COUNT(*) >= " + minSessions : "") +
            ") t";

        String globalSql =
            "SELECT ROUND(100.0 * SUM(CASE WHEN a.status = 'Present' THEN 1 ELSE 0 END) / COUNT(*), 2) AS avg_att " +
            "FROM attendance_logs a JOIN students s ON s.id = a.student_id " +
            (useDept ? "WHERE s.course = ? " : "");

        String sql = (mode == AttendanceMode.PER_STUDENT) ? perStudentSql : globalSql;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (useDept) ps.setString(1, department);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double val = rs.getDouble("avg_att");
                    if (!rs.wasNull()) {
                        System.out.println("[AttendanceDAO] computeUsingAttendanceLogs succeeded. SQL used (attendance_logs).");
                        return val;
                    }
                }
            }
        }
        return null;
    }

    private static Double computeUsingAttendanceTable(String department, AttendanceMode mode, int minSessions, boolean useDept) throws SQLException {
        String perStudentSql =
            "SELECT ROUND(AVG(student_pct),2) AS avg_att FROM ( " +
            "  SELECT a.student_id, 100.0 * SUM(CASE WHEN a.status = 'P' THEN 1 ELSE 0 END) / COUNT(*) AS student_pct, COUNT(*) AS sessions_count " +
            "  FROM attendance a " +
            "  JOIN students s ON s.id = a.student_id " +
            (useDept ? "  WHERE s.course = ? " : "") +
            "  GROUP BY a.student_id " +
            (minSessions > 0 ? " HAVING COUNT(*) >= " + minSessions : "") +
            ") t";

        String globalSql =
            "SELECT ROUND(100.0 * SUM(CASE WHEN a.status = 'P' THEN 1 ELSE 0 END) / COUNT(*), 2) AS avg_att " +
            "FROM attendance a JOIN students s ON s.id = a.student_id " +
            (useDept ? "WHERE s.course = ? " : "");

        String sql = (mode == AttendanceMode.PER_STUDENT) ? perStudentSql : globalSql;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (useDept) ps.setString(1, department);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double val = rs.getDouble("avg_att");
                    if (!rs.wasNull()) {
                        System.out.println("[AttendanceDAO] computeUsingAttendanceTable succeeded. SQL used (attendance).");
                        return val;
                    }
                }
            }
        }
        return null;
    }

    // convenience overloads
    public static Double computeAverageAttendance(String department, AttendanceMode mode) {
        return computeAverageAttendance(department, mode, 0);
    }

    public static Double computeAverageAttendance(String department) {
        return computeAverageAttendance(department, AttendanceMode.PER_STUDENT, 0);
    }
}

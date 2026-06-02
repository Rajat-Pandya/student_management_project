package dao;

import util.DatabaseConnection;
import model.FacultySubjectAssignment;
import model.Subject;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class FacultySubjectDAO {

    /**
     * Assign subject to faculty for semester.
     * Returns true if inserted, false if assignment already exists.
     */
    public static boolean assignSubjectToFaculty(int facultyId, int subjectId, int semester) throws SQLException {
        String check = "SELECT id FROM faculty_subject WHERE faculty_id = ? AND subject_id = ? LIMIT 1";
        String insert = "INSERT INTO faculty_subject (faculty_id, subject_id, semester, created_at) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(check)) {
            pst.setInt(1, facultyId);
            pst.setInt(2, subjectId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return false; // already exists
            }
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(insert)) {
            pst.setInt(1, facultyId);
            pst.setInt(2, subjectId);
            pst.setInt(3, semester);
            pst.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            int changed = pst.executeUpdate();
            return changed > 0;
        }
    }

    /**
     * Remove assignment by id.
     */
    public static boolean removeAssignment(int id) throws SQLException {
        String sql = "DELETE FROM faculty_subject WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, id);
            int changed = pst.executeUpdate();
            return changed > 0;
        }
    }

    /**
     * Returns all assignments (joined with faculty + subject for display).
     */
    public static List<FacultySubjectAssignment> findAllAssignments() {
        List<FacultySubjectAssignment> out = new ArrayList<>();
        String sql = "SELECT fs.id AS fsid, fs.semester, fs.created_at, f.id AS fid, f.name AS faculty_name, s.id AS sid, s.code AS subject_code, s.name AS subject_name " +
                     "FROM faculty_subject fs " +
                     "LEFT JOIN faculty f ON fs.faculty_id = f.id " +
                     "LEFT JOIN subject s ON fs.subject_id = s.id " +
                     "ORDER BY fs.id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("fsid");
                int semester = rs.getInt("semester");
                Timestamp ts = rs.getTimestamp("created_at");
                LocalDate assigned = ts != null ? ts.toLocalDateTime().toLocalDate() : null;
                int fid = rs.getInt("fid");
                String facultyName = rs.getString("faculty_name");
                int sid = rs.getInt("sid");
                String subjectCode = rs.getString("subject_code");
                String subjectName = rs.getString("subject_name");

                FacultySubjectAssignment a = new FacultySubjectAssignment(id, fid, facultyName, sid, subjectCode, subjectName, semester, assigned);
                out.add(a);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return out;
    }

    /**
     * Finds subjects assigned to a faculty id (list of Subject).
     */
    public static List<Subject> findSubjectsByFaculty(int facultyId) {
        List<Subject> out = new ArrayList<>();
        String sql = "SELECT s.id, s.code, s.name, s.semester FROM faculty_subject fs " +
                     "JOIN subject s ON fs.subject_id = s.id WHERE fs.faculty_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, facultyId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    Subject s = new Subject(rs.getInt("id"), rs.getString("code"), rs.getString("name"), rs.getInt("semester"));
                    out.add(s);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return out;
    }
}

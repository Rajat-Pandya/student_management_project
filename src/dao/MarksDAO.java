package dao;

import util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MarksDAO {

    public static class StudentMark {
        public int studentId;
        public String studentName;
        public String roll;
        public Integer marks; // nullable
        public String grade;  // nullable

        public StudentMark(int studentId, String studentName, String roll, Integer marks, String grade) {
            this.studentId = studentId;
            this.studentName = studentName;
            this.roll = roll;
            this.marks = marks;
            this.grade = grade;
        }

        @Override
        public String toString() {
            return "StudentMark{studentId=" + studentId + ", name='" + studentName + "', roll='" + roll + "', marks=" + marks + ", grade=" + grade + "}";
        }
    }

    public boolean upsertMark(int studentId, int subjectId, int semester, int facultyId, Integer marks, String grade) {
        String sql =
            "INSERT INTO marks (student_id, subject_id, semester, faculty_id, marks, grade, updated_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP) " +
            "ON DUPLICATE KEY UPDATE marks = VALUES(marks), grade = VALUES(grade), faculty_id = VALUES(faculty_id), updated_at = CURRENT_TIMESTAMP";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setInt(1, studentId);
            pst.setInt(2, subjectId);
            pst.setInt(3, semester);
            pst.setInt(4, facultyId);
            if (marks == null) pst.setNull(5, Types.INTEGER); else pst.setInt(5, marks);
            if (grade == null) pst.setNull(6, Types.VARCHAR); else pst.setString(6, grade);

            int rows = pst.executeUpdate();
            System.out.println("[MarksDAO] upsert returned affectedRows=" + rows + " for student=" + studentId + " subject=" + subjectId + " sem=" + semester);
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("[MarksDAO] upsert SQL error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public List<StudentMark> findMarksForSubjectAndSemester(int subjectId, int semester) {
        List<StudentMark> list = new ArrayList<>();
        String sql =
            "SELECT s.id AS sid, s.name AS sname, s.roll_number AS sroll, m.marks AS marks, m.grade AS grade " +
            "FROM students s " +
            "LEFT JOIN marks m ON m.student_id = s.id AND m.subject_id = ? AND m.semester = ? " +
            "WHERE s.semester = ? " +
            "ORDER BY s.roll_number";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, subjectId);
            pst.setInt(2, semester);
            pst.setInt(3, semester);

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    int sid = rs.getInt("sid");
                    String sname = rs.getString("sname");
                    String sroll = rs.getString("sroll");
                    Integer marks = rs.getObject("marks") == null ? null : rs.getInt("marks");
                    String grade = rs.getString("grade");
                    list.add(new StudentMark(sid, sname, sroll, marks, grade));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public Optional<StudentMark> findMarkForStudent(int studentId, int subjectId, int semester) {
        String sql = "SELECT m.marks AS marks, m.grade AS grade, s.name AS sname, s.roll_number AS sroll " +
                     "FROM marks m JOIN students s ON s.id = m.student_id " +
                     "WHERE m.student_id = ? AND m.subject_id = ? AND m.semester = ? LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, studentId);
            pst.setInt(2, subjectId);
            pst.setInt(3, semester);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    Integer marks = rs.getObject("marks") == null ? null : rs.getInt("marks");
                    String grade = rs.getString("grade");
                    String sname = rs.getString("sname");
                    String sroll = rs.getString("sroll");
                    return Optional.of(new StudentMark(studentId, sname, sroll, marks, grade));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public List<String> getRawMarkRow(int studentId, int subjectId, int semester) {
        List<String> out = new ArrayList<>();
        String sql = "SELECT * FROM marks WHERE student_id = ? AND subject_id = ? AND semester = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, studentId);
            pst.setInt(2, subjectId);
            pst.setInt(3, semester);
            try (ResultSet rs = pst.executeQuery()) {
                ResultSetMetaData md = rs.getMetaData();
                int colCount = md.getColumnCount();
                while (rs.next()) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 1; i <= colCount; ++i) {
                        String colName = md.getColumnName(i);
                        Object val = rs.getObject(i);
                        sb.append(colName).append("=").append(val).append("; ");
                    }
                    out.add(sb.toString());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return out;
    }
}

package util;

import model.Student;
import model.FacultyModel;
import model.Subject;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Advanced search utility for the Student Management System.
 * Provides comprehensive filtering and search capabilities.
 */
public class SearchUtil {
    
    // ==================== Student Search ====================
    
    /**
     * Search students with multiple filter criteria.
     */
    public static List<Student> searchStudents(String query, String course, String semester, 
                                               String department, Double minAttendance, 
                                               Double maxAttendance, Integer year) throws SQLException {
        List<Student> results = new ArrayList<>();
        
        StringBuilder sql = new StringBuilder("SELECT DISTINCT s.* FROM students s WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        
        // Text search
        if (query != null && !query.trim().isEmpty()) {
            sql.append("AND (s.name LIKE ? OR s.roll_number LIKE ? OR s.email LIKE ? OR s.phone LIKE ?) ");
            String searchPattern = "%" + query.trim() + "%";
            params.add(searchPattern);
            params.add(searchPattern);
            params.add(searchPattern);
            params.add(searchPattern);
        }
        
        // Course filter
        if (course != null && !course.trim().isEmpty()) {
            sql.append("AND s.course = ? ");
            params.add(course);
        }
        
        // Semester filter
        if (semester != null && !semester.trim().isEmpty()) {
            sql.append("AND s.semester = ? ");
            params.add(semester);
        }
        
        // Department filter
        if (department != null && !department.trim().isEmpty()) {
            sql.append("AND s.department = ? ");
            params.add(department);
        }
        
        // Attendance range
        if (minAttendance != null || maxAttendance != null) {
            sql.append("AND s.id IN (SELECT student_id FROM attendance GROUP BY student_id ");
            sql.append("HAVING AVG(CASE WHEN status = 'Present' THEN 1 ELSE 0 END) * 100 BETWEEN ? AND ?) ");
            params.add(minAttendance != null ? minAttendance : 0);
            params.add(maxAttendance != null ? maxAttendance : 100);
        }
        
        // Year filter (based on roll number or creation date)
        if (year != null) {
            sql.append("AND (s.roll_number LIKE ? OR YEAR(s.created_at) = ?) ");
            params.add("%" + year + "%");
            params.add(year);
        }
        
        sql.append("ORDER BY s.name");
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                results.add(mapResultSetToStudent(rs));
            }
        }
        
        return results;
    }
    
    /**
     * Quick search students by name, roll number, or email.
     */
    public static List<Student> quickSearchStudents(String query) throws SQLException {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        return searchStudents(query, null, null, null, null, null, null);
    }
    
    /**
     * Filter students by course.
     */
    public static List<Student> filterStudentsByCourse(String course) throws SQLException {
        return searchStudents(null, course, null, null, null, null, null);
    }
    
    /**
     * Filter students by semester.
     */
    public static List<Student> filterStudentsBySemester(String semester) throws SQLException {
        return searchStudents(null, null, semester, null, null, null, null);
    }
    
    /**
     * Filter students by attendance range.
     */
    public static List<Student> filterStudentsByAttendance(double minPercent, double maxPercent) throws SQLException {
        return searchStudents(null, null, null, null, minPercent, maxPercent, null);
    }
    
    /**
     * Get students with low attendance.
     */
    public static List<Student> getStudentsWithLowAttendance(double threshold) throws SQLException {
        return searchStudents(null, null, null, null, 0.0, threshold, null);
    }
    
    // ==================== Faculty Search ====================
    
    /**
     * Search faculty with filters.
     */
    public static List<FacultyModel> searchFaculty(String query, String department, 
                                                   String subject) throws SQLException {
        List<FacultyModel> results = new ArrayList<>();
        
        StringBuilder sql = new StringBuilder(
            "SELECT DISTINCT f.id, f.name, f.email, f.username, f.password, f.phone, f.department, f.role " +
            "FROM faculty f WHERE 1=1 "
        );
        List<Object> params = new ArrayList<>();
        
        // Text search
        if (query != null && !query.trim().isEmpty()) {
            sql.append("AND (f.name LIKE ? OR f.username LIKE ? OR f.email LIKE ?) ");
            String searchPattern = "%" + query.trim() + "%";
            params.add(searchPattern);
            params.add(searchPattern);
            params.add(searchPattern);
        }
        
        // Department filter
        if (department != null && !department.trim().isEmpty()) {
            sql.append("AND f.department = ? ");
            params.add(department);
        }
        
        // Subject filter
        if (subject != null && !subject.trim().isEmpty()) {
            sql.append("AND f.id IN (SELECT faculty_id FROM faculty_subjects fs " +
                       "JOIN subjects s ON fs.subject_id = s.id WHERE s.name LIKE ?) ");
            params.add("%" + subject + "%");
        }
        
        sql.append("ORDER BY f.name");
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                results.add(mapResultSetToFaculty(rs));
            }
        }
        
        return results;
    }
    
    /**
     * Quick search faculty.
     */
    public static List<FacultyModel> quickSearchFaculty(String query) throws SQLException {
        return searchFaculty(query, null, null);
    }
    
    // ==================== Subject Search ====================
    
    /**
     * Search subjects with filters.
     */
    public static List<Subject> searchSubjects(String query, String semester, 
                                               String department) throws SQLException {
        List<Subject> results = new ArrayList<>();
        
        StringBuilder sql = new StringBuilder("SELECT * FROM subjects WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        
        // Text search
        if (query != null && !query.trim().isEmpty()) {
            sql.append("AND (name LIKE ? OR code LIKE ?) ");
            String searchPattern = "%" + query.trim() + "%";
            params.add(searchPattern);
            params.add(searchPattern);
        }
        
        // Semester filter
        if (semester != null && !semester.trim().isEmpty()) {
            sql.append("AND semester = ? ");
            params.add(Integer.parseInt(semester));
        }
        
        // Department filter
        if (department != null && !department.trim().isEmpty()) {
            sql.append("AND department = ? ");
            params.add(department);
        }
        
        sql.append("ORDER BY semester, name");
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                results.add(mapResultSetToSubject(rs));
            }
        }
        
        return results;
    }
    
    /**
     * Quick search subjects.
     */
    public static List<Subject> quickSearchSubjects(String query) throws SQLException {
        return searchSubjects(query, null, null);
    }
    
    // ==================== Marks Search ====================
    
    /**
     * Search marks with filters.
     */
    public static List<MarkRecord> searchMarks(String studentQuery, String subject, 
                                                String semester, Integer minMarks, 
                                                Integer maxMarks, String grade) throws SQLException {
        List<MarkRecord> results = new ArrayList<>();
        
        StringBuilder sql = new StringBuilder(
            "SELECT m.id, s.name as student_name, s.roll_number, sub.name as subject_name, " +
            "m.semester, m.marks, m.grade " +
            "FROM marks m " +
            "JOIN students s ON m.student_id = s.id " +
            "JOIN subjects sub ON m.subject_id = sub.id " +
            "WHERE 1=1 "
        );
        List<Object> params = new ArrayList<>();
        
        // Student search
        if (studentQuery != null && !studentQuery.trim().isEmpty()) {
            sql.append("AND (s.name LIKE ? OR s.roll_number LIKE ?) ");
            String searchPattern = "%" + studentQuery.trim() + "%";
            params.add(searchPattern);
            params.add(searchPattern);
        }
        
        // Subject filter
        if (subject != null && !subject.trim().isEmpty()) {
            sql.append("AND sub.name = ? ");
            params.add(subject);
        }
        
        // Semester filter
        if (semester != null && !semester.trim().isEmpty()) {
            sql.append("AND m.semester = ? ");
            params.add(semester);
        }
        
        // Marks range
        if (minMarks != null) {
            sql.append("AND m.marks >= ? ");
            params.add(minMarks);
        }
        
        if (maxMarks != null) {
            sql.append("AND m.marks <= ? ");
            params.add(maxMarks);
        }
        
        // Grade filter
        if (grade != null && !grade.trim().isEmpty()) {
            sql.append("AND m.grade = ? ");
            params.add(grade);
        }
        
        sql.append("ORDER BY s.roll_number, sub.name");
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                results.add(mapResultSetToMarkRecord(rs));
            }
        }
        
        return results;
    }
    
    /**
     * Get students with marks in a range.
     */
    public static List<MarkRecord> getStudentsByMarksRange(int minMarks, int maxMarks) throws SQLException {
        return searchMarks(null, null, null, minMarks, maxMarks, null);
    }
    
    /**
     * Get students by grade.
     */
    public static List<MarkRecord> getStudentsByGrade(String grade) throws SQLException {
        return searchMarks(null, null, null, null, null, grade);
    }
    
    // ==================== Attendance Search ====================
    
    /**
     * Search attendance records.
     */
    public static List<AttendanceRecord> searchAttendance(String studentQuery, String subject,
                                                           Date fromDate, Date toDate,
                                                           String status) throws SQLException {
        List<AttendanceRecord> results = new ArrayList<>();
        
        StringBuilder sql = new StringBuilder(
            "SELECT a.id, a.attendance_date, s.name as student_name, s.roll_number, " +
            "sub.name as subject_name, a.status " +
            "FROM attendance a " +
            "JOIN students s ON a.student_id = s.id " +
            "LEFT JOIN subjects sub ON a.subject_id = sub.id " +
            "WHERE 1=1 "
        );
        List<Object> params = new ArrayList<>();
        
        // Student search
        if (studentQuery != null && !studentQuery.trim().isEmpty()) {
            sql.append("AND (s.name LIKE ? OR s.roll_number LIKE ?) ");
            String searchPattern = "%" + studentQuery.trim() + "%";
            params.add(searchPattern);
            params.add(searchPattern);
        }
        
        // Subject filter
        if (subject != null && !subject.trim().isEmpty()) {
            sql.append("AND sub.name = ? ");
            params.add(subject);
        }
        
        // Date range
        if (fromDate != null) {
            sql.append("AND a.attendance_date >= ? ");
            params.add(new java.sql.Date(fromDate.getTime()));
        }
        
        if (toDate != null) {
            sql.append("AND a.attendance_date <= ? ");
            params.add(new java.sql.Date(toDate.getTime()));
        }
        
        // Status filter
        if (status != null && !status.trim().isEmpty()) {
            sql.append("AND a.status = ? ");
            params.add(status);
        }
        
        sql.append("ORDER BY a.attendance_date DESC, s.roll_number");
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                results.add(mapResultSetToAttendanceRecord(rs));
            }
        }
        
        return results;
    }
    
    /**
     * Get attendance for a specific date.
     */
    public static List<AttendanceRecord> getAttendanceByDate(Date date) throws SQLException {
        return searchAttendance(null, null, date, date, null);
    }
    
    // ==================== Result Classes ====================
    
    public static class MarkRecord {
        public final int id;
        public final String studentName;
        public final String rollNumber;
        public final String subjectName;
        public final String semester;
        public final int marks;
        public final String grade;
        
        public MarkRecord(int id, String studentName, String rollNumber, String subjectName,
                         String semester, int marks, String grade) {
            this.id = id;
            this.studentName = studentName;
            this.rollNumber = rollNumber;
            this.subjectName = subjectName;
            this.semester = semester;
            this.marks = marks;
            this.grade = grade;
        }
    }
    
    public static class AttendanceRecord {
        public final int id;
        public final Date date;
        public final String studentName;
        public final String rollNumber;
        public final String subjectName;
        public final String status;
        
        public AttendanceRecord(int id, Date date, String studentName, String rollNumber,
                               String subjectName, String status) {
            this.id = id;
            this.date = date;
            this.studentName = studentName;
            this.rollNumber = rollNumber;
            this.subjectName = subjectName;
            this.status = status;
        }
    }
    
    // ==================== Mapper Methods ====================
    
    private static Student mapResultSetToStudent(ResultSet rs) throws SQLException {
        return new Student(
            rs.getInt("id"),
            rs.getString("roll_number"),
            rs.getString("name"),
            rs.getString("course"),
            rs.getString("email"),
            rs.getString("phone"),
            rs.getString("course"),
            rs.getString("semester"),
            rs.getInt("attendance")
        );
    }

    private static FacultyModel mapResultSetToFaculty(ResultSet rs) throws SQLException {
        return new FacultyModel(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("username"),
                rs.getString("password"),
                rs.getString("phone"),
                rs.getString("department"),
                rs.getString("role")
        );
    }
    
    private static Subject mapResultSetToSubject(ResultSet rs) throws SQLException {
        return new Subject(
            rs.getInt("id"),
            rs.getString("code"),
            rs.getString("name"),
            rs.getInt("semester"),
            rs.getString("department")
        );
    }
    
    private static MarkRecord mapResultSetToMarkRecord(ResultSet rs) throws SQLException {
        return new MarkRecord(
            rs.getInt("id"),
            rs.getString("student_name"),
            rs.getString("roll_number"),
            rs.getString("subject_name"),
            rs.getString("semester"),
            rs.getInt("marks"),
            rs.getString("grade")
        );
    }
    
    private static AttendanceRecord mapResultSetToAttendanceRecord(ResultSet rs) throws SQLException {
        return new AttendanceRecord(
            rs.getInt("id"),
            rs.getDate("attendance_date"),
            rs.getString("student_name"),
            rs.getString("roll_number"),
            rs.getString("subject_name"),
            rs.getString("status")
        );
    }
    
    // ==================== Filter Presets ====================
    
    /**
     * Get filter presets for common searches.
     */
    public static FilterPresets getFilterPresets() {
        return new FilterPresets();
    }
    
    public static class FilterPresets {
        public List<String> getCourses() {
            return List.of("CSE", "ECE", "Civil", "Mechanical", "IT", "EEE");
        }
        
        public List<String> getSemesters() {
            return List.of("1", "2", "3", "4", "5", "6", "7", "8");
        }
        
        public List<String> getDepartments() {
            return List.of("Computer Science", "Electronics", "Civil", "Mechanical", "IT");
        }
        
        public List<String> getGrades() {
            return List.of("A+", "A", "B+", "B", "C", "D", "F");
        }
        
        public List<String> getAttendanceStatus() {
            return List.of("Present", "Absent", "Late", "Excused");
        }
    }
    
    // ==================== Real-time Search ====================
    
    /**
     * Perform a real-time search with debouncing.
     * This method should be called with appropriate delays to avoid excessive DB queries.
     */
    public static List<Student> realTimeStudentSearch(String query, int minChars) throws SQLException {
        if (query == null || query.length() < minChars) {
            return new ArrayList<>();
        }
        return quickSearchStudents(query);
    }
    
    /**
     * Filter list in memory (for already loaded data).
     */
    public static List<Student> filterInMemory(List<Student> students, String query) {
        if (query == null || query.trim().isEmpty()) {
            return students;
        }
        
        String lowerQuery = query.toLowerCase().trim();
        return students.stream()
            .filter(s -> 
                s.getName().toLowerCase().contains(lowerQuery) ||
                s.getRoll().toLowerCase().contains(lowerQuery) ||
                (s.getEmail() != null && s.getEmail().toLowerCase().contains(lowerQuery)) ||
                (s.getPhone() != null && s.getPhone().toLowerCase().contains(lowerQuery))
            )
            .collect(Collectors.toList());
    }
}

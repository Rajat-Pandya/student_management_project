package util;

import dao.AttendanceDAO;

import java.sql.*;
import java.util.*;

/**
 * Analytics utility class for generating dashboard statistics and reports.
 * Provides comprehensive data analysis for the Student Management System.
 */
public class AnalyticsUtil {
    
    // Student statistics
    public static class StudentStats {
        public final int totalStudents;
        public final int newStudentsThisMonth;
        public final Map<String, Integer> studentsByCourse;
        public final Map<String, Integer> studentsBySemester;
        
        public StudentStats(int totalStudents, int newStudentsThisMonth, 
                          Map<String, Integer> studentsByCourse,
                          Map<String, Integer> studentsBySemester) {
            this.totalStudents = totalStudents;
            this.newStudentsThisMonth = newStudentsThisMonth;
            this.studentsByCourse = studentsByCourse;
            this.studentsBySemester = studentsBySemester;
        }
    }
    
    // Attendance statistics
    public static class AttendanceStats {
        public final double overallAttendance;
        public final int presentCount;
        public final int absentCount;
        public final Map<String, Double> attendanceByCourse;
        public final Map<String, Double> attendanceBySubject;
        
        public AttendanceStats(double overallAttendance, int presentCount, int absentCount,
                              Map<String, Double> attendanceByCourse,
                              Map<String, Double> attendanceBySubject) {
            this.overallAttendance = overallAttendance;
            this.presentCount = presentCount;
            this.absentCount = absentCount;
            this.attendanceByCourse = attendanceByCourse;
            this.attendanceBySubject = attendanceBySubject;
        }
    }
    
    // Marks statistics
    public static class MarksStats {
        public final double averageMarks;
        public final int highestMarks;
        public final int lowestMarks;
        public final int totalPassed;
        public final int totalFailed;
        public final Map<String, Double> averageBySubject;
        public final Map<String, Map<String, Integer>> gradeDistribution;
        
        public MarksStats(double averageMarks, int highestMarks, int lowestMarks,
                         int totalPassed, int totalFailed,
                         Map<String, Double> averageBySubject,
                         Map<String, Map<String, Integer>> gradeDistribution) {
            this.averageMarks = averageMarks;
            this.highestMarks = highestMarks;
            this.lowestMarks = lowestMarks;
            this.totalPassed = totalPassed;
            this.totalFailed = totalFailed;
            this.averageBySubject = averageBySubject;
            this.gradeDistribution = gradeDistribution;
        }
    }
    
    // Faculty statistics
    public static class FacultyStats {
        public final int totalFaculty;
        public final int totalDepartments;
        public final Map<String, Integer> facultyByDepartment;
        public final double averageSubjectsPerFaculty;
        
        public FacultyStats(int totalFaculty, int totalDepartments,
                          Map<String, Integer> facultyByDepartment,
                          double averageSubjectsPerFaculty) {
            this.totalFaculty = totalFaculty;
            this.totalDepartments = totalDepartments;
            this.facultyByDepartment = facultyByDepartment;
            this.averageSubjectsPerFaculty = averageSubjectsPerFaculty;
        }
    }
    
    // ==================== Student Analytics ====================
    
    /**
     * Get comprehensive student statistics.
     */
    public static StudentStats getStudentStats() throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            int totalStudents = getTotalStudents(conn);
            int newStudentsThisMonth = getNewStudentsThisMonth(conn);
            Map<String, Integer> studentsByCourse = getStudentsByCourse(conn);
            Map<String, Integer> studentsBySemester = getStudentsBySemester(conn);
            
            return new StudentStats(totalStudents, newStudentsThisMonth, 
                                   studentsByCourse, studentsBySemester);
        }
    }
    
    private static int getTotalStudents(Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) as total FROM students";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getInt("total") : 0;
        }
    }
    
    private static int getNewStudentsThisMonth(Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) as total FROM students WHERE MONTH(created_at) = MONTH(CURRENT_DATE) AND YEAR(created_at) = YEAR(CURRENT_DATE)";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getInt("total") : 0;
        }
    }
    
    private static Map<String, Integer> getStudentsByCourse(Connection conn) throws SQLException {
        Map<String, Integer> result = new LinkedHashMap<>();
        String sql = "SELECT course, COUNT(*) as total FROM students GROUP BY course ORDER BY total DESC";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                result.put(rs.getString("course"), rs.getInt("total"));
            }
        }
        return result;
    }
    
    private static Map<String, Integer> getStudentsBySemester(Connection conn) throws SQLException {
        Map<String, Integer> result = new LinkedHashMap<>();
        String sql = "SELECT semester, COUNT(*) as total FROM students GROUP BY semester ORDER BY semester ASC";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                result.put(rs.getString("semester"), rs.getInt("total"));
            }
        }
        return result;
    }
    
    // ==================== Attendance Analytics ====================
    
    /**
     * Get comprehensive attendance statistics.
     */
    public static AttendanceStats getAttendanceStats() throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            double overallAttendance = AttendanceDAO.computeAverageAttendance(null, AttendanceDAO.AttendanceMode.PER_STUDENT);
            if (Double.isNaN(overallAttendance)) overallAttendance = 0.0;
            
            Map<String, Integer> counts = getAttendanceCounts(conn);
            int presentCount = counts.getOrDefault("Present", 0);
            int absentCount = counts.getOrDefault("Absent", 0);
            
            Map<String, Double> attendanceByCourse = getAttendanceByCourse(conn);
            Map<String, Double> attendanceBySubject = getAttendanceBySubject(conn);
            
            return new AttendanceStats(overallAttendance, presentCount, absentCount,
                                      attendanceByCourse, attendanceBySubject);
        }
    }
    
    private static Map<String, Integer> getAttendanceCounts(Connection conn) throws SQLException {
        Map<String, Integer> result = new HashMap<>();
        String sql = "SELECT status, COUNT(*) as total FROM attendance GROUP BY status";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                result.put(rs.getString("status"), rs.getInt("total"));
            }
        }
        return result;
    }
    
    private static Map<String, Double> getAttendanceByCourse(Connection conn) throws SQLException {
        Map<String, Double> result = new LinkedHashMap<>();
        String sql = "SELECT s.course, AVG(CASE WHEN a.status = 'Present' THEN 1 ELSE 0 END) * 100 as avg_attendance " +
                    "FROM students s LEFT JOIN attendance a ON s.id = a.student_id " +
                    "GROUP BY s.course";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                result.put(rs.getString("course"), rs.getDouble("avg_attendance"));
            }
        }
        return result;
    }
    
    private static Map<String, Double> getAttendanceBySubject(Connection conn) throws SQLException {
        Map<String, Double> result = new LinkedHashMap<>();
        String sql = "SELECT sub.name, AVG(CASE WHEN a.status = 'Present' THEN 1 ELSE 0 END) * 100 as avg_attendance " +
                    "FROM subjects sub LEFT JOIN attendance a ON sub.id = a.subject_id " +
                    "GROUP BY sub.id, sub.name";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                result.put(rs.getString("name"), rs.getDouble("avg_attendance"));
            }
        }
        return result;
    }
    
    // ==================== Marks Analytics ====================
    
    /**
     * Get comprehensive marks statistics.
     */
    public static MarksStats getMarksStats() throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            Map<String, Object> basicStats = getBasicMarksStats(conn);
            double averageMarks = (Double) basicStats.get("average");
            int highestMarks = (Integer) basicStats.get("highest");
            int lowestMarks = (Integer) basicStats.get("lowest");
            int totalPassed = (Integer) basicStats.get("passed");
            int totalFailed = (Integer) basicStats.get("failed");
            
            Map<String, Double> averageBySubject = getAverageBySubject(conn);
            Map<String, Map<String, Integer>> gradeDistribution = getGradeDistribution(conn);
            
            return new MarksStats(averageMarks, highestMarks, lowestMarks,
                                 totalPassed, totalFailed, averageBySubject, gradeDistribution);
        }
    }
    
    private static Map<String, Object> getBasicMarksStats(Connection conn) throws SQLException {
        Map<String, Object> result = new HashMap<>();
        String sql = "SELECT AVG(marks) as avg_marks, MAX(marks) as max_marks, MIN(marks) as min_marks, " +
                    "SUM(CASE WHEN marks >= 40 THEN 1 ELSE 0 END) as passed, " +
                    "SUM(CASE WHEN marks < 40 THEN 1 ELSE 0 END) as failed " +
                    "FROM marks WHERE marks IS NOT NULL";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                result.put("average", rs.getDouble("avg_marks"));
                result.put("highest", rs.getInt("max_marks"));
                result.put("lowest", rs.getInt("min_marks"));
                result.put("passed", rs.getInt("passed"));
                result.put("failed", rs.getInt("failed"));
            }
        }
        return result;
    }
    
    private static Map<String, Double> getAverageBySubject(Connection conn) throws SQLException {
        Map<String, Double> result = new LinkedHashMap<>();
        String sql = "SELECT s.name, AVG(m.marks) as avg_marks FROM marks m " +
                    "JOIN subjects s ON m.subject_id = s.id GROUP BY s.id, s.name";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                result.put(rs.getString("name"), rs.getDouble("avg_marks"));
            }
        }
        return result;
    }
    
    private static Map<String, Map<String, Integer>> getGradeDistribution(Connection conn) throws SQLException {
        Map<String, Map<String, Integer>> result = new LinkedHashMap<>();
        String sql = "SELECT s.name, m.grade, COUNT(*) as count FROM marks m " +
                    "JOIN subjects s ON m.subject_id = s.id " +
                    "WHERE m.grade IS NOT NULL " +
                    "GROUP BY s.id, s.name, m.grade";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String subject = rs.getString("name");
                String grade = rs.getString("grade");
                int count = rs.getInt("count");
                
                result.computeIfAbsent(subject, _ -> new HashMap<>()).put(grade, count);
            }
        }
        return result;
    }
    
    // ==================== Faculty Analytics ====================
    
    /**
     * Get comprehensive faculty statistics.
     */
    public static FacultyStats getFacultyStats() throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            int totalFaculty = getTotalFaculty(conn);
            Map<String, Integer> facultyByDepartment = getFacultyByDepartment(conn);
            int totalDepartments = facultyByDepartment.size();
            double avgSubjects = getAverageSubjectsPerFaculty(conn);
            
            return new FacultyStats(totalFaculty, totalDepartments, facultyByDepartment, avgSubjects);
        }
    }
    
    private static int getTotalFaculty(Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) as total FROM faculty";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getInt("total") : 0;
        }
    }
    
    private static Map<String, Integer> getFacultyByDepartment(Connection conn) throws SQLException {
        Map<String, Integer> result = new LinkedHashMap<>();
        String sql = "SELECT department, COUNT(*) as total FROM faculty GROUP BY department ORDER BY total DESC";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                result.put(rs.getString("department"), rs.getInt("total"));
            }
        }
        return result;
    }
    
    private static double getAverageSubjectsPerFaculty(Connection conn) throws SQLException {
        String sql = "SELECT AVG(subject_count) as avg_subjects FROM (" +
                    "SELECT faculty_id, COUNT(*) as subject_count FROM faculty_subject GROUP BY faculty_id" +
                    ") as subquery";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getDouble("avg_subjects") : 0.0;
        }
    }
    
    // ==================== Quick Statistics ====================
    
    /**
     * Get quick dashboard statistics as a map.
     */
    public static Map<String, Object> getQuickStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            StudentStats studentStats = getStudentStats();
            stats.put("totalStudents", studentStats.totalStudents);
            stats.put("newStudentsThisMonth", studentStats.newStudentsThisMonth);
        } catch (SQLException e) {
            stats.put("totalStudents", 0);
            stats.put("newStudentsThisMonth", 0);
        }
        
        try {
            FacultyStats facultyStats = getFacultyStats();
            stats.put("totalFaculty", facultyStats.totalFaculty);
            stats.put("totalDepartments", facultyStats.totalDepartments);
        } catch (SQLException e) {
            stats.put("totalFaculty", 0);
            stats.put("totalDepartments", 0);
        }
        
        try {
            AttendanceStats attendanceStats = getAttendanceStats();
            stats.put("averageAttendance", String.format("%.1f%%", attendanceStats.overallAttendance));
            stats.put("presentCount", attendanceStats.presentCount);
            stats.put("absentCount", attendanceStats.absentCount);
        } catch (SQLException e) {
            stats.put("averageAttendance", "N/A");
            stats.put("presentCount", 0);
            stats.put("absentCount", 0);
        }
        
        try {
            MarksStats marksStats = getMarksStats();
            stats.put("averageMarks", String.format("%.1f", marksStats.averageMarks));
            stats.put("totalPassed", marksStats.totalPassed);
            stats.put("totalFailed", marksStats.totalFailed);
        } catch (SQLException e) {
            stats.put("averageMarks", "N/A");
            stats.put("totalPassed", 0);
            stats.put("totalFailed", 0);
        }
        
        return stats;
    }
    
    // ==================== Report Generation ====================
    
    /**
     * Generate a monthly report with key metrics.
     */
    public static Map<String, Object> generateMonthlyReport(int month, int year) throws SQLException {
        Map<String, Object> report = new HashMap<>();
        report.put("month", month);
        report.put("year", year);
        report.put("generatedAt", new java.util.Date());
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            // New students this month
            String newStudentsSql = "SELECT COUNT(*) as total FROM students " +
                                   "WHERE MONTH(created_at) = ? AND YEAR(created_at) = ?";
            try (PreparedStatement stmt = conn.prepareStatement(newStudentsSql)) {
                stmt.setInt(1, month);
                stmt.setInt(2, year);
                ResultSet rs = stmt.executeQuery();
                report.put("newStudents", rs.next() ? rs.getInt("total") : 0);
            }
            
            // Attendance summary for the month
            String attendanceSql = "SELECT COUNT(*) as total, " +
                                  "SUM(CASE WHEN status = 'Present' THEN 1 ELSE 0 END) as present " +
                                  "FROM attendance WHERE MONTH(date) = ? AND YEAR(date) = ?";
            try (PreparedStatement stmt = conn.prepareStatement(attendanceSql)) {
                stmt.setInt(1, month);
                stmt.setInt(2, year);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    int total = rs.getInt("total");
                    int present = rs.getInt("present");
                    report.put("attendanceTotal", total);
                    report.put("attendancePresent", present);
                    report.put("attendanceRate", total > 0 ? (present * 100.0 / total) : 0.0);
                }
            }
            
            // Marks summary
            String marksSql = "SELECT AVG(marks) as avg_marks, COUNT(*) as total_marks " +
                             "FROM marks WHERE MONTH(updated_at) = ? AND YEAR(updated_at) = ?";
            try (PreparedStatement stmt = conn.prepareStatement(marksSql)) {
                stmt.setInt(1, month);
                stmt.setInt(2, year);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    report.put("averageMarks", rs.getDouble("avg_marks"));
                    report.put("totalMarksEntered", rs.getInt("total_marks"));
                }
            }
        }
        
        return report;
    }
    
    /**
     * Get performance trends over time.
     */
    public static List<Map<String, Object>> getPerformanceTrends(int months) throws SQLException {
        List<Map<String, Object>> trends = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            Calendar cal = Calendar.getInstance();
            
            for (int i = 0; i < months; i++) {
                Map<String, Object> monthData = new HashMap<>();
                int month = cal.get(Calendar.MONTH) + 1;
                int year = cal.get(Calendar.YEAR);
                
                monthData.put("month", month);
                monthData.put("year", year);
                monthData.put("monthName", getMonthName(month));
                
                // Average marks for this month
                String marksSql = "SELECT AVG(marks) as avg_marks FROM marks " +
                                 "WHERE MONTH(updated_at) = ? AND YEAR(updated_at) = ?";
                try (PreparedStatement stmt = conn.prepareStatement(marksSql)) {
                    stmt.setInt(1, month);
                    stmt.setInt(2, year);
                    ResultSet rs = stmt.executeQuery();
                    monthData.put("averageMarks", rs.next() ? rs.getDouble("avg_marks") : 0.0);
                }
                
                // Attendance rate for this month
                String attendanceSql = "SELECT AVG(CASE WHEN status = 'Present' THEN 1 ELSE 0 END) * 100 as rate " +
                                      "FROM attendance WHERE MONTH(date) = ? AND YEAR(date) = ?";
                try (PreparedStatement stmt = conn.prepareStatement(attendanceSql)) {
                    stmt.setInt(1, month);
                    stmt.setInt(2, year);
                    ResultSet rs = stmt.executeQuery();
                    monthData.put("attendanceRate", rs.next() ? rs.getDouble("rate") : 0.0);
                }
                
                trends.add(monthData);
                cal.add(Calendar.MONTH, -1);
            }
        }
        
        Collections.reverse(trends);
        return trends;
    }
    
    private static String getMonthName(int month) {
        String[] months = {"January", "February", "March", "April", "May", "June",
                          "July", "August", "September", "October", "November", "December"};
        return months[month - 1];
    }
}

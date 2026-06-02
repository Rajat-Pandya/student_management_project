package util;

import model.Student;
import model.FacultyModel;
import model.Subject;

import java.io.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Report export utility for generating PDF and CSV reports.
 * Provides comprehensive export functionality for the Student Management System.
 */
public class ReportExportUtil {
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    // ==================== CSV Export ====================
    
    /**
     * Export students data to CSV.
     */
    public static boolean exportStudentsToCSV(File file) {
        try (Connection conn = DatabaseConnection.getConnection();
             PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            
            // Write header
            writer.println("ID,Roll Number,Name,Course,Semester,Email,Phone,Attendance,Created At");
            
            // Write data
            String sql = "SELECT * FROM students ORDER BY id";
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                
                while (rs.next()) {
                    writer.printf("%d,\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                        rs.getInt("id"),
                        escapeCSV(rs.getString("roll_number")),
                        escapeCSV(rs.getString("name")),
                        escapeCSV(rs.getString("course")),
                        escapeCSV(rs.getString("semester")),
                        escapeCSV(rs.getString("email")),
                        escapeCSV(rs.getString("phone")),
                        escapeCSV(rs.getString("attendance")),
                        rs.getTimestamp("created_at")
                    );
                }
            }
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Export faculty data to CSV.
     */
    public static boolean exportFacultyToCSV(File file) {
        try (Connection conn = DatabaseConnection.getConnection();
             PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            
            writer.println("ID,Name,Username,Email,Phone,Department,Role,Created At");
            
            String sql = "SELECT id, name, username, email, phone, department, role, created_at FROM faculty ORDER BY id";
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                
                while (rs.next()) {
                    writer.printf("%d,\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                        rs.getInt("id"),
                        escapeCSV(rs.getString("name")),
                        escapeCSV(rs.getString("username")),
                        escapeCSV(rs.getString("email")),
                        escapeCSV(rs.getString("phone")),
                        escapeCSV(rs.getString("department")),
                        escapeCSV(rs.getString("role")),
                        rs.getTimestamp("created_at")
                    );
                }
            }
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Export marks data to CSV.
     */
    public static boolean exportMarksToCSV(File file, String semester) {
        try (Connection conn = DatabaseConnection.getConnection();
             PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            
            writer.println("Student ID,Student Name,Roll Number,Subject,Semester,Marks,Grade,Recorded By,Recorded At");
            
            String sql = "SELECT m.id, s.id as student_id, s.name as student_name, s.roll_number, " +
                        "sub.name as subject_name, m.semester, m.marks, m.grade, " +
                        "f.name as faculty_name, m.updated_at " +
                        "FROM marks m " +
                        "JOIN students s ON m.student_id = s.id " +
                        "JOIN subjects sub ON m.subject_id = sub.id " +
                        "LEFT JOIN faculty f ON m.faculty_id = f.id " +
                        (semester != null && !semester.isEmpty() ? "WHERE m.semester = ? " : "") +
                        "ORDER BY s.roll_number, sub.name";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                if (semester != null && !semester.isEmpty()) {
                    stmt.setString(1, semester);
                }
                
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    writer.printf("%d,\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                        rs.getInt("student_id"),
                        escapeCSV(rs.getString("student_name")),
                        escapeCSV(rs.getString("roll_number")),
                        escapeCSV(rs.getString("subject_name")),
                        escapeCSV(rs.getString("semester")),
                        rs.getObject("marks") != null ? rs.getInt("marks") : "",
                        escapeCSV(rs.getString("grade")),
                        escapeCSV(rs.getString("faculty_name")),
                        rs.getTimestamp("updated_at")
                    );
                }
            }
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Export attendance data to CSV.
     */
    public static boolean exportAttendanceToCSV(File file, Date fromDate, Date toDate) {
        try (Connection conn = DatabaseConnection.getConnection();
             PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            
            writer.println("Date,Student ID,Student Name,Roll Number,Subject,Status");
            
            String sql = "SELECT a.attendance_date, s.id as student_id, s.name as student_name, " +
                        "s.roll_number, sub.name as subject_name, a.status " +
                        "FROM attendance a " +
                        "JOIN students s ON a.student_id = s.id " +
                        "LEFT JOIN subjects sub ON a.subject_id = sub.id " +
                        "WHERE a.attendance_date BETWEEN ? AND ? " +
                        "ORDER BY a.attendance_date DESC, s.roll_number";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setDate(1, new java.sql.Date(fromDate.getTime()));
                stmt.setDate(2, new java.sql.Date(toDate.getTime()));
                
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    writer.printf("\"%s\",%d,\"%s\",\"%s\",\"%s\",\"%s\"%n",
                        rs.getDate("attendance_date"),
                        rs.getInt("student_id"),
                        escapeCSV(rs.getString("student_name")),
                        escapeCSV(rs.getString("roll_number")),
                        escapeCSV(rs.getString("subject_name")),
                        escapeCSV(rs.getString("status"))
                    );
                }
            }
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Export database backup (all tables) to CSV files.
     */
    public static Map<String, Boolean> exportDatabaseBackup(File directory) {
        Map<String, Boolean> results = new HashMap<>();
        String[] tables = {"students", "faculty", "subjects", "marks", "attendance", "users", "faculty_subjects"};
        
        for (String table : tables) {
            File file = new File(directory, table + "_" + DATE_FORMAT.format(new Date()) + ".csv");
            boolean success = exportTableToCSV(table, file);
            results.put(table, success);
        }
        
        return results;
    }
    
    private static boolean exportTableToCSV(String tableName, File file) {
        try (Connection conn = DatabaseConnection.getConnection();
             PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            
            // Get column names
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet columns = metaData.getColumns(null, null, tableName, null);
            List<String> columnNames = new ArrayList<>();
            
            while (columns.next()) {
                columnNames.add(columns.getString("COLUMN_NAME"));
            }
            
            // Write header
            writer.println(String.join(",", columnNames));
            
            // Write data
            String sql = "SELECT * FROM " + tableName;
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                
                while (rs.next()) {
                    StringBuilder row = new StringBuilder();
                    for (int i = 0; i < columnNames.size(); i++) {
                        if (i > 0) row.append(",");
                        Object value = rs.getObject(i + 1);
                        if (value != null) {
                            row.append("\"").append(escapeCSV(value.toString())).append("\"");
                        }
                    }
                    writer.println(row);
                }
            }
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // ==================== Report Generation ====================
    
    /**
     * Generate a comprehensive student report card.
     */
    public static String generateStudentReportCard(int studentId) {
        StringBuilder report = new StringBuilder();
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Student info
            String studentSql = "SELECT * FROM students WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(studentSql)) {
                stmt.setInt(1, studentId);
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    report.append("STUDENT REPORT CARD\n");
                    report.append("==================\n\n");
                    report.append("Name: ").append(rs.getString("name")).append("\n");
                    report.append("Roll Number: ").append(rs.getString("roll_number")).append("\n");
                    report.append("Course: ").append(rs.getString("course")).append("\n");
                    report.append("Semester: ").append(rs.getString("semester")).append("\n");
                    report.append("Email: ").append(rs.getString("email")).append("\n");
                    report.append("Phone: ").append(rs.getString("phone")).append("\n\n");
                }
            }
            
            // Marks
            report.append("ACADEMIC PERFORMANCE\n");
            report.append("-------------------\n");
            
            String marksSql = "SELECT sub.name as subject, m.marks, m.grade " +
                             "FROM marks m JOIN subjects sub ON m.subject_id = sub.id " +
                             "WHERE m.student_id = ? ORDER BY sub.name";
            try (PreparedStatement stmt = conn.prepareStatement(marksSql)) {
                stmt.setInt(1, studentId);
                ResultSet rs = stmt.executeQuery();
                
                double totalMarks = 0;
                int count = 0;
                
                while (rs.next()) {
                    String subject = rs.getString("subject");
                    int marks = rs.getInt("marks");
                    String grade = rs.getString("grade");
                    
                    report.append(String.format("%-20s %3d  %s%n", subject, marks, grade));
                    totalMarks += marks;
                    count++;
                }
                
                if (count > 0) {
                    report.append("\n");
                    report.append(String.format("Average Marks: %.2f%%", totalMarks / count)).append("\n");
                }
            }
            
            // Attendance
            report.append("\nATTENDANCE SUMMARY\n");
            report.append("-----------------\n");
            
            String attendanceSql = "SELECT COUNT(*) as total, " +
                                  "SUM(CASE WHEN status = 'Present' THEN 1 ELSE 0 END) as present " +
                                  "FROM attendance WHERE student_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(attendanceSql)) {
                stmt.setInt(1, studentId);
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    int total = rs.getInt("total");
                    int present = rs.getInt("present");
                    double percentage = total > 0 ? (present * 100.0 / total) : 0;
                    
                    report.append(String.format("Total Classes: %d%n", total));
                    report.append(String.format("Present: %d%n", present));
                    report.append(String.format("Absent: %d%n", total - present));
                    report.append(String.format("Attendance Rate: %.2f%%", percentage)).append("\n");
                }
            }
            
            report.append("\n==================\n");
            report.append("Generated: ").append(TIMESTAMP_FORMAT.format(new Date())).append("\n");
            
        } catch (SQLException e) {
            e.printStackTrace();
            report.append("Error generating report: ").append(e.getMessage());
        }
        
        return report.toString();
    }
    
    /**
     * Generate class performance report.
     */
    public static String generateClassReport(String course, String semester) {
        StringBuilder report = new StringBuilder();
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            report.append("CLASS PERFORMANCE REPORT\n");
            report.append("========================\n\n");
            report.append("Course: ").append(course != null ? course : "All").append("\n");
            report.append("Semester: ").append(semester != null ? semester : "All").append("\n");
            report.append("Generated: ").append(TIMESTAMP_FORMAT.format(new Date())).append("\n\n");
            
            // Student count
            String countSql = "SELECT COUNT(*) as total FROM students WHERE 1=1 " +
                            (course != null ? "AND course = ? " : "") +
                            (semester != null ? "AND semester = ? " : "");
            
            try (PreparedStatement stmt = conn.prepareStatement(countSql)) {
                int paramIndex = 1;
                if (course != null) stmt.setString(paramIndex++, course);
                if (semester != null) stmt.setString(paramIndex++, semester);
                
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    report.append("Total Students: ").append(rs.getInt("total")).append("\n\n");
                }
            }
            
            // Subject-wise average
            report.append("SUBJECT-WISE PERFORMANCE\n");
            report.append("-----------------------\n");
            
            String subjectSql = "SELECT sub.name as subject, AVG(m.marks) as avg_marks, " +
                               "MAX(m.marks) as max_marks, MIN(m.marks) as min_marks " +
                               "FROM marks m JOIN subjects sub ON m.subject_id = sub.id " +
                               "JOIN students s ON m.student_id = s.id " +
                               "WHERE 1=1 " +
                               (course != null ? "AND s.course = ? " : "") +
                               (semester != null ? "AND s.semester = ? " : "") +
                               "GROUP BY sub.id, sub.name";
            
            try (PreparedStatement stmt = conn.prepareStatement(subjectSql)) {
                int paramIndex = 1;
                if (course != null) stmt.setString(paramIndex++, course);
                if (semester != null) stmt.setString(paramIndex++, semester);
                
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    report.append(String.format("%-20s Avg: %6.2f  Max: %3d  Min: %3d%n",
                        rs.getString("subject"),
                        rs.getDouble("avg_marks"),
                        rs.getInt("max_marks"),
                        rs.getInt("min_marks")));
                }
            }
            
            // Grade distribution
            report.append("\nGRADE DISTRIBUTION\n");
            report.append("-----------------\n");
            
            String gradeSql = "SELECT m.grade, COUNT(*) as count " +
                             "FROM marks m JOIN students s ON m.student_id = s.id " +
                             "WHERE 1=1 " +
                             (course != null ? "AND s.course = ? " : "") +
                             (semester != null ? "AND s.semester = ? " : "") +
                             "GROUP BY m.grade ORDER BY count DESC";
            
            try (PreparedStatement stmt = conn.prepareStatement(gradeSql)) {
                int paramIndex = 1;
                if (course != null) stmt.setString(paramIndex++, course);
                if (semester != null) stmt.setString(paramIndex++, semester);
                
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    report.append(String.format("%-5s: %d%n",
                        rs.getString("grade"),
                        rs.getInt("count")));
                }
            }
            
            report.append("\n========================\n");
            
        } catch (SQLException e) {
            e.printStackTrace();
            report.append("Error generating report: ").append(e.getMessage());
        }
        
        return report.toString();
    }
    
    /**
     * Generate attendance summary report.
     */
    public static String generateAttendanceReport(Date fromDate, Date toDate) {
        StringBuilder report = new StringBuilder();
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            report.append("ATTENDANCE REPORT\n");
            report.append("=================\n\n");
            report.append("From: ").append(DATE_FORMAT.format(fromDate)).append("\n");
            report.append("To: ").append(DATE_FORMAT.format(toDate)).append("\n");
            report.append("Generated: ").append(TIMESTAMP_FORMAT.format(new Date())).append("\n\n");
            
            // Overall statistics
            String statsSql = "SELECT COUNT(*) as total, " +
                             "SUM(CASE WHEN status = 'Present' THEN 1 ELSE 0 END) as present, " +
                             "SUM(CASE WHEN status = 'Absent' THEN 1 ELSE 0 END) as absent " +
                             "FROM attendance WHERE attendance_date BETWEEN ? AND ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(statsSql)) {
                stmt.setDate(1, new java.sql.Date(fromDate.getTime()));
                stmt.setDate(2, new java.sql.Date(toDate.getTime()));
                
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    int total = rs.getInt("total");
                    int present = rs.getInt("present");
                    int absent = rs.getInt("absent");
                    double percentage = total > 0 ? (present * 100.0 / total) : 0;
                    
                    report.append("Total Records: ").append(total).append("\n");
                    report.append("Present: ").append(present).append("\n");
                    report.append("Absent: ").append(absent).append("\n");
                    report.append(String.format("Attendance Rate: %.2f%%", percentage)).append("\n\n");
                }
            }
            
            // Daily breakdown
            report.append("DAILY BREAKDOWN\n");
            report.append("--------------\n");
            
            String dailySql = "SELECT attendance_date, COUNT(*) as total, " +
                             "SUM(CASE WHEN status = 'Present' THEN 1 ELSE 0 END) as present " +
                             "FROM attendance WHERE attendance_date BETWEEN ? AND ? " +
                             "GROUP BY attendance_date ORDER BY attendance_date DESC";
            
            try (PreparedStatement stmt = conn.prepareStatement(dailySql)) {
                stmt.setDate(1, new java.sql.Date(fromDate.getTime()));
                stmt.setDate(2, new java.sql.Date(toDate.getTime()));
                
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    Date date = rs.getDate("attendance_date");
                    int total = rs.getInt("total");
                    int present = rs.getInt("present");
                    double percentage = total > 0 ? (present * 100.0 / total) : 0;
                    
                    report.append(String.format("%s: %d/%d (%.1f%%)%n",
                        DATE_FORMAT.format(date), present, total, percentage));
                }
            }
            
            report.append("\n=================\n");
            
        } catch (SQLException e) {
            e.printStackTrace();
            report.append("Error generating report: ").append(e.getMessage());
        }
        
        return report.toString();
    }
    
    // ==================== Helper Methods ====================
    
    private static String escapeCSV(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"").replace("\n", " ").replace("\r", "");
    }
    
    /**
     * Get file extension.
     */
    public static String getFileExtension(File file) {
        String name = file.getName();
        int lastIndex = name.lastIndexOf('.');
        return lastIndex > 0 ? name.substring(lastIndex + 1).toLowerCase() : "";
    }
    
    /**
     * Ensure file has correct extension.
     */
    public static File ensureExtension(File file, String extension) {
        String name = file.getName();
        if (!name.toLowerCase().endsWith("." + extension.toLowerCase())) {
            return new File(file.getParentFile(), name + "." + extension);
        }
        return file;
    }
    
    /**
     * Generate unique filename with timestamp.
     */
    public static String generateTimestampedFilename(String prefix, String extension) {
        return prefix + "_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + "." + extension;
    }
}

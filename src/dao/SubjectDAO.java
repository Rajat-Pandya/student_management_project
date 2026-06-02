package dao;

import util.DatabaseConnection;
import model.Subject;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SubjectDAO {

    // Find all subjects (includes department)
    public List<Subject> findAll() {
        List<Subject> list = new ArrayList<>();
        String sql = "SELECT id, code, name, semester, department FROM subject ORDER BY id";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Subject s = new Subject();
                s.setId(rs.getInt("id"));
                s.setCode(rs.getString("code"));
                s.setName(rs.getString("name"));
                s.setSemester(rs.getInt("semester"));
                s.setDepartment(rs.getString("department"));
                list.add(s);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // Insert subject (includes department)
    public boolean insert(Subject s) {
        String sql = "INSERT INTO subject (code, name, semester, department) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pst.setString(1, s.getCode());
            pst.setString(2, s.getName());
            pst.setInt(3, s.getSemester());
            pst.setString(4, s.getDepartment());
            int affected = pst.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = pst.getGeneratedKeys()) {
                    if (rs.next()) s.setId(rs.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Update subject (includes department)
    public boolean update(Subject s) {
        String sql = "UPDATE subject SET code = ?, name = ?, semester = ?, department = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, s.getCode());
            pst.setString(2, s.getName());
            pst.setInt(3, s.getSemester());
            pst.setString(4, s.getDepartment());
            pst.setInt(5, s.getId());
            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Delete subject
    public boolean delete(int id) {
        String sql = "DELETE FROM subject WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, id);
            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
 // Find subjects by semester
    public List<Subject> findBySemester(int semester) {
        List<Subject> list = new ArrayList<>();
        String sql = "SELECT id, code, name, semester, department FROM subject WHERE semester = ? ORDER BY code";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, semester);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    Subject s = new Subject();
                    s.setId(rs.getInt("id"));
                    s.setCode(rs.getString("code"));
                    s.setName(rs.getString("name"));
                    s.setSemester(rs.getInt("semester"));
                    s.setDepartment(rs.getString("department"));
                    list.add(s);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

}

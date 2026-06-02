package dao;

import util.DatabaseConnection;
import model.FacultyModel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FacultyDAO {

    /**
     * Return all faculty rows (id, name, department, email, username, role, phone).
     */
    public static List<FacultyModel> findAll() {
        List<FacultyModel> out = new ArrayList<>();
        String sql = "SELECT id, name, department, email, username, password, phone, role FROM faculty ORDER BY name";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                FacultyModel fm = new FacultyModel(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("email"),
                    rs.getString("username"),
                    rs.getString("password"),
                    rs.getString("phone"),
                    rs.getString("department"),
                    rs.getString("role")
                );
                out.add(fm);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return out;
    }

    /**
     * Finds faculty id by username or email. Returns -1 if not found.
     */
    public static int findIdByUsername(String username) {
        if (username == null || username.isBlank()) return -1;
        String sql = "SELECT id FROM faculty WHERE username = ? OR email = ? LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, username);
            pst.setString(2, username);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }
}

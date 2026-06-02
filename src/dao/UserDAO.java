package dao;

import util.PasswordUtils;
import util.DatabaseConnection;
import model.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * UserDAO - basic CRUD for users.
 *
 * Notes:
 * - PasswordUtils.hashPassword(password, iterations) is called with iterations = 10000.
 * - Adjust SQL and column names to match your DB schema if needed.
 */
public final class UserDAO {

    private UserDAO() { /* static utility */ }

    /**
     * Create a new user in the database.
     *
     * @param username unique username
     * @param rawPassword plain-text password (will be hashed)
     * @param role e.g. "ADMIN", "FACULTY", "STUDENT"
     * @return true if inserted successfully
     */
    public static boolean createUser(String username, String rawPassword, String role) {
        if (username == null || username.isBlank()) return false;
        if (rawPassword == null) rawPassword = "";

        // Hash password with an explicit iteration count.
        String hashed = rawPassword;
        try {
            hashed = PasswordUtils.hashPassword(rawPassword, 10000);
        } catch (Throwable t) {
            // Don't fail entirely if hashing crashes — log and fallback to storing raw (not ideal for prod)
            System.err.println("[UserDAO] Password hashing failed: " + t.getMessage());
            t.printStackTrace();
        }

        String sql = "INSERT INTO users (username, password_hash, role, created_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, hashed);
            ps.setString(3, role == null ? "USER" : role);

            int affected = ps.executeUpdate();
            return affected == 1;
        } catch (SQLException ex) {
            // Handle duplicate username or other DB errors
            System.err.println("[UserDAO] createUser failed for username=" + username + " : " + ex.getMessage());
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * Find a user by username.
     *
     * @param username username
     * @return User model or null if not found
     */
    public static User findByUsername(String username) {
        if (username == null || username.isBlank()) return null;

        String sql = "SELECT id, username, password_hash, role, created_at FROM users WHERE username = ? LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User u = new User();
                    u.setId(rs.getInt("id"));
                    u.setUsername(rs.getString("username"));
                    u.setPasswordHash(rs.getString("password_hash"));
                    u.setRole(rs.getString("role"));
                    u.setCreatedAt(rs.getTimestamp("created_at"));
                    return u;
                }
            }
        } catch (SQLException ex) {
            System.err.println("[UserDAO] findByUsername failed for username=" + username + " : " + ex.getMessage());
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * Update user's role or password.
     *
     * @param userId id of user to update
     * @param newRawPassword nullable - if provided will be hashed and updated
     * @param newRole nullable - if provided will be updated
     * @return true if an update occurred
     */
    public static boolean updateUser(int userId, String newRawPassword, String newRole) {
        if (userId <= 0) return false;

        // Build dynamic SQL depending on which fields provided
        List<String> parts = new ArrayList<>();
        if (newRawPassword != null) parts.add("password_hash = ?");
        if (newRole != null) parts.add("role = ?");
        if (parts.isEmpty()) return false;

        String sql = "UPDATE users SET " + String.join(", ", parts) + " WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int idx = 1;
            if (newRawPassword != null) {
                String hashed = newRawPassword;
                try {
                    hashed = PasswordUtils.hashPassword(newRawPassword, 10000);
                } catch (Throwable t) {
                    System.err.println("[UserDAO] Password hashing failed on update: " + t.getMessage());
                    t.printStackTrace();
                }
                ps.setString(idx++, hashed);
            }
            if (newRole != null) {
                ps.setString(idx++, newRole);
            }
            ps.setInt(idx, userId);

            int affected = ps.executeUpdate();
            return affected >= 1;
        } catch (SQLException ex) {
            System.err.println("[UserDAO] updateUser failed for id=" + userId + " : " + ex.getMessage());
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * Delete a user by id.
     *
     * @param userId user id
     * @return true if deleted
     */
    public static boolean deleteUser(int userId) {
        if (userId <= 0) return false;
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            int affected = ps.executeUpdate();
            return affected == 1;
        } catch (SQLException ex) {
            System.err.println("[UserDAO] deleteUser failed for id=" + userId + " : " + ex.getMessage());
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * List all users (basic).
     *
     * @return list of User models
     */
    public static List<User> listUsers() {
        List<User> out = new ArrayList<>();
        String sql = "SELECT id, username, role, created_at FROM users ORDER BY username";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                User u = new User();
                u.setId(rs.getInt("id"));
                u.setUsername(rs.getString("username"));
                u.setRole(rs.getString("role"));
                u.setCreatedAt(rs.getTimestamp("created_at"));
                out.add(u);
            }
        } catch (SQLException ex) {
            System.err.println("[UserDAO] listUsers failed: " + ex.getMessage());
            ex.printStackTrace();
        }
        return out;
    }
}

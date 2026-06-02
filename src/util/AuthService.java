package util;

import java.sql.*;

/**
 * AuthService: authenticate against users/faculty, auto-upgrade plaintext to bcrypt,
 * and upgrade bcrypt cost when it differs from desiredCost.
 *
 * Usage:
 *   AuthService.AuthResult result = AuthService.authenticate(conn, "username", "secret", 12);
 */
public class AuthService {

    public static class AuthResult {
        public final boolean authenticated;
        public final String role;       // e.g. "admin" or "faculty"
        public final String department; // may be null
        public final int id;            // DB id of the authenticated user (users or faculty)

        public AuthResult(boolean ok, String role, String dept, int id) {
            this.authenticated = ok;
            this.role = role;
            this.department = dept;
            this.id = id;
        }
    }

    /**
     * Main authenticate entry:
     * - checks `users` by username
     * - if not found, checks `faculty` by username OR email
     * - automatically re-hashes legacy plaintext passwords or upgrades bcrypt cost
     *
     * @param conn open DB connection (will be used for SELECT and possibly UPDATE)
     * @param usernameOrEmail the username input (or email)
     * @param providedPlain the plaintext password provided by user
     * @param desiredCost bcrypt cost (e.g. 12)
     * @return AuthResult (authenticated==true on success)
     * @throws SQLException on DB errors
     */
    public static AuthResult authenticate(Connection conn, String usernameOrEmail, String providedPlain, int desiredCost) throws SQLException {
        // 1) Try users table
        String usersSql = "SELECT id, password, role, department FROM users WHERE username = ? LIMIT 1";
        try (PreparedStatement pst = conn.prepareStatement(usersSql)) {
            pst.setString(1, usernameOrEmail);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("id");
                    String stored = rs.getString("password");
                    String role = rs.getString("role");
                    String dept = rs.getString("department");

                    boolean ok = verifyAndUpgradeIfNeeded(conn, "users", "id", id, stored, providedPlain, desiredCost);
                    return new AuthResult(ok, ok ? role : null, ok ? dept : null, ok ? id : -1);
                }
            }
        }

        // 2) Try faculty table (username or email)
        String facSql = "SELECT id, password, department, name FROM faculty WHERE username = ? OR email = ? LIMIT 1";
        try (PreparedStatement pst = conn.prepareStatement(facSql)) {
            pst.setString(1, usernameOrEmail);
            pst.setString(2, usernameOrEmail);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("id");
                    String stored = rs.getString("password");
                    String dept = rs.getString("department");
                    String role = "faculty";

                    if (stored == null || stored.isEmpty()) {
                        return new AuthResult(false, null, null, -1);
                    }

                    boolean ok = verifyAndUpgradeIfNeeded(conn, "faculty", "id", id, stored, providedPlain, desiredCost);
                    return new AuthResult(ok, ok ? role : null, ok ? dept : null, ok ? id : -1);
                }
            }
        }

        // no account found
        return new AuthResult(false, null, null, -1);
    }

    /**
     * Verify given storedPassword vs providedPlain.
     * If legacy plaintext matches, re-hash and update DB record.
     * If bcrypt matches but cost differs, re-hash with desiredCost and update DB.
     *
     * @return true if verification succeeds
     */
    private static boolean verifyAndUpgradeIfNeeded(Connection conn,
                                                    String table,
                                                    String idColumn,
                                                    int idValue,
                                                    String storedPassword,
                                                    String providedPlain,
                                                    int desiredCost) throws SQLException {
        if (storedPassword == null) return false;

        if (PasswordUtils.looksLikeBcrypt(storedPassword)) {
            // bcrypt path
            boolean ok = PasswordUtils.verifyPassword(providedPlain, storedPassword);
            if (!ok) return false;

            // check cost and upgrade if needed
            if (PasswordUtils.needsRehash(storedPassword, desiredCost)) {
                rehashAndSave(conn, table, idColumn, idValue, providedPlain, desiredCost);
            }
            return true;
        } else {
            // legacy plaintext stored (or other non-bcrypt)
            if (providedPlain.equals(storedPassword)) {
                // Immediately hash and update DB
                rehashAndSave(conn, table, idColumn, idValue, providedPlain, desiredCost);
                return true;
            } else {
                return false;
            }
        }
    }

    /** Create new bcrypt hash and update the record in `table`. */
    private static void rehashAndSave(Connection conn, String table, String idColumn, int idValue, String plain, int desiredCost) throws SQLException {
        // Validate table to avoid injection risk
        if (!"users".equals(table) && !"faculty".equals(table)) {
            throw new SQLException("Invalid table for password update: " + table);
        }

        String newHash = PasswordUtils.hashPassword(plain, desiredCost);
        String sql = "UPDATE " + table + " SET password = ? WHERE " + idColumn + " = ?";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, newHash);
            pst.setInt(2, idValue);
            int updated = pst.executeUpdate();
            if (updated <= 0) {
                // update failed - log (printing here, but use logger in production)
                System.err.println("Warning: failed to update password hash for " + table + " id=" + idValue);
            } else {
                System.out.println("Password hash updated for " + table + " id=" + idValue);
            }
        }
    }

    /**
     * Convenience: create a new user in `users` table (hashes password before insert).
     */
    public static void createUser(Connection conn, String username, String plainPassword, String role, String department, int desiredCost) throws SQLException {
        String hashed = PasswordUtils.hashPassword(plainPassword, desiredCost);
        String sql = "INSERT INTO users (username, password, role, department) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, username);
            pst.setString(2, hashed);
            pst.setString(3, role);
            pst.setString(4, department);
            pst.executeUpdate();
        }
    }
}

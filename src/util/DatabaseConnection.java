package util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;

public class DatabaseConnection {

    private static final String DB_TYPE = System.getenv("DB_TYPE");
    private static final String MYSQL_URL = Objects.requireNonNullElse(System.getenv("DB_URL"),
            "jdbc:mysql://localhost:3306/student_db?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=UTC");
    private static final String MYSQL_USER = Objects.requireNonNullElse(System.getenv("DB_USER"), "root");
    private static final String MYSQL_PASSWORD = Objects.requireNonNullElse(System.getenv("DB_PASSWORD"), "");
    private static final String SQLITE_FILE = Objects.requireNonNullElse(System.getenv("DB_SQLITE"), "student_db.db");

    private DatabaseConnection() {}

    public static Connection getConnection() {
        if ("SQLITE".equalsIgnoreCase(DB_TYPE)) {
            return getSQLiteConnection();
        }
        if ("SQLITE".equalsIgnoreCase(MYSQL_URL) || MYSQL_URL.contains("sqlite")) {
            return getSQLiteConnection();
        }
        return getMySQLConnection();
    }

    private static Connection getMySQLConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(MYSQL_URL, MYSQL_USER, MYSQL_PASSWORD);
        } catch (SQLException | ClassNotFoundException e) {
            throw new RuntimeException("Unable to obtain MySQL connection: " + e.getMessage(), e);
        }
    }

    private static Connection getSQLiteConnection() {
        try {
            Class.forName("org.sqlite.JDBC");
            return DriverManager.getConnection("jdbc:sqlite:" + SQLITE_FILE);
        } catch (SQLException | ClassNotFoundException e) {
            throw new RuntimeException("Unable to obtain SQLite connection: " + e.getMessage(), e);
        }
    }
}
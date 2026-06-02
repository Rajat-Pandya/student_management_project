package util;

/**
 * Simple global Session holder for the logged-in user.
 * Call Session.setUser(...) or Session.setFaculty(...) in LoginController after login.
 * Call Session.clear() on logout.
 */
public final class Session {
    private static int userId = -1;         // generic user id
    private static int facultyId = -1;      // faculty-specific id
    private static String username = null;
    private static String role = null;      // "admin", "faculty", "student"
    private static String department = null;
    private static String displayName = null;

    private Session() {} // prevent instantiation

    // --- Setters ---
    public static synchronized void setUser(int userIdVal, String usernameVal, String roleVal) {
        userId = userIdVal;
        username = usernameVal;
        role = roleVal;
    }

    public static synchronized void setFaculty(int facId, String usernameVal, String departmentVal) {
        facultyId = facId;
        username = usernameVal;
        department = departmentVal;
        role = "faculty";
    }

    public static synchronized void setDisplayName(String name) {
        displayName = name;
    }

    public static synchronized void clear() {
        userId = -1;
        facultyId = -1;
        username = null;
        role = null;
        department = null;
        displayName = null;
    }

    // --- Getters ---
    public static int getUserId() { return userId; }
    public static int getFacultyId() { return facultyId; }
    public static String getUsername() { return username; }
    public static String getRole() { return role; }
    public static String getDepartment() { return department; }
    public static String getDisplayName() { return displayName; }

    // --- Faculty-specific convenience (aliases) ---
    public static void setCurrentFacultyId(int id) {
        facultyId = id;
        role = "faculty";
    }

    public static int getCurrentFacultyId() {
        return facultyId;
    }
}

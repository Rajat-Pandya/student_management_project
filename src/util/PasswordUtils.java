package util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Simple password helpers (hashing, verify, cost detection).
 */
public class PasswordUtils {

    /** Hash a plaintext password with a given work factor (cost). */
    public static String hashPassword(String plain, int cost) {
        return BCrypt.hashpw(plain, BCrypt.gensalt(cost));
    }

    /** Verify a plaintext password against a stored bcrypt hash. */
    public static boolean verifyPassword(String plain, String storedHash) {
        if (plain == null || storedHash == null) return false;
        try {
            return BCrypt.checkpw(plain, storedHash);
        } catch (IllegalArgumentException e) {
            // Malformed storedHash
            return false;
        }
    }

    /** Quick check if the stored string looks like a bcrypt hash. */
    public static boolean looksLikeBcrypt(String stored) {
        if (stored == null) return false;
        return stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$");
    }

    /**
     * Extract cost factor from bcrypt hash string like "$2a$12$..." -> 12
     * Returns -1 on parse failure.
     */
    public static int extractCost(String bcryptHash) {
        if (!looksLikeBcrypt(bcryptHash)) return -1;
        try {
            String[] parts = bcryptHash.split("\\$");
            if (parts.length >= 3) {
                return Integer.parseInt(parts[2]);
            }
        } catch (Exception ignored) {}
        return -1;
    }

    /** Return true if the stored value is non-bcrypt (legacy) or has different cost. */
    public static boolean needsRehash(String storedHash, int desiredCost) {
        if (!looksLikeBcrypt(storedHash)) return true; // legacy plaintext -> needs rehash
        int current = extractCost(storedHash);
        return current <= 0 || current != desiredCost;
    }
}

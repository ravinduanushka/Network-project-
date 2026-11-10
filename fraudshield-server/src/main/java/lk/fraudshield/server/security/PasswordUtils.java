package lk.fraudshield.server.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Salted SHA-256 password hashing.
 *
 * <p>Each user gets a unique random salt, so identical passwords produce different hashes and
 * precomputed (rainbow) tables are useless. Plain-text passwords are never stored or logged.
 *
 * <p>Note: a single SHA-256 round is fast, which helps brute-force attackers. If the project
 * allows it, {@code PBKDF2WithHmacSHA256} (available in the JDK) with ~100k iterations is a
 * drop-in upgrade; the method signatures here would not need to change.
 */
public final class PasswordUtils {

    private static final int SALT_BYTES = 16;

    /** SecureRandom is thread-safe; one shared instance is fine. */
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtils() {
        // static utility class
    }

    /** @return a fresh 16-byte cryptographically random salt, Base64 encoded. */
    public static String generateSalt() {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * Computes SHA-256(salt || password).
     *
     * @param password plain-text password
     * @param salt     Base64 salt from {@link #generateSalt()}
     * @return Base64 encoded hash
     * @throws IllegalArgumentException if an argument is null or the salt is not valid Base64
     */
    public static String hashPassword(String password, String salt) {
        if (password == null || salt == null) {
            throw new IllegalArgumentException("password and salt must not be null");
        }
        byte[] saltBytes = Base64.getDecoder().decode(salt);
        byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);
        try {
            // MessageDigest is not thread-safe, so create one per call
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            sha256.update(saltBytes);
            return Base64.getEncoder().encodeToString(sha256.digest(passwordBytes));
        } catch (NoSuchAlgorithmException e) {
            // every Java platform is required to support SHA-256
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /**
     * Checks a login attempt against the stored credentials.
     *
     * <p>The comparison is constant-time so response timing does not reveal how many leading
     * characters of the hash matched.
     *
     * @return true only if the password matches; false for a mismatch or malformed input
     */
    public static boolean verifyPassword(String password, String storedHash, String storedSalt) {
        if (password == null || storedHash == null || storedSalt == null) {
            return false;
        }
        try {
            byte[] actual = hashPassword(password, storedSalt).getBytes(StandardCharsets.UTF_8);
            byte[] expected = storedHash.getBytes(StandardCharsets.UTF_8);
            return MessageDigest.isEqual(actual, expected);
        } catch (IllegalArgumentException e) {
            return false; // corrupt salt in the database
        }
    }
}

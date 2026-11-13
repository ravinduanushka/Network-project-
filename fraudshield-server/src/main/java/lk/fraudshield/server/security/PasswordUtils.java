package lk.fraudshield.server.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public final class PasswordUtils {

    private static final int SALT_BYTES = 16;

        private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtils() {
        // static utility class
    }

        public static String generateSalt() {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

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

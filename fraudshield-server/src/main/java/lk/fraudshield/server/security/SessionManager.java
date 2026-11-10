package lk.fraudshield.server.security;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory store of logged-in sessions, shared by all client-handler threads.
 * Tokens are 256-bit random values, so they cannot be guessed, and they expire after a fixed time.
 */
public class SessionManager {

    /** Who a token belongs to. */
    public record Session(String userId, String username, long expiresAtMillis) { }

    private static final long TTL_MILLIS = 30L * 60 * 1000; // 30 minutes
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ConcurrentMap<String, Session> sessions = new ConcurrentHashMap<>();

    /** Creates a session and returns its opaque token. */
    public String create(String userId, String username) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sessions.put(token, new Session(userId, username, System.currentTimeMillis() + TTL_MILLIS));
        return token;
    }

    /** @return the live session for the token, or null if missing, unknown or expired */
    public Session get(String token) {
        if (token == null) {
            return null;
        }
        Session s = sessions.get(token);
        if (s == null) {
            return null;
        }
        if (s.expiresAtMillis() < System.currentTimeMillis()) {
            sessions.remove(token);
            return null;
        }
        return s;
    }

    /** Ends a session (logout / disconnect). Safe to call with null or an unknown token. */
    public void invalidate(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }
}

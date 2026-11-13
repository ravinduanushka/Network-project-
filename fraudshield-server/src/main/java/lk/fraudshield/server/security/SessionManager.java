package lk.fraudshield.server.security;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class SessionManager {

        public record Session(String userId, String username, long expiresAtMillis) { }

    private static final long TTL_MILLIS = 30L * 60 * 1000; // 30 minutes
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ConcurrentMap<String, Session> sessions = new ConcurrentHashMap<>();

        public String create(String userId, String username) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sessions.put(token, new Session(userId, username, System.currentTimeMillis() + TTL_MILLIS));
        return token;
    }

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

        public void invalidate(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }
}

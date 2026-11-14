package lk.fraudshield.server.security;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public class RateLimiter {

    private static final int MAX_REQUESTS = 5;
    private static final long TIME_WINDOW_MS = 10_000L;
    private static final long BLOCK_DURATION_MS = 60_000L;

    private final ConcurrentHashMap<String, ConcurrentLinkedDeque<Long>> requestHistory = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> blockedIps = new ConcurrentHashMap<>();

    public boolean allowRequest(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            return true;
        }

        long now = System.currentTimeMillis();

        Long blockUntil = blockedIps.get(clientIp);
        if (blockUntil != null) {
            if (now < blockUntil) {
                return false;
            } else {
                blockedIps.remove(clientIp);
            }
        }

        ConcurrentLinkedDeque<Long> timestamps = requestHistory.computeIfAbsent(clientIp, k -> new ConcurrentLinkedDeque<>());

        long cutoff = now - TIME_WINDOW_MS;
        while (!timestamps.isEmpty() && timestamps.peekFirst() < cutoff) {
            timestamps.pollFirst();
        }

        if (timestamps.size() >= MAX_REQUESTS) {
            blockedIps.put(clientIp, now + BLOCK_DURATION_MS);
            timestamps.clear();
            return false;
        }

        timestamps.addLast(now);
        return true;
    }

    public long getRemainingBlockSeconds(String clientIp) {
        Long blockUntil = blockedIps.get(clientIp);
        if (blockUntil == null) {
            return 0;
        }
        long diff = blockUntil - System.currentTimeMillis();
        return diff > 0 ? (diff / 1000) : 0;
    }

    public void unblock(String clientIp) {
        if (clientIp != null) {
            blockedIps.remove(clientIp);
            requestHistory.remove(clientIp);
        }
    }

    public void clear() {
        blockedIps.clear();
        requestHistory.clear();
    }
}

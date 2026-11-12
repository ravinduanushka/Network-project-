package lk.fraudshield.fraud.profile;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.DoubleAdder;

/**
 * Thread-safe profile storing behavioral history for an account.
 */
public class UserBehaviourProfile {
    private final String accountNo;
    private final DoubleAdder totalAmount = new DoubleAdder();
    private final AtomicInteger transactionCount = new AtomicInteger(0);
    private final Set<String> knownIps = ConcurrentHashMap.newKeySet();
    private final ConcurrentLinkedDeque<Long> recentTxTimestamps = new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedDeque<Double> recentAmounts = new ConcurrentLinkedDeque<>();

    public UserBehaviourProfile(String accountNo) {
        this.accountNo = accountNo;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public void recordTransaction(double amount, String ip, long timestamp) {
        totalAmount.add(amount);
        transactionCount.incrementAndGet();
        if (ip != null && !ip.isBlank()) {
            knownIps.add(ip);
        }

        recentTxTimestamps.addLast(timestamp);
        // prune timestamps older than 10 minutes
        long cutoff = timestamp - (10 * 60 * 1000L);
        while (!recentTxTimestamps.isEmpty() && recentTxTimestamps.peekFirst() < cutoff) {
            recentTxTimestamps.pollFirst();
        }

        recentAmounts.addLast(amount);
        // keep only the last 10 amounts
        while (recentAmounts.size() > 10) {
            recentAmounts.pollFirst();
        }
    }

    public double getAverageTransfer() {
        int count = transactionCount.get();
        return count == 0 ? 0.0 : (totalAmount.sum() / count);
    }

    public int getTransactionCount() {
        return transactionCount.get();
    }

    public boolean isKnownIp(String ip) {
        if (ip == null || ip.isBlank() || knownIps.isEmpty()) {
            return true; // don't flag if profile has no IPs recorded yet
        }
        return knownIps.contains(ip);
    }

    public int getRecentTransactionCount(long windowMillis, long currentTimestamp) {
        long cutoff = currentTimestamp - windowMillis;
        int count = 0;
        for (Long ts : recentTxTimestamps) {
            if (ts >= cutoff) {
                count++;
            }
        }
        return count;
    }

    public ConcurrentLinkedDeque<Double> getRecentAmounts() {
        return recentAmounts;
    }

    public Set<String> getKnownIps() {
        return Collections.unmodifiableSet(knownIps);
    }
}

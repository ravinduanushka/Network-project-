package lk.fraudshield.server.fraud;

import lk.fraudshield.server.dao.TransactionDao;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rule-based fraud check run BEFORE any money moves. Rules are evaluated cheapest first and the
 * first hard failure wins:
 *
 * <ol>
 *   <li>Invalid amount, self-transfer or blacklisted destination -> BLOCKED</li>
 *   <li>Amount above {@value #MAX_AMOUNT}                         -> BLOCKED</li>
 *   <li>Velocity: already {@value #VELOCITY_LIMIT} transfers in the last minute -> BLOCKED</li>
 *   <li>Amount of {@value #REVIEW_AMOUNT} or more                  -> FLAGGED (needs verification)</li>
 *   <li>otherwise                                                  -> APPROVED</li>
 * </ol>
 *
 * Thread-safe: holds no per-request state; the blacklist is a concurrent set.
 */
public class FraudDetector {

    /** Hard limit: anything above is refused. */
    public static final double MAX_AMOUNT = 50_000.0;
    /** Soft limit: large but allowed amounts are held for verification. */
    public static final double REVIEW_AMOUNT = 10_000.0;
    /** Maximum transfers a sender may make per window; the next one is blocked. */
    public static final int VELOCITY_LIMIT = 3;
    public static final long WINDOW_MILLIS = 60_000L;

    private final TransactionDao transactionDao;
    private final Set<String> blacklist = ConcurrentHashMap.newKeySet();

    public FraudDetector(TransactionDao transactionDao) {
        this.transactionDao = transactionDao;
    }

    /** Adds an account number that may never receive transfers (e.g. reported mule account). */
    public void blacklist(String accountNo) {
        if (accountNo != null) {
            blacklist.add(accountNo);
        }
    }

    /**
     * @param fromAcc sender account (taken from the session, never from the client)
     * @param toAcc   destination account
     * @param amount  requested amount
     */
    public FraudResult evaluate(String fromAcc, String toAcc, double amount) {
        // Rule 3: self-transfer / blacklist (and malformed input, which is never legitimate)
        if (Double.isNaN(amount) || Double.isInfinite(amount) || amount <= 0) {
            return FraudResult.blocked(100, "Invalid amount");
        }
        if (toAcc == null || toAcc.isBlank() || toAcc.equals(fromAcc)) {
            return FraudResult.blocked(100, "Transfer to the same account is not allowed");
        }
        if (blacklist.contains(toAcc) || blacklist.contains(fromAcc)) {
            // deliberately vague: do not tell an attacker which accounts are listed
            return FraudResult.blocked(100, "Transfer not permitted");
        }

        // Rule 1: amount threshold
        if (amount > MAX_AMOUNT) {
            return FraudResult.blocked(90, "Amount exceeds the per-transfer limit of " + (long) MAX_AMOUNT);
        }

        // Rule 2: velocity. Counts ALL recent attempts by this sender (including rejected ones),
        // so hammering the server does not reset the clock.
        long since = System.currentTimeMillis() - WINDOW_MILLIS;
        int recent = transactionDao.countRecentTransactions(fromAcc, since);
        if (recent >= VELOCITY_LIMIT) {
            return FraudResult.blocked(80, "Too many transfers in a short time, please wait a minute");
        }

        // soft threshold: large transfers are held instead of executed
        if (amount >= REVIEW_AMOUNT) {
            return FraudResult.flagged(60, "Large transfer requires additional verification");
        }

        // approved: risk grows with size, 0..30
        return FraudResult.approved((int) (amount / REVIEW_AMOUNT * 30));
    }
}

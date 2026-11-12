package lk.fraudshield.fraud.rules;

import lk.fraudshield.common.fraud.TransactionContext;
import lk.fraudshield.fraud.profile.UserBehaviourProfile;

/**
 * Rule 3: VelocityRule
 * Triggers if more than 5 transfers occur within 2 minutes.
 * Points: +35
 */
public class VelocityRule implements FraudRule {
    private static final long TWO_MINUTES_MS = 2 * 60 * 1000L;

    @Override
    public String getName() {
        return "VELOCITY_EXCEEDED";
    }

    @Override
    public int getPoints() {
        return 35;
    }

    @Override
    public boolean evaluate(TransactionContext tx, UserBehaviourProfile profile) {
        if (profile == null) return false;
        long now = tx.timestamp() > 0 ? tx.timestamp() : System.currentTimeMillis();
        return profile.getRecentTransactionCount(TWO_MINUTES_MS, now) >= 5;
    }
}

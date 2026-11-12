package lk.fraudshield.fraud.rules;

import lk.fraudshield.common.fraud.TransactionContext;
import lk.fraudshield.fraud.profile.UserBehaviourProfile;

import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Rule 9: RoundAmountBurstRule
 * Triggers if multiple identical round amounts (e.g. 50,000, 50,000, 50,000) occur in succession.
 * Points: +15
 */
public class RoundAmountBurstRule implements FraudRule {
    @Override
    public String getName() {
        return "ROUND_AMOUNT_BURST";
    }

    @Override
    public int getPoints() {
        return 15;
    }

    @Override
    public boolean evaluate(TransactionContext tx, UserBehaviourProfile profile) {
        if (profile == null) return false;
        double amt = tx.amount();
        // check if round number (divisible by 10,000 and >= 10,000)
        if (amt >= 10_000.0 && amt % 10_000.0 == 0) {
            ConcurrentLinkedDeque<Double> recent = profile.getRecentAmounts();
            int identicalCount = 0;
            for (Double prev : recent) {
                if (Math.abs(prev - amt) < 0.01) {
                    identicalCount++;
                }
            }
            return identicalCount >= 2; // with the current one, this makes 3
        }
        return false;
    }
}

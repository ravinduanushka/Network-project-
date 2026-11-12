package lk.fraudshield.fraud.rules;

import lk.fraudshield.common.fraud.TransactionContext;
import lk.fraudshield.fraud.profile.UserBehaviourProfile;

/**
 * Rule 8: DrainAccountRule
 * Triggers if transfer amount exceeds 90% of current account balance.
 * Points: +20
 */
public class DrainAccountRule implements FraudRule {
    @Override
    public String getName() {
        return "DRAIN_ACCOUNT";
    }

    @Override
    public int getPoints() {
        return 20;
    }

    @Override
    public boolean evaluate(TransactionContext tx, UserBehaviourProfile profile) {
        if (tx.fromBalance() <= 0) return false;
        return tx.amount() > (0.90 * tx.fromBalance());
    }
}

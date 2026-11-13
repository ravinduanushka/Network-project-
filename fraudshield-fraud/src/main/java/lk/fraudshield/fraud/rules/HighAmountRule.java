package lk.fraudshield.fraud.rules;

import lk.fraudshield.common.fraud.TransactionContext;
import lk.fraudshield.fraud.profile.UserBehaviourProfile;

public class HighAmountRule implements FraudRule {
    private final double threshold;

    public HighAmountRule() {
        this(100_000.0);
    }

    public HighAmountRule(double threshold) {
        this.threshold = threshold;
    }

    @Override
    public String getName() {
        return "HIGH_AMOUNT";
    }

    @Override
    public int getPoints() {
        return 30;
    }

    @Override
    public boolean evaluate(TransactionContext tx, UserBehaviourProfile profile) {
        return tx.amount() > threshold;
    }
}

package lk.fraudshield.fraud.rules;

import lk.fraudshield.common.fraud.TransactionContext;
import lk.fraudshield.fraud.profile.UserBehaviourProfile;

/**
 * Rule 4: NewPayeeRule
 * Triggers if receiver is newly added or never paid before.
 * Points: +15
 */
public class NewPayeeRule implements FraudRule {
    @Override
    public String getName() {
        return "NEW_PAYEE";
    }

    @Override
    public int getPoints() {
        return 15;
    }

    @Override
    public boolean evaluate(TransactionContext tx, UserBehaviourProfile profile) {
        return tx.isNewPayee();
    }
}

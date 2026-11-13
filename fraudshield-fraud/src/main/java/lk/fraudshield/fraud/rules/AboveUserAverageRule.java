package lk.fraudshield.fraud.rules;

import lk.fraudshield.common.fraud.TransactionContext;
import lk.fraudshield.fraud.profile.UserBehaviourProfile;

public class AboveUserAverageRule implements FraudRule {
    @Override
    public String getName() {
        return "ABOVE_USER_AVG";
    }

    @Override
    public int getPoints() {
        return 25;
    }

    @Override
    public boolean evaluate(TransactionContext tx, UserBehaviourProfile profile) {
        if (profile == null || profile.getTransactionCount() < 2) {
            return false;
        }
        double avg = profile.getAverageTransfer();
        return avg > 0 && tx.amount() > (3.0 * avg);
    }
}

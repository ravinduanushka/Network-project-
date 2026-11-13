package lk.fraudshield.fraud.rules;

import lk.fraudshield.common.fraud.TransactionContext;
import lk.fraudshield.fraud.profile.UserBehaviourProfile;

public class FailedLoginBurstRule implements FraudRule {
    @Override
    public String getName() {
        return "FAILED_LOGIN_BURST";
    }

    @Override
    public int getPoints() {
        return 25;
    }

    @Override
    public boolean evaluate(TransactionContext tx, UserBehaviourProfile profile) {
        return tx.recentFailedLogins() >= 3;
    }
}

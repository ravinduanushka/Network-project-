package lk.fraudshield.fraud.rules;

import lk.fraudshield.common.fraud.TransactionContext;
import lk.fraudshield.fraud.profile.UserBehaviourProfile;

/**
 * Rule 7: FailedLoginBurstRule
 * Triggers if 3 or more failed logins occurred in the last 15 minutes before this transaction.
 * Points: +25
 */
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

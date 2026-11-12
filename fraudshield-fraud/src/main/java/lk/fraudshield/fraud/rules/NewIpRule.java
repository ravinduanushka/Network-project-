package lk.fraudshield.fraud.rules;

import lk.fraudshield.common.fraud.TransactionContext;
import lk.fraudshield.fraud.profile.UserBehaviourProfile;

/**
 * Rule 6: NewIpRule
 * Triggers if client IP differs from user's known history.
 * Points: +15
 */
public class NewIpRule implements FraudRule {
    @Override
    public String getName() {
        return "NEW_IP_ADDRESS";
    }

    @Override
    public int getPoints() {
        return 15;
    }

    @Override
    public boolean evaluate(TransactionContext tx, UserBehaviourProfile profile) {
        if (profile == null) return false;
        return !profile.isKnownIp(tx.clientIp());
    }
}

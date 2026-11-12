package lk.fraudshield.fraud.rules;

import lk.fraudshield.common.fraud.TransactionContext;
import lk.fraudshield.fraud.profile.UserBehaviourProfile;

public interface FraudRule {
    String getName();
    int getPoints();
    boolean evaluate(TransactionContext tx, UserBehaviourProfile profile);
}

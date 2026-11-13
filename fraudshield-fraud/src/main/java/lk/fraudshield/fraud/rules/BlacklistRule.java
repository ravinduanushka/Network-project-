package lk.fraudshield.fraud.rules;

import lk.fraudshield.common.fraud.TransactionContext;
import lk.fraudshield.fraud.profile.UserBehaviourProfile;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class BlacklistRule implements FraudRule {
    private final Set<String> blacklistedAccounts = ConcurrentHashMap.newKeySet();

    public BlacklistRule() {
        // seed with default demo blacklisted account
        blacklistedAccounts.add("ACC9999");
        blacklistedAccounts.add("MULE_ACCOUNT_1");
    }

    public void addBlacklist(String accountNo) {
        if (accountNo != null) blacklistedAccounts.add(accountNo.trim().toUpperCase());
    }

    public void removeBlacklist(String accountNo) {
        if (accountNo != null) blacklistedAccounts.remove(accountNo.trim().toUpperCase());
    }

    @Override
    public String getName() {
        return "BLACKLISTED_RECEIVER";
    }

    @Override
    public int getPoints() {
        return 100;
    }

    @Override
    public boolean evaluate(TransactionContext tx, UserBehaviourProfile profile) {
        if (tx.toAcc() == null) return false;
        return blacklistedAccounts.contains(tx.toAcc().trim().toUpperCase());
    }
}

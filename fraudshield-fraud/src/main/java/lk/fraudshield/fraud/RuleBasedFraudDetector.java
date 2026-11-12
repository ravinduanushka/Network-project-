package lk.fraudshield.fraud;

import lk.fraudshield.common.fraud.Decision;
import lk.fraudshield.common.fraud.FraudDetector;
import lk.fraudshield.common.fraud.FraudResult;
import lk.fraudshield.common.fraud.TransactionContext;
import lk.fraudshield.fraud.profile.ProfileStore;
import lk.fraudshield.fraud.profile.UserBehaviourProfile;
import lk.fraudshield.fraud.rules.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Core Rule-Based Fraud Detection Engine implementing FraudDetector.
 * Thread-safe and pluggable into BankServer.
 */
public class RuleBasedFraudDetector implements FraudDetector {

    private final List<FraudRule> rules = new ArrayList<>();
    private final ProfileStore profileStore;
    private final BlacklistRule blacklistRule;

    public RuleBasedFraudDetector() {
        this(new ProfileStore());
    }

    public RuleBasedFraudDetector(ProfileStore profileStore) {
        this.profileStore = profileStore;

        // Register the 10 detection rules
        this.blacklistRule = new BlacklistRule();
        rules.add(new HighAmountRule());
        rules.add(new AboveUserAverageRule());
        rules.add(new VelocityRule());
        rules.add(new NewPayeeRule());
        rules.add(new OddHourRule());
        rules.add(new NewIpRule());
        rules.add(new FailedLoginBurstRule());
        rules.add(new DrainAccountRule());
        rules.add(new RoundAmountBurstRule());
        rules.add(blacklistRule);
    }

    public BlacklistRule getBlacklistRule() {
        return blacklistRule;
    }

    public ProfileStore getProfileStore() {
        return profileStore;
    }

    @Override
    public FraudResult evaluate(TransactionContext tx) {
        if (tx == null) {
            return new FraudResult(0, Decision.ALLOW, Collections.emptyList());
        }

        UserBehaviourProfile profile = profileStore.getProfile(tx.fromAcc());
        int totalScore = 0;
        List<String> reasons = new ArrayList<>();

        for (FraudRule rule : rules) {
            try {
                if (rule.evaluate(tx, profile)) {
                    totalScore += rule.getPoints();
                    reasons.add(rule.getName());
                }
            } catch (Exception e) {
                // Safeguard against individual rule exceptions
                System.err.println("Error evaluating rule " + rule.getName() + ": " + e.getMessage());
            }
        }

        // Cap total score at 100
        int finalScore = Math.min(totalScore, 100);

        // Map score to Decision according to specification
        Decision decision;
        if (finalScore < 30) {
            decision = Decision.ALLOW;
        } else if (finalScore < 60) {
            decision = Decision.OTP;
        } else if (finalScore < 85) {
            decision = Decision.HOLD;
        } else {
            decision = Decision.BLOCK;
        }

        return new FraudResult(finalScore, decision, reasons);
    }

    @Override
    public void onTransactionCompleted(TransactionContext tx) {
        if (tx == null || tx.fromAcc() == null) return;
        UserBehaviourProfile profile = profileStore.getProfile(tx.fromAcc());
        if (profile != null) {
            long ts = tx.timestamp() > 0 ? tx.timestamp() : System.currentTimeMillis();
            profile.recordTransaction(tx.amount(), tx.clientIp(), ts);
        }
    }
}

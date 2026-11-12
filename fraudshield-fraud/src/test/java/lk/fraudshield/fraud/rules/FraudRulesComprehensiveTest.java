package lk.fraudshield.fraud.rules;

import lk.fraudshield.common.fraud.TransactionContext;
import lk.fraudshield.fraud.profile.UserBehaviourProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class FraudRulesComprehensiveTest {

    private UserBehaviourProfile profile;
    private long now;

    @BeforeEach
    void setUp() {
        profile = new UserBehaviourProfile("ACC1001");
        now = System.currentTimeMillis();
    }

    @Test
    void testHighAmountRule() {
        HighAmountRule rule = new HighAmountRule(100_000.0);
        TransactionContext normal = createContext(50_000.0, 100_000.0, "192.168.1.1", false, 0, now);
        TransactionContext high = createContext(150_000.0, 300_000.0, "192.168.1.1", false, 0, now);

        assertFalse(rule.evaluate(normal, profile), "Amount under threshold should not fire");
        assertTrue(rule.evaluate(high, profile), "Amount over threshold must fire");
    }

    @Test
    void testVelocityRule() {
        VelocityRule rule = new VelocityRule();
        // Record 5 transactions in the last 60 seconds
        for (int i = 0; i < 5; i++) {
            profile.recordTransaction(1000.0, "192.168.1.1", now - (i * 10_000L));
        }

        TransactionContext sixthTx = createContext(2000.0, 50_000.0, "192.168.1.1", false, 0, now);
        assertTrue(rule.evaluate(sixthTx, profile), "Velocity rule should trigger on 5+ recent transactions");
    }

    @Test
    void testAboveUserAverageRule() {
        AboveUserAverageRule rule = new AboveUserAverageRule();
        // User typically transfers Rs. 5,000
        profile.recordTransaction(5000.0, "192.168.1.1", now - 20000);
        profile.recordTransaction(5000.0, "192.168.1.1", now - 10000);

        TransactionContext normal = createContext(10_000.0, 50_000.0, "192.168.1.1", false, 0, now);
        TransactionContext spike = createContext(25_000.0, 50_000.0, "192.168.1.1", false, 0, now); // > 3x average

        assertFalse(rule.evaluate(normal, profile));
        assertTrue(rule.evaluate(spike, profile), "Spike over 3x average must fire");
    }

    @Test
    void testNewPayeeRule() {
        NewPayeeRule rule = new NewPayeeRule();
        TransactionContext known = createContext(1000.0, 10_000.0, "192.168.1.1", false, 0, now);
        TransactionContext newPayee = createContext(1000.0, 10_000.0, "192.168.1.1", true, 0, now);

        assertFalse(rule.evaluate(known, profile));
        assertTrue(rule.evaluate(newPayee, profile));
    }

    @Test
    void testNewIpRule() {
        NewIpRule rule = new NewIpRule();
        profile.recordTransaction(2000.0, "192.168.1.50", now - 10000);

        TransactionContext sameIp = createContext(1000.0, 10_000.0, "192.168.1.50", false, 0, now);
        TransactionContext diffIp = createContext(1000.0, 10_000.0, "10.0.0.99", false, 0, now);

        assertFalse(rule.evaluate(sameIp, profile));
        assertTrue(rule.evaluate(diffIp, profile), "Unseen IP address should trigger rule");
    }

    @Test
    void testFailedLoginBurstRule() {
        FailedLoginBurstRule rule = new FailedLoginBurstRule();
        TransactionContext safe = createContext(1000.0, 10_000.0, "192.168.1.1", false, 1, now);
        TransactionContext burst = createContext(1000.0, 10_000.0, "192.168.1.1", false, 3, now);

        assertFalse(rule.evaluate(safe, profile));
        assertTrue(rule.evaluate(burst, profile), "3 or more failed logins must trigger rule");
    }

    @Test
    void testDrainAccountRule() {
        DrainAccountRule rule = new DrainAccountRule();
        // Balance 100,000. 95,000 is 95% (> 90%)
        TransactionContext normal = createContext(50_000.0, 100_000.0, "192.168.1.1", false, 0, now);
        TransactionContext drain = createContext(95_000.0, 100_000.0, "192.168.1.1", false, 0, now);

        assertFalse(rule.evaluate(normal, profile));
        assertTrue(rule.evaluate(drain, profile), "Transfer > 90% of balance must trigger drain rule");
    }

    @Test
    void testRoundAmountBurstRule() {
        RoundAmountBurstRule rule = new RoundAmountBurstRule();
        profile.recordTransaction(50_000.0, "192.168.1.1", now - 20000);
        profile.recordTransaction(50_000.0, "192.168.1.1", now - 10000);

        TransactionContext thirdRound = createContext(50_000.0, 200_000.0, "192.168.1.1", false, 0, now);
        assertTrue(rule.evaluate(thirdRound, profile), "3 consecutive identical round amounts should fire");
    }

    @Test
    void testBlacklistRule() {
        BlacklistRule rule = new BlacklistRule();
        TransactionContext normal = new TransactionContext(
                UUID.randomUUID().toString(), "ACC1001", "ACC2002",
                1000.0, 50_000.0, "192.168.1.1", now, false, 0
        );
        TransactionContext mule = new TransactionContext(
                UUID.randomUUID().toString(), "ACC1001", "ACC9999",
                1000.0, 50_000.0, "192.168.1.1", now, false, 0
        );

        assertFalse(rule.evaluate(normal, profile));
        assertTrue(rule.evaluate(mule, profile), "Destination on blacklist must trigger rule");
    }

    private TransactionContext createContext(double amount, double balance, String ip, boolean isNewPayee, int failedLogins, long timestamp) {
        return new TransactionContext(
                UUID.randomUUID().toString(), "ACC1001", "ACC2002",
                amount, balance, ip, timestamp, isNewPayee, failedLogins
        );
    }
}

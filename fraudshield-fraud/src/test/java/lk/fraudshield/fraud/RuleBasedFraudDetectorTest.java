package lk.fraudshield.fraud;

import lk.fraudshield.common.fraud.Decision;
import lk.fraudshield.common.fraud.FraudResult;
import lk.fraudshield.common.fraud.TransactionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class RuleBasedFraudDetectorTest {

    private RuleBasedFraudDetector detector;

    @BeforeEach
    void setUp() {
        detector = new RuleBasedFraudDetector();
    }

    @Test
    void testNormalTransactionAllows() {
        TransactionContext tx = new TransactionContext(
                UUID.randomUUID().toString(), "ACC100", "ACC200",
                5000.0, 50000.0, "192.168.1.5", System.currentTimeMillis(),
                false, 0
        );

        FraudResult res = detector.evaluate(tx);
        assertEquals(Decision.ALLOW, res.decision());
        assertTrue(res.riskScore() < 30);
    }

    @Test
    void testHighAmountTriggersHoldOrOtp() {
        TransactionContext tx = new TransactionContext(
                UUID.randomUUID().toString(), "ACC100", "ACC200",
                120000.0, 200000.0, "192.168.1.5", System.currentTimeMillis(),
                false, 0
        );

        FraudResult res = detector.evaluate(tx);
        assertTrue(res.riskScore() >= 30);
        assertTrue(res.reasons().contains("HIGH_AMOUNT"));
    }

    @Test
    void testBlacklistedAccountBlocksImmediately() {
        TransactionContext tx = new TransactionContext(
                UUID.randomUUID().toString(), "ACC100", "ACC9999",
                500.0, 10000.0, "192.168.1.5", System.currentTimeMillis(),
                false, 0
        );

        FraudResult res = detector.evaluate(tx);
        assertEquals(Decision.BLOCK, res.decision());
        assertEquals(100, res.riskScore());
        assertTrue(res.reasons().contains("BLACKLISTED_RECEIVER"));
    }

    @Test
    void testDrainAccountRuleTriggers() {
        TransactionContext tx = new TransactionContext(
                UUID.randomUUID().toString(), "ACC100", "ACC200",
                95000.0, 100000.0, "192.168.1.5", System.currentTimeMillis(),
                false, 0
        );

        FraudResult res = detector.evaluate(tx);
        assertTrue(res.reasons().contains("DRAIN_ACCOUNT"));
    }
}

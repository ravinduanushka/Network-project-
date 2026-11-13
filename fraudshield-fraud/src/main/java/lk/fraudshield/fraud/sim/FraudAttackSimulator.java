package lk.fraudshield.fraud.sim;

import lk.fraudshield.common.fraud.Decision;
import lk.fraudshield.common.fraud.FraudResult;
import lk.fraudshield.common.fraud.TransactionContext;
import lk.fraudshield.fraud.RuleBasedFraudDetector;

import java.util.UUID;

public class FraudAttackSimulator {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println(" FraudShield - Demo Fraud Attack Simulator (M2)   ");
        System.out.println("==================================================\n");

        RuleBasedFraudDetector detector = new RuleBasedFraudDetector();
        long now = System.currentTimeMillis();

        // Scenario 1: Normal everyday transfer
        System.out.println("[Scenario 1] Normal Transfer (Rs. 5,000 to known payee)");
        TransactionContext tx1 = new TransactionContext(
                UUID.randomUUID().toString(), "ACC1001", "ACC1002",
                5000.0, 50000.0, "192.168.1.10", now, false, 0
        );
        printResult(detector.evaluate(tx1));
        detector.onTransactionCompleted(tx1);

        // Scenario 2: New payee transfer with medium amount (OTP Step-up)
        System.out.println("[Scenario 2] New Payee + Failed Login Attempt (Rs. 45,000)");
        TransactionContext tx2 = new TransactionContext(
                UUID.randomUUID().toString(), "ACC1001", "ACC2005",
                45000.0, 45000.0, "192.168.1.10", now + 1000, true, 3
        );
        printResult(detector.evaluate(tx2));

        // Scenario 3: High Amount above LKR 100,000 (HOLD)
        System.out.println("[Scenario 3] High Value Transfer (Rs. 150,000)");
        TransactionContext tx3 = new TransactionContext(
                UUID.randomUUID().toString(), "ACC1001", "ACC1003",
                150000.0, 300000.0, "192.168.1.10", now + 2000, false, 0
        );
        printResult(detector.evaluate(tx3));

        // Scenario 4: Account Drain Attempt (> 90% balance)
        System.out.println("[Scenario 4] Account Drain (Rs. 95,000 out of Rs. 100,000)");
        TransactionContext tx4 = new TransactionContext(
                UUID.randomUUID().toString(), "ACC1001", "ACC1004",
                95000.0, 100000.0, "192.168.1.10", now + 3000, true, 0
        );
        printResult(detector.evaluate(tx4));

        // Scenario 5: Transfer to Blacklisted Mule Account (BLOCK)
        System.out.println("[Scenario 5] Blacklisted Mule Account Transfer");
        TransactionContext tx5 = new TransactionContext(
                UUID.randomUUID().toString(), "ACC1001", "ACC9999",
                10000.0, 50000.0, "192.168.1.10", now + 4000, false, 0
        );
        printResult(detector.evaluate(tx5));

        System.out.println("==================================================");
        System.out.println(" Simulation Complete. All rules evaluated successfully.");
        System.out.println("==================================================");
    }

    private static void printResult(FraudResult res) {
        System.out.println(" -> Score: " + res.riskScore() + "/100");
        System.out.println(" -> Decision: " + res.decision());
        System.out.println(" -> Triggered Rules: " + (res.reasons().isEmpty() ? "None (Safe)" : String.join(", ", res.reasons())));
        System.out.println();
    }
}

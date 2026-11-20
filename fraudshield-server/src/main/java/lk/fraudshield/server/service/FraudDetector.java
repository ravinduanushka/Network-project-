package lk.fraudshield.server.service;

public class FraudDetector {

    public static final String APPROVED = "APPROVED";
    public static final String FLAGGED = "FLAGGED";
    public static final String BLOCKED = "BLOCKED";

        public static final double HIGH_AMOUNT_LIMIT = 200_000.0;

        public record FraudResult(boolean approved, String status, String reason) {

                public int riskScore() {
            return switch (status) {
                case FLAGGED -> 70;
                case BLOCKED -> 100;
                default -> 0;
            };
        }
    }

        public FraudResult evaluateTransfer(String senderUsername, String targetAccount, double amount) {
        // Rule 3 (checked first): invalid amount
        if (Double.isNaN(amount) || Double.isInfinite(amount) || amount <= 0) {
            return new FraudResult(false, BLOCKED, "Invalid amount");
        }

        // Rule 1: self-transfer
        if (targetAccount == null || targetAccount.isBlank()
                || (senderUsername != null && senderUsername.equalsIgnoreCase(targetAccount.trim()))) {
            return new FraudResult(false, BLOCKED, "Transfer to yourself is not allowed");
        }

        // Rule 2: high amount - more than 200,000 requires Admin Approval
        if (amount > HIGH_AMOUNT_LIMIT) {
            return new FraudResult(false, FLAGGED,
                    "Amount exceeds LKR 200,000 and requires Admin Approval");
        }

        return new FraudResult(true, APPROVED, "Approved");
    }
}

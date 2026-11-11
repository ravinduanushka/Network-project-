package lk.fraudshield.server.service;

/**
 * Rule-based fraud check run BEFORE any money moves. Stateless and thread-safe.
 *
 * <p>Rules, in order (first match wins):
 * <ol>
 *   <li>Invalid amount (zero, negative, NaN, infinite)   -> BLOCKED</li>
 *   <li>Self-transfer (sender equals target, ignoring case) -> BLOCKED</li>
 *   <li>Amount above {@value #HIGH_AMOUNT_LIMIT}          -> FLAGGED</li>
 *   <li>otherwise                                         -> APPROVED</li>
 * </ol>
 * The invalid-amount rule runs first so the later rules never see a meaningless number.
 */
public class FraudDetector {

    public static final String APPROVED = "APPROVED";
    public static final String FLAGGED = "FLAGGED";
    public static final String BLOCKED = "BLOCKED";

    /** Transfers above this amount are held for review. */
    public static final double HIGH_AMOUNT_LIMIT = 50_000.0;

    /** Outcome of an evaluation. */
    public record FraudResult(boolean approved, String status, String reason) {

        /** Score stored in {@code transactions.risk_score}: 0 approved, 70 flagged, 100 blocked. */
        public int riskScore() {
            return switch (status) {
                case FLAGGED -> 70;
                case BLOCKED -> 100;
                default -> 0;
            };
        }
    }

    /**
     * @param senderUsername the authenticated user making the transfer
     * @param targetAccount  destination as supplied by the client
     * @param amount         requested amount
     */
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

        // Rule 2: high amount
        if (amount > HIGH_AMOUNT_LIMIT) {
            return new FraudResult(false, FLAGGED,
                    "Amount exceeds " + (long) HIGH_AMOUNT_LIMIT + " and needs manual review");
        }

        return new FraudResult(true, APPROVED, "Approved");
    }
}

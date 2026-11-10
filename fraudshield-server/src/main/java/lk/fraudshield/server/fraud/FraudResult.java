package lk.fraudshield.server.fraud;

/**
 * Outcome of a fraud evaluation.
 *
 * @param status    what the server should do with the transfer
 * @param riskScore 0 (safe) to 100 (certain fraud); stored in {@code transactions.risk_score}
 * @param reason    short human-readable explanation, safe to show to the client
 */
public record FraudResult(Status status, int riskScore, String reason) {

    public enum Status {
        /** Safe: execute the transfer. */
        APPROVED,
        /** Suspicious: do NOT execute; hold for extra verification (e.g. OTP). */
        FLAGGED,
        /** Rejected outright by a hard rule. */
        BLOCKED
    }

    public static FraudResult approved(int riskScore) {
        return new FraudResult(Status.APPROVED, riskScore, "Approved");
    }

    public static FraudResult flagged(int riskScore, String reason) {
        return new FraudResult(Status.FLAGGED, riskScore, reason);
    }

    public static FraudResult blocked(int riskScore, String reason) {
        return new FraudResult(Status.BLOCKED, riskScore, reason);
    }

    public boolean isApproved() {
        return status == Status.APPROVED;
    }
}

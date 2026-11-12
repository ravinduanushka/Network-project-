package lk.fraudshield.common.fraud;

public record TransactionContext(
    String txId,
    String fromAcc,
    String toAcc,
    double amount,
    double fromBalance,
    String clientIp,
    long timestamp,
    boolean isNewPayee,
    int recentFailedLogins
) {}

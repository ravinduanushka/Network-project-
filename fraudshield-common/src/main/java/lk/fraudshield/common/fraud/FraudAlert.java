package lk.fraudshield.common.fraud;

import java.util.List;

public record FraudAlert(
    String txId,
    String account,
    int score,
    Decision decision,
    List<String> reasons,
    long ts
) {}

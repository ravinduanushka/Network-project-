package lk.fraudshield.common.fraud;

import java.util.List;

public record FraudResult(
    int riskScore,
    Decision decision,
    List<String> reasons
) {}

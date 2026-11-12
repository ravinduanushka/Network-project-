package lk.fraudshield.common.fraud;

public interface FraudDetector {
    FraudResult evaluate(TransactionContext tx);
    void onTransactionCompleted(TransactionContext tx);
}

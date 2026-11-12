package lk.fraudshield.common.fraud;

public interface AlertSink {
    void publish(FraudAlert alert);
}

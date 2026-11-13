package lk.fraudshield.fraud.rules;

import lk.fraudshield.common.fraud.TransactionContext;
import lk.fraudshield.fraud.profile.UserBehaviourProfile;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;

public class OddHourRule implements FraudRule {
    @Override
    public String getName() {
        return "ODD_HOUR";
    }

    @Override
    public int getPoints() {
        return 10;
    }

    @Override
    public boolean evaluate(TransactionContext tx, UserBehaviourProfile profile) {
        long ts = tx.timestamp() > 0 ? tx.timestamp() : System.currentTimeMillis();
        LocalTime time = Instant.ofEpochMilli(ts)
                .atZone(ZoneId.systemDefault())
                .toLocalTime();
        int hour = time.getHour();
        return hour >= 0 && hour < 5;
    }
}

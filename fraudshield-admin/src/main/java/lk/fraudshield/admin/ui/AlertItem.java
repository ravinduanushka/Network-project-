package lk.fraudshield.admin.ui;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;

public class AlertItem {
    private final SimpleStringProperty txId;
    private final SimpleStringProperty account;
    private final SimpleIntegerProperty score;
    private final SimpleStringProperty decision;
    private final SimpleStringProperty reasons;
    private final SimpleLongProperty timestamp;

    public AlertItem(String txId, String account, int score, String decision, String reasons, long timestamp) {
        this.txId = new SimpleStringProperty(txId);
        this.account = new SimpleStringProperty(account);
        this.score = new SimpleIntegerProperty(score);
        this.decision = new SimpleStringProperty(decision);
        this.reasons = new SimpleStringProperty(reasons);
        this.timestamp = new SimpleLongProperty(timestamp);
    }

    public String getTxId() { return txId.get(); }
    public String getAccount() { return account.get(); }
    public int getScore() { return score.get(); }
    public String getDecision() { return decision.get(); }
    public String getReasons() { return reasons.get(); }
    public long getTimestamp() { return timestamp.get(); }
}
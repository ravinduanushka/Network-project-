package lk.fraudshield.admin.ui;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;

public class AlertItem {
    private final SimpleStringProperty txId;
    private final SimpleStringProperty account;
    private final SimpleStringProperty recipient;
    private final SimpleStringProperty amount;
    private final SimpleIntegerProperty score;
    private final SimpleStringProperty decision;
    private final SimpleStringProperty reasons;
    private final SimpleLongProperty timestamp;

    public AlertItem(String txId, String account, int score, String decision, String reasons, long timestamp) {
        this(txId, account, "-", "-", score, decision, reasons, timestamp);
    }

    public AlertItem(String txId, String account, String recipient, String amount, int score, String decision, String reasons, long timestamp) {
        this.txId = new SimpleStringProperty(txId);
        this.account = new SimpleStringProperty(account);
        this.recipient = new SimpleStringProperty(recipient != null ? recipient : "-");
        this.amount = new SimpleStringProperty(amount != null ? amount : "-");
        this.score = new SimpleIntegerProperty(score);
        this.decision = new SimpleStringProperty(decision);
        this.reasons = new SimpleStringProperty(reasons);
        this.timestamp = new SimpleLongProperty(timestamp);
    }

    public String getTxId() { return txId.get(); }
    public String getAccount() { return account.get(); }
    public String getRecipient() { return recipient.get(); }
    public String getAmount() { return amount.get(); }
    public int getScore() { return score.get(); }
    public String getDecision() { return decision.get(); }
    public void setDecision(String newDecision) { this.decision.set(newDecision); }
    public String getReasons() { return reasons.get(); }
    public long getTimestamp() { return timestamp.get(); }
}

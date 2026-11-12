package lk.fraudshield.client.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class TransactionRecord {

    private final StringProperty timestamp;
    private final StringProperty recipient;
    private final StringProperty amount;
    private final StringProperty status;

    public TransactionRecord(String timestamp, String recipient, String amount, String status) {
        this.timestamp = new SimpleStringProperty(timestamp);
        this.recipient = new SimpleStringProperty(recipient);
        this.amount = new SimpleStringProperty(amount);
        this.status = new SimpleStringProperty(status);
    }

    public String getTimestamp() {
        return timestamp.get();
    }

    public StringProperty timestampProperty() {
        return timestamp;
    }

    public String getRecipient() {
        return recipient.get();
    }

    public StringProperty recipientProperty() {
        return recipient;
    }

    public String getAmount() {
        return amount.get();
    }

    public StringProperty amountProperty() {
        return amount;
    }

    public String getStatus() {
        return status.get();
    }

    public StringProperty statusProperty() {
        return status;
    }
}
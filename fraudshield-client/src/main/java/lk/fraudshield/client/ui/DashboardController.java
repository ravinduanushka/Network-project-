package lk.fraudshield.client.ui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class DashboardController {

    // Header & Account Overview Elements
    @FXML
    private Label balanceLabel;

    @FXML
    private Label accountNumberLabel;

    @FXML
    private Label engineStatusLabel;

    @FXML
    private TableView<?> transactionsTable;

    // Fund Transfer Tab Elements
    @FXML
    private TextField recipientAccountField;

    @FXML
    private TextField transferAmountField;

    @FXML
    private TextField referenceNoteField;

    @FXML
    private Label transferStatusLabel;

    private double currentBalance = 250000.00;

    @FXML
    public void initialize() {
        updateBalanceDisplay();
    }

    private void updateBalanceDisplay() {
        if (balanceLabel != null) {
            balanceLabel.setText(String.format("LKR %,.2f", currentBalance));
        }
    }

    @FXML
    private void handleFundTransfer() {
        String recipient = recipientAccountField.getText().trim();
        String amountText = transferAmountField.getText().trim();
        String note = referenceNoteField.getText().trim();

        if (recipient.isEmpty() || amountText.isEmpty()) {
            transferStatusLabel.setStyle("-fx-text-fill: #ef4444;");
            transferStatusLabel.setText("Error: Recipient account and amount are required.");
            return;
        }

        try {
            double amount = Double.parseDouble(amountText);

            if (amount <= 0) {
                transferStatusLabel.setStyle("-fx-text-fill: #ef4444;");
                transferStatusLabel.setText("Error: Amount must be greater than zero.");
                return;
            }

            if (amount > currentBalance) {
                transferStatusLabel.setStyle("-fx-text-fill: #ef4444;");
                transferStatusLabel.setText("Error: Insufficient account balance.");
                return;
            }

            // High-value transaction anomaly check (FraudShield trigger simulation)
            if (amount >= 100000.00) {
                transferStatusLabel.setStyle("-fx-text-fill: #f59e0b;");
                transferStatusLabel.setText("OTP Verification Required: High-value transfer flagged by FraudShield.");
            } else {
                currentBalance -= amount;
                updateBalanceDisplay();

                transferStatusLabel.setStyle("-fx-text-fill: #22c55e;");
                transferStatusLabel.setText("Success: LKR " + String.format("%,.2f", amount) + " transferred to " + recipient);

                recipientAccountField.clear();
                transferAmountField.clear();
                referenceNoteField.clear();
            }

        } catch (NumberFormatException e) {
            transferStatusLabel.setStyle("-fx-text-fill: #ef4444;");
            transferStatusLabel.setText("Error: Invalid numeric amount.");
        }
    }

    @FXML
    private void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) balanceLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("FraudShield - Customer Client");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
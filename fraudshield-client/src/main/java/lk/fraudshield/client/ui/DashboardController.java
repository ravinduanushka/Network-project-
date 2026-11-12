package lk.fraudshield.client.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

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

    // Fraud Alerts (UDP Live) Tab Elements
    @FXML
    private ListView<String> fraudAlertsListView;

    private double currentBalance = 250000.00;
    private volatile boolean listeningUdp = true;
    private DatagramSocket udpSocket;

    @FXML
    public void initialize() {
        updateBalanceDisplay();
        startUdpAlertListener();
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

    private void startUdpAlertListener() {
        Thread udpThread = new Thread(() -> {
            try {
                udpSocket = new DatagramSocket(6000);
                byte[] buffer = new byte[1024];

                while (listeningUdp) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    udpSocket.receive(packet);

                    String message = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                    String timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

                    Platform.runLater(() -> {
                        if (fraudAlertsListView != null) {
                            fraudAlertsListView.getItems().add(0, "[" + timestamp + "] 🚨 UDP ALERT: " + message);
                        }
                    });
                }
            } catch (Exception e) {
                if (listeningUdp) {
                    Platform.runLater(() -> {
                        if (fraudAlertsListView != null) {
                            fraudAlertsListView.getItems().add("[SYSTEM] UDP Listener standby on Port 6000 (" + e.getMessage() + ")");
                        }
                    });
                }
            }
        });
        udpThread.setDaemon(true);
        udpThread.start();
    }

    @FXML
    private void handleSimulateAlert() {
        String timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        if (fraudAlertsListView != null) {
            fraudAlertsListView.getItems().add(0, "[" + timestamp + "] ⚠️ HIGH RISK: Unusual transfer velocity detected on ACC-88910 (Score: 0.94)");
        }
    }

    @FXML
    private void handleLogout() {
        listeningUdp = false;
        if (udpSocket != null && !udpSocket.isClosed()) {
            udpSocket.close();
        }

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
package lk.fraudshield.client.ui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import lk.fraudshield.client.model.TransactionRecord;
import lk.fraudshield.client.net.BankClientService;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class DashboardController {

    @FXML
    private Label balanceLabel;

    @FXML
    private Label accountNumberLabel;

    @FXML
    private Label engineStatusLabel;

    @FXML
    private TableView<TransactionRecord> transactionsTable;

    @FXML
    private TableColumn<TransactionRecord, String> colTimestamp;

    @FXML
    private TableColumn<TransactionRecord, String> colRecipient;

    @FXML
    private TableColumn<TransactionRecord, String> colAmount;

    @FXML
    private TableColumn<TransactionRecord, String> colStatus;

    @FXML
    private TextField recipientAccountField;

    @FXML
    private TextField transferAmountField;

    @FXML
    private TextField referenceNoteField;

    @FXML
    private Label transferStatusLabel;

    @FXML
    private ListView<String> fraudAlertsListView;

    private final ObservableList<TransactionRecord> transactionList = FXCollections.observableArrayList();
    private double currentBalance = 250000.00;
    private volatile boolean listeningUdp = true;
    private DatagramSocket udpSocket;

    @FXML
    public void initialize() {
        updateBalanceDisplay();
        setupTransactionsTable();
        startUdpAlertListener();

        if (BankClientService.getInstance().getCurrentUsername() != null && accountNumberLabel != null) {
            accountNumberLabel.setText("Account: " + BankClientService.getInstance().getAccountNumber() + " (" + BankClientService.getInstance().getCurrentUsername() + ")");
        }
    }

    private void setupTransactionsTable() {
        if (transactionsTable != null) {
            colTimestamp.setCellValueFactory(data -> data.getValue().timestampProperty());
            colRecipient.setCellValueFactory(data -> data.getValue().recipientProperty());
            colAmount.setCellValueFactory(data -> data.getValue().amountProperty());
            colStatus.setCellValueFactory(data -> data.getValue().statusProperty());

            String initialTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            transactionList.add(new TransactionRecord(initialTime, "INITIAL-DEPOSIT", "250,000.00", "Cleared"));
            transactionsTable.setItems(transactionList);
        }
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
                boolean verified = showOtpVerificationDialog(amount, recipient);
                if (!verified) {
                    transferStatusLabel.setStyle("-fx-text-fill: #ef4444;");
                    transferStatusLabel.setText("Transfer Cancelled: OTP verification failed or aborted.");
                    return;
                }
                executeTransfer(recipient, amount, "Verified (OTP)");
            } else {
                executeTransfer(recipient, amount, "Success (Standard)");
            }

        } catch (NumberFormatException e) {
            transferStatusLabel.setStyle("-fx-text-fill: #ef4444;");
            transferStatusLabel.setText("Error: Invalid numeric amount.");
        }
    }

    private boolean showOtpVerificationDialog(double amount, String recipient) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("FraudShield Step-Up Authentication");
        dialog.setHeaderText("High-Value Transfer Flagged (LKR " + String.format("%,.2f", amount) + ")");
        dialog.setContentText("Enter 6-digit OTP sent to registered device (Demo OTP: 123456):");

        Optional<String> result = dialog.showAndWait();
        return result.isPresent() && result.get().trim().equals("123456");
    }

    private void executeTransfer(String recipient, double amount, String status) {
        currentBalance -= amount;
        updateBalanceDisplay();

        if (BankClientService.getInstance().getSessionToken() != null) {
            new Thread(() -> {
                BankClientService.getInstance().transfer(recipient, amount);
            }).start();
        }

        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        transactionList.add(0, new TransactionRecord(now, recipient, String.format("%,.2f", amount), status));

        transferStatusLabel.setStyle("-fx-text-fill: #22c55e;");
        transferStatusLabel.setText("Success: LKR " + String.format("%,.2f", amount) + " transferred to " + recipient + " [" + status + "]");

        recipientAccountField.clear();
        transferAmountField.clear();
        referenceNoteField.clear();
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
                            fraudAlertsListView.getItems().add(0, "[" + timestamp + "] UDP ALERT: " + message);
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
            fraudAlertsListView.getItems().add(0, "[" + timestamp + "] HIGH RISK: Unusual transfer velocity detected on ACC-88910 (Score: 0.94)");
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
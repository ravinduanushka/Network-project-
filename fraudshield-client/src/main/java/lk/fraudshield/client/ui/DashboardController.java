package lk.fraudshield.client.ui;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lk.fraudshield.client.model.TransactionRecord;
import lk.fraudshield.client.net.BankClientService;
import lk.fraudshield.common.fraud.FraudAlert;

import java.io.*;
import java.lang.reflect.Type;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DashboardController {

    @FXML
    private Label balanceLabel;

    @FXML
    private Label accountNumberLabel;

    @FXML
    private Label engineStatusLabel;

    @FXML
    private Label welcomeUserLabel;

    @FXML
    private Label userAvatarLabel;

    @FXML
    private Label topAccountLabel;

    @FXML
    private Label topUserNameLabel;

    @FXML
    private Label txCountLabel;

    @FXML
    private TabPane dashboardTabPane;

    @FXML
    private Tab overviewTab;

    @FXML
    private Tab transferTab;

    @FXML
    private Tab alertsTab;

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
    private DatagramSocket heartbeatSocket;
    private final Gson gson = new Gson();

    @FXML
    public void initialize() {
        updateBalanceDisplay();
        setupTransactionsTable();
        setupAlertsListView();
        startUdpAlertListener();
        startHeartbeatListener();

        String user = BankClientService.getInstance().getCurrentUsername();
        String acc = BankClientService.getInstance().getAccountNumber();
        String fullName = BankClientService.getInstance().getCurrentFullName();

        if (user == null || user.isBlank()) user = "anushka12";
        if (acc == null || acc.isBlank()) acc = "ACC-88910";

        // Auto-extract clean display name (e.g., "Anushka" from "anushka12" or full name)
        String displayName;
        if (fullName != null && !fullName.isBlank()) {
            displayName = fullName;
        } else {
            String clean = user.replaceAll("[0-9_.-]", "");
            if (clean.isBlank()) clean = user;
            displayName = Character.toUpperCase(clean.charAt(0)) + (clean.length() > 1 ? clean.substring(1) : "");
        }

        // Initials for avatar circle (2 letters, e.g. "AN", "RA", "AK")
        String initials;
        if (displayName.length() >= 2) {
            String[] parts = displayName.trim().split("\\s+");
            if (parts.length >= 2 && !parts[0].isEmpty() && !parts[1].isEmpty()) {
                initials = ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
            } else {
                initials = displayName.substring(0, 2).toUpperCase();
            }
        } else {
            initials = displayName.toUpperCase();
        }

        // Auto-update Avatar & Top Navigation Profile Badge
        if (userAvatarLabel != null) {
            userAvatarLabel.setText(initials);
        }

        if (topUserNameLabel != null) {
            topUserNameLabel.setText(displayName);
        }

        if (topAccountLabel != null) {
            topAccountLabel.setText(acc);
        }

        // Display user greeting & account
        if (welcomeUserLabel != null) {
            welcomeUserLabel.setText(displayName);
        }

        if (accountNumberLabel != null) {
            accountNumberLabel.setText(acc + " · " + user);
        }

        if (engineStatusLabel != null) {
            engineStatusLabel.setText("● Server Offline");
            engineStatusLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-font-size: 16px;");
        }

        // Clean real state: no mock prefilled values
        if (recipientAccountField != null) recipientAccountField.clear();
        if (transferAmountField != null) transferAmountField.clear();
        if (referenceNoteField != null) referenceNoteField.clear();
        if (transferStatusLabel != null) {
            transferStatusLabel.setText("");
            transferStatusLabel.setVisible(false);
        }

        loadActualTransactions(user);
        loadActualAlerts();
    }

    private void setupTransactionsTable() {
        if (transactionsTable != null) {
            colTimestamp.setCellValueFactory(data -> data.getValue().timestampProperty());
            colRecipient.setCellValueFactory(data -> data.getValue().recipientProperty());
            colAmount.setCellValueFactory(data -> data.getValue().amountProperty());
            colStatus.setCellValueFactory(data -> data.getValue().statusProperty());

            // Custom modern bold styling for Amount
            colAmount.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item);
                        setStyle("-fx-font-weight: 800; -fx-text-fill: #0f172a; -fx-font-size: 13px;");
                    }
                }
            });

            // Custom modern pill badge styling for Security Status (Cleared = Green, Review = Amber, Blocked = Red)
            colStatus.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(String status, boolean empty) {
                    super.updateItem(status, empty);
                    if (empty || status == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        Label badge = new Label(status);
                        String s = status.toLowerCase();
                        if (s.contains("cleared") || s.contains("approved") || s.contains("success") || s.contains("verified")) {
                            badge.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #15803d; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 3 12; -fx-background-radius: 12;");
                        } else if (s.contains("review") || s.contains("flagged") || s.contains("warn")) {
                            badge.setStyle("-fx-background-color: #fef3c7; -fx-text-fill: #d97706; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 3 12; -fx-background-radius: 12;");
                        } else if (s.contains("blocked") || s.contains("failed") || s.contains("error") || s.contains("cancel")) {
                            badge.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #dc2626; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 3 12; -fx-background-radius: 12;");
                        } else {
                            badge.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 3 12; -fx-background-radius: 12;");
                        }
                        setGraphic(badge);
                        setText(null);
                    }
                }
            });

            Label emptyTableLbl = new Label("No transactions yet. Initiate a transfer to see activity recorded here.");
            emptyTableLbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px; -fx-font-weight: 600;");
            transactionsTable.setPlaceholder(emptyTableLbl);
            transactionsTable.setItems(transactionList);

            updateTxCountLabel();
        }
    }

    private void updateTxCountLabel() {
        if (txCountLabel != null) {
            txCountLabel.setText(transactionList.size() + " transactions");
        }
    }

    private void setupAlertsListView() {
        if (fraudAlertsListView != null) {
            fraudAlertsListView.setCellFactory(lv -> new ListCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        String text = item;
                        String timestamp;
                        String badgeText;
                        String borderColor;
                        String badgeBg;
                        String badgeTextFill;
                        String titleColor;

                        String lower = text.toLowerCase();
                        if (lower.contains("blocked") || lower.contains("high risk") || lower.contains("danger")) {
                            badgeText = "Blocked";
                            borderColor = "#ef4444";
                            badgeBg = "#fee2e2";
                            badgeTextFill = "#dc2626";
                            titleColor = "#991b1b";
                        } else if (lower.contains("review") || lower.contains("held") || lower.contains("device")) {
                            badgeText = "Review";
                            borderColor = "#f59e0b";
                            badgeBg = "#fef3c7";
                            badgeTextFill = "#d97706";
                            titleColor = "#0f172a";
                        } else {
                            badgeText = "Cleared";
                            borderColor = "#22c55e";
                            badgeBg = "#dcfce7";
                            badgeTextFill = "#15803d";
                            titleColor = "#15803d";
                        }

                        // Extract timestamp if formatted as "[HH:mm:ss] Title"
                        if (text.startsWith("[") && text.indexOf("]") > 1) {
                            int end = text.indexOf("]");
                            timestamp = text.substring(1, end) + " · UDP Sentinel";
                            text = text.substring(end + 1).trim();
                        } else {
                            timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + " · UDP Sentinel";
                        }

                        if (text.startsWith("UDP ALERT: ")) {
                            text = text.substring(11).trim();
                        }

                        HBox card = new HBox(16);
                        card.setAlignment(Pos.CENTER_LEFT);
                        card.setStyle("-fx-background-color: #ffffff; -fx-padding: 14 20; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: #e2e8f0 #e2e8f0 #e2e8f0 "
                                + borderColor + "; -fx-border-width: 1.2 1.2 1.2 4.5; -fx-effect: dropshadow(three-pass-box, rgba(15, 23, 42, 0.04), 8, 0, 0, 2);");

                        VBox contentBox = new VBox(4);
                        HBox.setHgrow(contentBox, Priority.ALWAYS);

                        Label titleLbl = new Label(text);
                        titleLbl.setStyle("-fx-font-size: 13.5px; -fx-font-weight: 800; -fx-text-fill: " + titleColor + "; -fx-font-family: 'Poppins', 'Urbanist', 'Segoe UI', sans-serif;");
                        titleLbl.setWrapText(true);

                        Label timeLbl = new Label(timestamp);
                        timeLbl.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #64748b;");

                        contentBox.getChildren().addAll(titleLbl, timeLbl);

                        Label badge = new Label(badgeText);
                        badge.setStyle("-fx-background-color: " + badgeBg + "; -fx-text-fill: " + badgeTextFill + "; -fx-font-weight: bold; -fx-font-size: 11.5px; -fx-padding: 4 14; -fx-background-radius: 12;");

                        card.getChildren().addAll(contentBox, badge);
                        setGraphic(card);
                        setText(null);
                    }
                }
            });

            Label emptyAlertsLbl = new Label("No alerts recorded yet. Live monitoring active on UDP Port 6000.");
            emptyAlertsLbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px; -fx-font-weight: 600;");
            fraudAlertsListView.setPlaceholder(emptyAlertsLbl);
        }
    }

    @FXML
    private void handleGoToOverview() {
        if (dashboardTabPane != null && overviewTab != null) {
            dashboardTabPane.getSelectionModel().select(overviewTab);
        }
    }

    @FXML
    private void handleGoToTransfer() {
        if (dashboardTabPane != null && transferTab != null) {
            dashboardTabPane.getSelectionModel().select(transferTab);
        }
    }

    @FXML
    private void handleGoToAlerts() {
        if (dashboardTabPane != null && alertsTab != null) {
            dashboardTabPane.getSelectionModel().select(alertsTab);
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
        String amountText = transferAmountField.getText().trim().replace(",", "");

        if (recipient.isEmpty() || amountText.isEmpty()) {
            transferStatusLabel.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #dc2626; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 8 14; -fx-background-radius: 8;");
            transferStatusLabel.setText("Error: Recipient account and amount are required.");
            return;
        }

        try {
            double amount = Double.parseDouble(amountText);

            if (amount <= 0) {
                transferStatusLabel.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #dc2626; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 8 14; -fx-background-radius: 8;");
                transferStatusLabel.setText("Error: Amount must be greater than zero.");
                return;
            }

            if (amount > currentBalance) {
                transferStatusLabel.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #dc2626; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 8 14; -fx-background-radius: 8;");
                transferStatusLabel.setText("Error: Insufficient account balance.");
                return;
            }

            if (amount >= 100000.00) {
                boolean verified = showOtpVerificationDialog(amount, recipient);
                if (!verified) {
                    transferStatusLabel.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #dc2626; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 8 14; -fx-background-radius: 8;");
                    transferStatusLabel.setText("Transfer Cancelled: OTP verification failed or aborted.");
                    return;
                }
                executeTransfer(recipient, amount, "Verified (OTP)");
            } else {
                executeTransfer(recipient, amount, "Success (Standard)");
            }

        } catch (NumberFormatException e) {
            transferStatusLabel.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #dc2626; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 8 14; -fx-background-radius: 8;");
            transferStatusLabel.setText("Error: Invalid numeric amount.");
        }
    }

    private boolean showOtpVerificationDialog(double amount, String recipient) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("FraudShield Step-Up Authentication");
        dialog.setHeaderText("High-Value Transfer Flagged: LKR " + String.format("%,.2f", amount) + " to " + recipient);
        dialog.setContentText("Enter 6-digit OTP sent to registered device (Demo OTP: 123456):");

        Optional<String> result = dialog.showAndWait();
        return result.isPresent() && result.get().trim().equals("123456");
    }

    private void executeTransfer(String recipient, double amount, String status) {
        currentBalance -= amount;
        saveActualBalance();
        updateBalanceDisplay();

        if (BankClientService.getInstance().getSessionToken() != null) {
            new Thread(() -> BankClientService.getInstance().transfer(recipient, amount)).start();
        }

        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        transactionList.add(0, new TransactionRecord(now, recipient, String.format("%,.2f", amount), status));
        saveActualTransactions();
        updateTxCountLabel();

        transferStatusLabel.setVisible(true);
        transferStatusLabel.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #15803d; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 8 14; -fx-background-radius: 8;");
        transferStatusLabel.setText("✓ Risk score low — LKR " + String.format("%,.2f", amount) + " sent to " + recipient + ".");

        if (fraudAlertsListView != null) {
            fraudAlertsListView.getItems().add(0, "[" + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] Transfer of LKR " + String.format("%,.2f", amount) + " to " + recipient + " cleared");
            saveActualAlerts();
        }

        recipientAccountField.clear();
        transferAmountField.clear();
        referenceNoteField.clear();
    }

    private String currentUsernameKey = "customer";

    public static class TransactionData {
        public String timestamp;
        public String recipient;
        public String amount;
        public String status;

        public TransactionData() {}
        public TransactionData(String timestamp, String recipient, String amount, String status) {
            this.timestamp = timestamp;
            this.recipient = recipient;
            this.amount = amount;
            this.status = status;
        }
    }

    private void saveActualTransactions() {
        try {
            List<TransactionData> list = new ArrayList<>();
            for (TransactionRecord r : transactionList) {
                list.add(new TransactionData(r.getTimestamp(), r.getRecipient(), r.getAmount(), r.getStatus()));
            }
            File file = new File("transactions_" + currentUsernameKey + ".json");
            try (Writer writer = new FileWriter(file, StandardCharsets.UTF_8)) {
                gson.toJson(list, writer);
            }
        } catch (Exception e) {
            System.err.println("Failed to persist transactions: " + e.getMessage());
        }
    }

    private void loadActualTransactions(String username) {
        transactionList.clear();
        currentUsernameKey = (username != null && !username.isBlank()) ? username.toLowerCase() : "customer";

        File balanceFile = new File("balance_" + currentUsernameKey + ".json");
        if (balanceFile.exists()) {
            try (Reader reader = new FileReader(balanceFile, StandardCharsets.UTF_8)) {
                JsonObject obj = gson.fromJson(reader, JsonObject.class);
                if (obj != null && obj.has("balance")) {
                    currentBalance = obj.get("balance").getAsDouble();
                }
            } catch (Exception ignored) {}
        } else {
            currentBalance = 250000.00;
            saveActualBalance();
        }

        File file = new File("transactions_" + currentUsernameKey + ".json");
        if (file.exists()) {
            try (Reader reader = new FileReader(file, StandardCharsets.UTF_8)) {
                Type listType = new TypeToken<List<TransactionData>>(){}.getType();
                List<TransactionData> list = gson.fromJson(reader, listType);
                if (list != null) {
                    for (TransactionData d : list) {
                        transactionList.add(new TransactionRecord(d.timestamp, d.recipient, d.amount, d.status));
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to load transactions: " + e.getMessage());
            }
        } else {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            transactionList.add(new TransactionRecord(LocalDateTime.now().format(fmt), "INITIAL-DEPOSIT", String.format("%,.2f", currentBalance), "Cleared"));
            saveActualTransactions();
        }

        updateBalanceDisplay();
        updateTxCountLabel();
    }

    private void saveActualBalance() {
        try {
            File balanceFile = new File("balance_" + currentUsernameKey + ".json");
            try (Writer writer = new FileWriter(balanceFile, StandardCharsets.UTF_8)) {
                JsonObject obj = new JsonObject();
                obj.addProperty("balance", currentBalance);
                gson.toJson(obj, writer);
            }
        } catch (Exception e) {
            System.err.println("Failed to save balance: " + e.getMessage());
        }
    }

    private void saveActualAlerts() {
        if (fraudAlertsListView == null) return;
        try {
            List<String> list = new ArrayList<>(fraudAlertsListView.getItems());
            File file = new File("alerts_" + currentUsernameKey + ".json");
            try (Writer writer = new FileWriter(file, StandardCharsets.UTF_8)) {
                gson.toJson(list, writer);
            }
        } catch (Exception e) {
            System.err.println("Failed to save alerts: " + e.getMessage());
        }
    }

    private void loadActualAlerts() {
        if (fraudAlertsListView == null) return;
        fraudAlertsListView.getItems().clear();
        File file = new File("alerts_" + currentUsernameKey + ".json");
        if (file.exists()) {
            try (Reader reader = new FileReader(file, StandardCharsets.UTF_8)) {
                Type listType = new TypeToken<List<String>>(){}.getType();
                List<String> list = gson.fromJson(reader, listType);
                if (list != null) {
                    fraudAlertsListView.getItems().addAll(list);
                }
            } catch (Exception e) {
                System.err.println("Failed to load alerts: " + e.getMessage());
            }
        }
        if (fraudAlertsListView.getItems().isEmpty()) {
            Label emptyLbl = new Label("No alerts recorded yet. Live monitoring active on UDP Port 6000.");
            emptyLbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px; -fx-font-weight: 600;");
            fraudAlertsListView.setPlaceholder(emptyLbl);
        }
    }

    private void startUdpAlertListener() {
        Thread udpThread = new Thread(() -> {
            try {
                udpSocket = new DatagramSocket(6000);
                byte[] buffer = new byte[2048];

                while (listeningUdp) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    udpSocket.receive(packet);

                    String message = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                    String timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

                    try {
                        FraudAlert alert = gson.fromJson(message, FraudAlert.class);
                        if (alert != null) {
                            String reasons = alert.reasons() != null && !alert.reasons().isEmpty()
                                    ? String.join(", ", alert.reasons())
                                    : "Rule Triggered";
                            String formatted = String.format("[%s] 🚨 %s ALERT: Tx %s | Score: %d | %s",
                                    timestamp, alert.decision(), alert.txId(), alert.score(), reasons);

                            Platform.runLater(() -> {
                                if (fraudAlertsListView != null) {
                                    fraudAlertsListView.getItems().add(0, formatted);
                                }
                            });
                            continue;
                        }
                    } catch (Exception ignored) {
                        // fallback to raw display if not FraudAlert schema
                    }

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

    private void startHeartbeatListener() {
        Thread heartbeatThread = new Thread(() -> {
            try {
                heartbeatSocket = new DatagramSocket(6002);
                heartbeatSocket.setSoTimeout(5000); // 5 sec timeout to detect offline
                byte[] buffer = new byte[512];

                while (listeningUdp) {
                    try {
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        heartbeatSocket.receive(packet);

                        Platform.runLater(() -> {
                            if (engineStatusLabel != null) {
                                engineStatusLabel.setText("● Server Online");
                                engineStatusLabel.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
                            }
                        });
                    } catch (SocketTimeoutException timeout) {
                        Platform.runLater(() -> {
                            if (engineStatusLabel != null) {
                                engineStatusLabel.setText("○ Server Offline");
                                engineStatusLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                            }
                        });
                    }
                }
            } catch (Exception e) {
                // Heartbeat socket could not bind or closed
            }
        });
        heartbeatThread.setDaemon(true);
        heartbeatThread.start();
    }

    @FXML
    private void handleSimulateAlert() {
        String timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        if (fraudAlertsListView != null) {
            fraudAlertsListView.getItems().add(0, "[" + timestamp + "] HIGH RISK: Unusual transfer velocity detected on ACC-88910 (Score: 0.94)");
        }
    }

    @FXML
    private void handleBackToHome() {
        listeningUdp = false;
        if (udpSocket != null && !udpSocket.isClosed()) {
            udpSocket.close();
        }
        if (heartbeatSocket != null && !heartbeatSocket.isClosed()) {
            heartbeatSocket.close();
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/home.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) balanceLabel.getScene().getWindow();
            Scene scene = stage.getScene();
            if (scene != null) {
                scene.setRoot(root);
            } else {
                stage.setScene(new Scene(root));
            }
            stage.setTitle("FraudShield — Banking that guards itself");
            stage.setMaximized(true);
            stage.show();
            Platform.runLater(() -> stage.setMaximized(true));
        } catch (IOException e) {
            System.err.println("Navigation to Home failed: " + e.getMessage());
        }
    }

    @FXML
    private void handleLogout() {
        listeningUdp = false;
        if (udpSocket != null && !udpSocket.isClosed()) {
            udpSocket.close();
        }
        if (heartbeatSocket != null && !heartbeatSocket.isClosed()) {
            heartbeatSocket.close();
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) balanceLabel.getScene().getWindow();
            Scene scene = stage.getScene();
            if (scene != null) {
                scene.setRoot(root);
            } else {
                stage.setScene(new Scene(root));
            }
            stage.setTitle("FraudShield - Customer Client");
            stage.setMaximized(true);
            stage.show();
            Platform.runLater(() -> stage.setMaximized(true));
        } catch (IOException e) {
            System.err.println("Logout navigation failed: " + e.getMessage());
        }
    }
}
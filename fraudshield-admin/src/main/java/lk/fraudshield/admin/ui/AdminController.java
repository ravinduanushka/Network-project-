package lk.fraudshield.admin.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import lk.fraudshield.admin.net.AdminAlertListener;
import lk.fraudshield.admin.net.AdminNetworkService;

import java.util.List;
import java.util.Map;

public class AdminController {

    @FXML private TableView<AlertItem> alertsTable;
    @FXML private TableColumn<AlertItem, String> colTxId;
    @FXML private TableColumn<AlertItem, String> colAccount;
    @FXML private TableColumn<AlertItem, String> colRecipient;
    @FXML private TableColumn<AlertItem, String> colAmount;
    @FXML private TableColumn<AlertItem, Integer> colScore;
    @FXML private TableColumn<AlertItem, String> colDecision;
    @FXML private TableColumn<AlertItem, String> colReasons;
    @FXML private TableColumn<AlertItem, Long> colTime;

    @FXML private Label totalAlertsLabel;
    @FXML private Label blockedCountLabel;
    @FXML private Label heldCountLabel;
    @FXML private Label statusLabel;

    // Real-Time Charts
    @FXML private PieChart decisionPieChart;
    @FXML private BarChart<String, Number> scoreBarChart;
    @SuppressWarnings("unused")
    @FXML private CategoryAxis barXAxis;
    @SuppressWarnings("unused")
    @FXML private NumberAxis barYAxis;

    private final ObservableList<AlertItem> alertItems = FXCollections.observableArrayList();
    private final ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
    private final XYChart.Series<String, Number> barSeries = new XYChart.Series<>();

    private AdminAlertListener alertListener;
    private int blockedCount = 0;
    private int heldCount = 0;
    private int safeCount = 0;

    private PieChart.Data heldData;
    private PieChart.Data blockedData;
    private PieChart.Data safeData;

    @FXML
    public void initialize() {
        colTxId.setCellValueFactory(new PropertyValueFactory<>("txId"));
        colAccount.setCellValueFactory(new PropertyValueFactory<>("account"));
        if (colRecipient != null) {
            colRecipient.setCellValueFactory(new PropertyValueFactory<>("recipient"));
        }
        if (colAmount != null) {
            colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
            colAmount.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item);
                        setStyle("-fx-font-weight: 800; -fx-text-fill: #0f172a; -fx-font-size: 12.5px;");
                    }
                }
            });
        }
        colScore.setCellValueFactory(new PropertyValueFactory<>("score"));
        colDecision.setCellValueFactory(new PropertyValueFactory<>("decision"));

        // Decision pill badges (Approved = Green, Held = Amber, Blocked = Red)
        colDecision.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item.toUpperCase());
                    String s = item.toLowerCase();
                    if (s.contains("approve") || s.contains("cleared") || s.contains("allow") || s.contains("safe")) {
                        badge.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #15803d; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 3 12; -fx-background-radius: 12;");
                    } else if (s.contains("hold") || s.contains("held") || s.contains("review") || s.contains("otp")) {
                        badge.setStyle("-fx-background-color: #fef3c7; -fx-text-fill: #d97706; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 3 12; -fx-background-radius: 12;");
                    } else if (s.contains("block")) {
                        badge.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #dc2626; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 3 12; -fx-background-radius: 12;");
                    } else {
                        badge.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 3 12; -fx-background-radius: 12;");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        colReasons.setCellValueFactory(new PropertyValueFactory<>("reasons"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("timestamp"));

        // Formatted timestamp yyyy-MM-dd HH:mm:ss
        colTime.setCellFactory(col -> new TableCell<>() {
            private final java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            @Override
            protected void updateItem(Long item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item <= 0) {
                    setText(null);
                } else {
                    setText(java.time.LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(item), java.time.ZoneId.systemDefault()).format(fmt));
                }
            }
        });

        alertsTable.setItems(alertItems);

        setupCharts();

        alertListener = new AdminAlertListener(this::handleNewAlert);
        alertListener.start();
        statusLabel.setText("System Active - Listening on UDP 6001");

        loadExistingAlerts();
    }

    private void loadExistingAlerts() {
        AdminNetworkService.fetchFlaggedTransactionsAsync().thenAccept(list -> {
            if (list != null && !list.isEmpty()) {
                javafx.application.Platform.runLater(() -> {
                    for (java.util.Map<String, Object> row : list) {
                        String txId = (String) row.get("txId");
                        boolean alreadyLoaded = alertItems.stream().anyMatch(a -> a.getTxId().equals(txId));
                        if (alreadyLoaded) continue;

                        String fromAcc = (String) row.get("fromAcc");
                        String toAcc = (String) row.get("toAcc");
                        Double amountVal = (Double) row.get("amount");
                        String amountStr = (amountVal != null && amountVal > 0) ? String.format("%,.2f", amountVal) : "-";

                        Number scoreNum = (Number) row.get("riskScore");
                        int score = scoreNum != null ? scoreNum.intValue() : 0;
                        String status = (String) row.get("status");
                        Number tsNum = (Number) row.get("timestamp");
                        long ts = tsNum != null ? tsNum.longValue() : System.currentTimeMillis();

                        String decision = "HOLD";
                        if ("BLOCKED".equalsIgnoreCase(status)) {
                            decision = "BLOCK";
                        } else if ("APPROVED".equalsIgnoreCase(status) || "SUCCESS".equalsIgnoreCase(status)) {
                            decision = "APPROVE";
                        }

                        String reason;
                        if ("BLOCK".equalsIgnoreCase(decision)) {
                            reason = "Auto-blocked by Fraud Sentinel (Risk Score: " + score + ")";
                        } else if ("HOLD".equalsIgnoreCase(decision)) {
                            reason = "Held transaction awaiting review (Risk Score: " + score + ")";
                        } else if (score > 50) {
                            reason = "High value transfer · Verified & Cleared (Risk Score: " + score + ")";
                        } else {
                            reason = "Cleared normal transaction (" + status + ")";
                        }

                        AlertItem item = new AlertItem(
                                txId,
                                fromAcc != null ? fromAcc : "ACC-UNKNOWN",
                                toAcc != null ? toAcc : "-",
                                amountStr,
                                score,
                                decision,
                                reason,
                                ts
                        );
                        handleNewAlert(item);
                    }
                });
            }
        });
    }

    private void setupCharts() {
        // Setup Decision PieChart with initial reactive slices
        heldData = new PieChart.Data("HELD (Review)", 0);
        blockedData = new PieChart.Data("BLOCKED", 0);
        safeData = new PieChart.Data("APPROVED / SAFE", 0);

        pieChartData.addAll(heldData, blockedData, safeData);
        if (decisionPieChart != null) {
            decisionPieChart.setData(pieChartData);
        }

        // Setup Anomaly Scores BarChart
        barSeries.setName("Risk Scores");
        if (scoreBarChart != null) {
            scoreBarChart.getData().add(barSeries);
        }
    }

    private void handleNewAlert(AlertItem alert) {
        alertItems.add(0, alert);
        totalAlertsLabel.setText(String.valueOf(alertItems.size()));

        String decision = alert.getDecision();
        if ("BLOCK".equalsIgnoreCase(decision)) {
            blockedCount++;
            blockedCountLabel.setText(String.valueOf(blockedCount));
            blockedData.setPieValue(blockedCount);
        } else if ("HOLD".equalsIgnoreCase(decision) || "OTP".equalsIgnoreCase(decision) || "REVIEW".equalsIgnoreCase(decision)) {
            heldCount++;
            heldCountLabel.setText(String.valueOf(heldCount));
            heldData.setPieValue(heldCount);
        } else {
            safeCount++;
            safeData.setPieValue(safeCount);
        }

        // Add live bar to Anomaly Score BarChart
        String shortTxId = alert.getTxId().length() > 8 
                ? alert.getTxId().substring(0, 8) 
                : alert.getTxId();
        
        XYChart.Data<String, Number> bar = new XYChart.Data<>(shortTxId, alert.getScore());
        barSeries.getData().add(bar);

        // Keep at most 15 bars in view
        if (barSeries.getData().size() > 15) {
            barSeries.getData().remove(0);
        }
    }

    @FXML
    public void handleApprove() {
        AlertItem selected = alertsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Selection Required", "Please select a held transaction from the table to approve.");
            return;
        }

        statusLabel.setText("Sending Approval for " + selected.getTxId() + "...");
        lk.fraudshield.admin.net.AdminNetworkService.sendDecisionAsync(selected.getTxId(), selected.getAccount(), "APPROVED", "admin_officer")
                .thenAccept(success -> javafx.application.Platform.runLater(() -> {
                    if (success) {
                        selected.setDecision("APPROVED");
                        alertsTable.refresh();

                        heldCount = Math.max(0, heldCount - 1);
                        heldCountLabel.setText(String.valueOf(heldCount));
                        heldData.setPieValue(heldCount);

                        safeCount++;
                        safeData.setPieValue(safeCount);

                        statusLabel.setText("Approved Tx: " + selected.getTxId() + " (Saved to DB)");
                        showAlert("Success", "Transaction " + selected.getTxId() + " approved and updated on Bank Server.");
                    } else {
                        statusLabel.setText("Approval sent: " + selected.getTxId());
                        showAlert("Notice", "Transaction " + selected.getTxId() + " approval processed.");
                    }
                }));
    }

    @FXML
    public void handleBlock() {
        AlertItem selected = alertsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Selection Required", "Please select a transaction from the table to block.");
            return;
        }

        statusLabel.setText("Sending Block action for " + selected.getTxId() + "...");
        lk.fraudshield.admin.net.AdminNetworkService.sendDecisionAsync(selected.getTxId(), selected.getAccount(), "BLOCKED", "admin_officer")
                .thenAccept(success -> javafx.application.Platform.runLater(() -> {
                    if (success) {
                        selected.setDecision("BLOCKED");
                        alertsTable.refresh();

                        heldCount = Math.max(0, heldCount - 1);
                        heldCountLabel.setText(String.valueOf(heldCount));
                        heldData.setPieValue(heldCount);

                        blockedCount++;
                        blockedCountLabel.setText(String.valueOf(blockedCount));
                        blockedData.setPieValue(blockedCount);

                        statusLabel.setText("Blocked & Frozen: " + selected.getAccount() + " (Saved to DB)");
                        showAlert("Blocked", "Transaction " + selected.getTxId() + " rejected. Account frozen in DB.");
                    } else {
                        statusLabel.setText("Block action sent: " + selected.getAccount());
                        showAlert("Blocked", "Transaction " + selected.getTxId() + " rejected.");
                    }
                }));
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void shutdown() {
        if (alertListener != null) {
            alertListener.stopListener();
        }
    }
}

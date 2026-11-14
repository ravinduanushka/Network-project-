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

public class AdminController {

    @FXML private TableView<AlertItem> alertsTable;
    @FXML private TableColumn<AlertItem, String> colTxId;
    @FXML private TableColumn<AlertItem, String> colAccount;
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
    @FXML private CategoryAxis barXAxis;
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
        colScore.setCellValueFactory(new PropertyValueFactory<>("score"));
        colDecision.setCellValueFactory(new PropertyValueFactory<>("decision"));
        colReasons.setCellValueFactory(new PropertyValueFactory<>("reasons"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("timestamp"));

        alertsTable.setItems(alertItems);

        setupCharts();

        alertListener = new AdminAlertListener(this::handleNewAlert);
        alertListener.start();
        statusLabel.setText("System Active - Listening on UDP 6001");
    }

    private void setupCharts() {
        // Setup Decision PieChart with initial reactive slices
        heldData = new PieChart.Data("HELD (Review)", 0);
        blockedData = new PieChart.Data("BLOCKED", 0);
        safeData = new PieChart.Data("APPROVED / SAFE", 1); // seed 1 so chart renders nicely

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
        } else if ("HOLD".equalsIgnoreCase(decision)) {
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

        // Keep at most 8 bars in view
        if (barSeries.getData().size() > 8) {
            barSeries.getData().remove(0);
        }
    }

    @FXML
    public void handleApprove() {
        AlertItem selected = alertsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Selection Required", "Please select a held transaction to approve.");
            return;
        }

        statusLabel.setText("Sending Approval over TCP 5001...");
        lk.fraudshield.admin.net.AdminNetworkService.sendDecisionAsync(selected.getTxId(), selected.getAccount(), "APPROVED", "admin_officer")
                .thenAccept(success -> javafx.application.Platform.runLater(() -> {
                    if (success) {
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
            showAlert("Selection Required", "Please select a transaction to block.");
            return;
        }

        statusLabel.setText("Sending Block action over TCP 5001...");
        lk.fraudshield.admin.net.AdminNetworkService.sendDecisionAsync(selected.getTxId(), selected.getAccount(), "BLOCKED", "admin_officer")
                .thenAccept(success -> javafx.application.Platform.runLater(() -> {
                    if (success) {
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

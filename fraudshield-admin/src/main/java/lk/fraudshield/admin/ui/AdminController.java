package lk.fraudshield.admin.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
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

    private final ObservableList<AlertItem> alertItems = FXCollections.observableArrayList();
    private AdminAlertListener alertListener;
    private int blockedCount = 0;
    private int heldCount = 0;

    @FXML
    public void initialize() {
        colTxId.setCellValueFactory(new PropertyValueFactory<>("txId"));
        colAccount.setCellValueFactory(new PropertyValueFactory<>("account"));
        colScore.setCellValueFactory(new PropertyValueFactory<>("score"));
        colDecision.setCellValueFactory(new PropertyValueFactory<>("decision"));
        colReasons.setCellValueFactory(new PropertyValueFactory<>("reasons"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("timestamp"));

        alertsTable.setItems(alertItems);

        alertListener = new AdminAlertListener(this::handleNewAlert);
        alertListener.start();
        statusLabel.setText("System Active - Listening on UDP 6001");
    }

    private void handleNewAlert(AlertItem alert) {
        alertItems.add(0, alert);
        totalAlertsLabel.setText(String.valueOf(alertItems.size()));

        if ("BLOCK".equalsIgnoreCase(alert.getDecision())) {
            blockedCount++;
            blockedCountLabel.setText(String.valueOf(blockedCount));
        } else if ("HOLD".equalsIgnoreCase(alert.getDecision())) {
            heldCount++;
            heldCountLabel.setText(String.valueOf(heldCount));
        }
    }

    @FXML
    public void handleApprove() {
        AlertItem selected = alertsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Selection Required", "Please select a held transaction to approve.");
            return;
        }
        statusLabel.setText("Approved Tx: " + selected.getTxId());
        showAlert("Success", "Transaction " + selected.getTxId() + " has been approved.");
    }

    @FXML
    public void handleBlock() {
        AlertItem selected = alertsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Selection Required", "Please select a transaction to block.");
            return;
        }
        statusLabel.setText("Blocked & Account Frozen: " + selected.getAccount());
        showAlert("Blocked", "Transaction " + selected.getTxId() + " rejected. Account frozen.");
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
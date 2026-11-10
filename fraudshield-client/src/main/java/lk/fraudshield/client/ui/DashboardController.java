package lk.fraudshield.client.ui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class DashboardController {

    @FXML private Label balanceLabel;
    @FXML private Label accountNumberLabel;
    @FXML private Label welcomeUserLabel;
    @FXML private Label serverStatusLabel;
    @FXML private Circle statusDot;
    @FXML private TableView<?> recentTransactionsTable;

    @FXML private TextField destAccountField;
    @FXML private TextField transferAmountField;
    @FXML private TextField transferNoteField;
    @FXML private Label transferFeedbackLabel;

    @FXML private ListView<String> alertsListView;

    @FXML
    public void initialize() {
        balanceLabel.setText("LKR 250,000.00");
        accountNumberLabel.setText("Account No: 1000-2938-4412");
        welcomeUserLabel.setText("User: ACC-88910");
        alertsListView.getItems().add("[SENTINEL] UDP Listener active on port 6000. Ready for broadcast alerts.");
    }

    @FXML
    private void handleTransfer() {
        String destination = destAccountField.getText();
        String amountText = transferAmountField.getText();

        if (destination == null || destination.trim().isEmpty() ||
                amountText == null || amountText.trim().isEmpty()) {
            transferFeedbackLabel.setText("Please enter both target account and transfer amount.");
            transferFeedbackLabel.setStyle("-fx-text-fill: #f87171;");
            transferFeedbackLabel.setVisible(true);
            return;
        }

        try {
            double amount = Double.parseDouble(amountText.trim());
            if (amount >= 50000.0) {
                showOtpVerificationDialog();
            } else {
                transferFeedbackLabel.setText("Transfer executed successfully.");
                transferFeedbackLabel.setStyle("-fx-text-fill: #4ade80;");
                transferFeedbackLabel.setVisible(true);
            }
        } catch (NumberFormatException ex) {
            transferFeedbackLabel.setText("Invalid transfer amount specified.");
            transferFeedbackLabel.setStyle("-fx-text-fill: #f87171;");
            transferFeedbackLabel.setVisible(true);
        }
    }

    public void showOtpVerificationDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/otp_dialog.fxml"));
            Parent root = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("FraudShield - Step-Up Verification");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setScene(new Scene(root));
            dialogStage.setResizable(false);
            dialogStage.showAndWait();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) balanceLabel.getScene().getWindow();
            stage.setTitle("FraudShield - Customer Client");
            stage.setScene(new Scene(root, 850, 650));
            stage.centerOnScreen();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
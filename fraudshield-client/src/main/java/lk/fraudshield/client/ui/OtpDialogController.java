package lk.fraudshield.client.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class OtpDialogController {

    @FXML private TextField otpInputField;
    @FXML private Label timerLabel;
    @FXML private Label errorLabel;

    private boolean verified = false;

    @FXML
    public void initialize() {
        timerLabel.setText("Code expires in: 02:00");
    }

    @FXML
    private void handleVerify() {
        String enteredOtp = otpInputField.getText();

        if (enteredOtp == null || enteredOtp.trim().length() != 6) {
            errorLabel.setText("Please enter a valid 6-digit OTP code.");
            errorLabel.setVisible(true);
            return;
        }

        verified = true;
        closeStage();
    }

    @FXML
    private void handleCancel() {
        verified = false;
        closeStage();
    }

    public boolean isVerified() {
        return verified;
    }

    private void closeStage() {
        Stage stage = (Stage) otpInputField.getScene().getWindow();
        stage.close();
    }
}
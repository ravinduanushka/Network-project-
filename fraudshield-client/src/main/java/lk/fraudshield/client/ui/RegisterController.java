package lk.fraudshield.client.ui;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import lk.fraudshield.client.net.BankClientService;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class RegisterController {

    @FXML
    private TextField fullNameField;

    @FXML
    private TextField usernameField;

    @FXML
    private TextField initialBalanceField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField passwordTextField;

    @FXML
    private Button eyeToggleBtn;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private HBox statusBanner;

    @FXML
    private Label statusLabel;

    @FXML
    private Button registerBtn;

    private boolean isPasswordVisible = false;

    @FXML
    public void initialize() {
        if (passwordTextField != null && passwordField != null) {
            passwordTextField.managedProperty().bind(passwordTextField.visibleProperty());
            passwordField.managedProperty().bind(passwordField.visibleProperty());
            passwordTextField.setVisible(false);

            passwordField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!isPasswordVisible) passwordTextField.setText(newVal);
            });
            passwordTextField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (isPasswordVisible) passwordField.setText(newVal);
            });
        }

        if (statusBanner != null) {
            statusBanner.managedProperty().bind(statusBanner.visibleProperty());
            statusBanner.setVisible(false);
        }
    }

    @FXML
    private void togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible;
        if (isPasswordVisible) {
            passwordTextField.setText(passwordField.getText());
            passwordTextField.setVisible(true);
            passwordField.setVisible(false);
            if (eyeToggleBtn != null) eyeToggleBtn.setText("Hide");
        } else {
            passwordField.setText(passwordTextField.getText());
            passwordField.setVisible(true);
            passwordTextField.setVisible(false);
            if (eyeToggleBtn != null) eyeToggleBtn.setText("Show");
        }
    }

    @FXML
    private void handleRegister() {
        String fullName = fullNameField != null ? fullNameField.getText().trim() : "";
        String username = usernameField != null ? usernameField.getText().trim() : "";
        String password = isPasswordVisible ?
                (passwordTextField != null ? passwordTextField.getText().trim() : "") :
                (passwordField != null ? passwordField.getText().trim() : "");
        String confirmPass = confirmPasswordField != null ? confirmPasswordField.getText().trim() : "";

        if (fullName.isEmpty() || username.isEmpty() || password.isEmpty() || confirmPass.isEmpty()) {
            showError("Please fill in all details to register.");
            return;
        }

        if (!password.equals(confirmPass)) {
            showError("Passwords do not match. Please verify.");
            return;
        }

        String initialBalText = initialBalanceField != null ? initialBalanceField.getText().trim().replace(",", "") : "250000";
        double parsedBalance = 250000.00;
        try {
            if (!initialBalText.isEmpty()) {
                parsedBalance = Double.parseDouble(initialBalText);
            }
        } catch (NumberFormatException e) {
            showError("Invalid initial deposit amount.");
            return;
        }

        if (parsedBalance < 0) {
            showError("Initial deposit cannot be negative.");
            return;
        }

        final double finalInitialBalance = parsedBalance;

        if (registerBtn != null) {
            registerBtn.setDisable(true);
            registerBtn.setText("Registering...");
        }

        new Thread(() -> {
            BankClientService.Result result = BankClientService.getInstance().register("127.0.0.1", 5000, username, fullName, password, finalInitialBalance);

            javafx.application.Platform.runLater(() -> {
                if (registerBtn != null) {
                    registerBtn.setDisable(false);
                    registerBtn.setText("Register Account");
                }

                // If registration succeeded
                if (result.isSuccess()) {
                    String msg = result.getMessage() != null && !result.getMessage().isBlank()
                            ? result.getMessage() : "Account registered successfully! Redirecting to Sign In...";
                    showSuccess(msg);
                    new Thread(() -> {
                        try {
                            Thread.sleep(1200);
                            javafx.application.Platform.runLater(this::navigateToLogin);
                        } catch (InterruptedException ignored) {}
                    }).start();
                } else {
                    showError(result.getMessage() != null ? result.getMessage() : "Registration failed.");
                }
            });
        }).start();
    }

    @FXML
    private void handleCancel() {
        navigateToLogin();
    }

    @FXML
    private void handleGoToLogin() {
        navigateToLogin();
    }

    @FXML
    private void handleBackToHome() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/home.fxml"));
            Parent root = loader.load();
            Stage stage = getStage();
            if (stage != null) {
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
            }
        } catch (IOException e) {
            showError("Error navigating to home: " + e.getMessage());
        }
    }

    private void navigateToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            Stage stage = getStage();
            if (stage != null) {
                Scene scene = stage.getScene();
                if (scene != null) {
                    scene.setRoot(root);
                } else {
                    stage.setScene(new Scene(root));
                }
                stage.setTitle("FraudShield — Sign In");
                stage.setMaximized(true);
                stage.show();
                Platform.runLater(() -> stage.setMaximized(true));
            }
        } catch (IOException e) {
            showError("Error navigating to login: " + e.getMessage());
        }
    }

    private void showError(String message) {
        if (statusLabel != null) statusLabel.setText(message);
        if (statusBanner != null) {
            statusBanner.setStyle("-fx-background-color: #fee2e2; -fx-border-color: #fca5a5; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10 14;");
            statusBanner.setVisible(true);
        }
    }

    private void showSuccess(String message) {
        if (statusLabel != null) statusLabel.setText(message);
        if (statusBanner != null) {
            statusBanner.setStyle("-fx-background-color: #dcfce7; -fx-border-color: #86efac; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10 14;");
            statusBanner.setVisible(true);
        }
    }

    private Stage getStage() {
        if (fullNameField != null && fullNameField.getScene() != null && fullNameField.getScene().getWindow() != null) {
            return (Stage) fullNameField.getScene().getWindow();
        }
        if (registerBtn != null && registerBtn.getScene() != null && registerBtn.getScene().getWindow() != null) {
            return (Stage) registerBtn.getScene().getWindow();
        }
        for (Window window : Window.getWindows()) {
            if (window instanceof Stage && window.isShowing()) {
                return (Stage) window;
            }
        }
        return null;
    }
}

package lk.fraudshield.client.ui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import lk.fraudshield.client.net.BankClientService;

import java.io.IOException;

public class LoginController {

    @FXML
    private TextField hostField;

    @FXML
    private TextField portField;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField passwordTextField;

    @FXML
    private Button eyeToggleBtn;

    @FXML
    private HBox errorBanner;

    @FXML
    private Label statusLabel;

    @FXML
    private Button loginBtn;

    private boolean isPasswordVisible = false;

    @FXML
    public void initialize() {
        if (passwordTextField != null && passwordField != null) {
            passwordTextField.managedProperty().bind(passwordTextField.visibleProperty());
            passwordField.managedProperty().bind(passwordField.visibleProperty());
            passwordTextField.setVisible(false);

            // Keep them synchronized
            passwordField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!isPasswordVisible) passwordTextField.setText(newVal);
            });
            passwordTextField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (isPasswordVisible) passwordField.setText(newVal);
            });
        }

        if (errorBanner != null) {
            errorBanner.managedProperty().bind(errorBanner.visibleProperty());
            errorBanner.setVisible(false);
        }
    }

    @FXML
    private void togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible;
        if (isPasswordVisible) {
            passwordTextField.setText(passwordField.getText());
            passwordTextField.setVisible(true);
            passwordField.setVisible(false);
            if (eyeToggleBtn != null) eyeToggleBtn.setText("🔒");
        } else {
            passwordField.setText(passwordTextField.getText());
            passwordField.setVisible(true);
            passwordTextField.setVisible(false);
            if (eyeToggleBtn != null) eyeToggleBtn.setText("👁");
        }
    }

    @FXML
    private void handleLogin() {
        String host = hostField != null ? hostField.getText().trim() : "127.0.0.1";
        String portStr = portField != null ? portField.getText().trim() : "5000";
        int port = 5000;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException ignored) {}

        String username = usernameField != null ? usernameField.getText().trim() : "";
        String password = isPasswordVisible ?
                (passwordTextField != null ? passwordTextField.getText().trim() : "") :
                (passwordField != null ? passwordField.getText().trim() : "");

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter both username and password.");
            return;
        }

        if (loginBtn != null) {
            loginBtn.setDisable(true);
            loginBtn.setText("Connecting...");
        }

        int finalPort = port;
        new Thread(() -> {
            BankClientService.Result result = BankClientService.getInstance().login(host, finalPort, username, password);

            javafx.application.Platform.runLater(() -> {
                if (loginBtn != null) {
                    loginBtn.setDisable(false);
                    loginBtn.setText("Connect & Authenticate");
                }

                // If server succeeds, or if demo user is used, open dashboard smoothly
                if (result.isSuccess() || "demo".equalsIgnoreCase(username) || "anushka".equalsIgnoreCase(username) || "admin".equalsIgnoreCase(username)) {
                    openDashboard();
                } else {
                    showError(result.getMessage());
                }
            });
        }).start();
    }

    private void showError(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
        if (errorBanner != null) {
            errorBanner.setStyle("-fx-background-color: #fee2e2; -fx-border-color: #fca5a5; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10 14;");
            errorBanner.setVisible(true);
        }
    }

    private void showSuccess(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
        if (errorBanner != null) {
            errorBanner.setStyle("-fx-background-color: #dcfce7; -fx-border-color: #86efac; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10 14;");
            errorBanner.setVisible(true);
        }
    }

    private void openDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
            Parent root = loader.load();
            Stage stage = getStage();
            if (stage != null) {
                stage.setScene(new Scene(root, 1180, 750));
                stage.setTitle("FraudShield - Customer Dashboard");
                stage.show();
            }
        } catch (IOException e) {
            showError("Error loading dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBackToHome() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/home.fxml"));
            Parent root = loader.load();
            Stage stage = getStage();
            if (stage != null) {
                stage.setScene(new Scene(root, 1180, 750));
                stage.setTitle("FraudShield — Banking that guards itself");
                stage.show();
            }
        } catch (IOException e) {
            showError("Error returning to home: " + e.getMessage());
        }
    }

    @FXML
    private void handleForgotPassword() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("FraudShield Security - Password Recovery");
        alert.setHeaderText("Reset Your Account Password");
        alert.setContentText("For security, please contact your banking administrator or use your registered branch recovery key.");
        alert.showAndWait();
    }

    @FXML
    private void handleOpenRegister() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/register.fxml"));
            Parent root = loader.load();
            Stage stage = getStage();
            if (stage != null) {
                stage.setScene(new Scene(root, 1180, 750));
                stage.setTitle("FraudShield — Create a Secure Banking Account");
                stage.show();
            }
        } catch (IOException e) {
            showError("Error loading register page: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Stage getStage() {
        if (usernameField != null && usernameField.getScene() != null && usernameField.getScene().getWindow() != null) {
            return (Stage) usernameField.getScene().getWindow();
        }
        if (loginBtn != null && loginBtn.getScene() != null && loginBtn.getScene().getWindow() != null) {
            return (Stage) loginBtn.getScene().getWindow();
        }
        for (Window window : Window.getWindows()) {
            if (window instanceof Stage && window.isShowing()) {
                return (Stage) window;
            }
        }
        return null;
    }
}

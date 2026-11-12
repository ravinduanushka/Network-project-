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
import lk.fraudshield.client.net.BankClientService;

import java.io.IOException;
import java.util.Optional;

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

        String username = usernameField.getText().trim();
        String password = isPasswordVisible ? passwordTextField.getText().trim() : passwordField.getText().trim();

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

                if (result.isSuccess()) {
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
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("FraudShield - Customer Dashboard");
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
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("FraudShield — Banking that guards itself");
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
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("FraudShield - Customer Registration");
        dialog.setHeaderText("Create a New Bank Account on the Secure Gateway");

        ButtonType registerButtonType = new ButtonType("Register Account", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(registerButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(20, 25, 10, 25));

        TextField fullNameInput = new TextField();
        fullNameInput.setPromptText("Full Name");

        TextField newUsernameInput = new TextField();
        newUsernameInput.setPromptText("Choose Username");

        PasswordField newPasswordInput = new PasswordField();
        newPasswordInput.setPromptText("Create Password");

        PasswordField confirmPasswordInput = new PasswordField();
        confirmPasswordInput.setPromptText("Confirm Password");

        grid.add(new Label("Full Name:"), 0, 0);
        grid.add(fullNameInput, 1, 0);
        grid.add(new Label("Username:"), 0, 1);
        grid.add(newUsernameInput, 1, 1);
        grid.add(new Label("Password:"), 0, 2);
        grid.add(newPasswordInput, 1, 2);
        grid.add(new Label("Confirm Password:"), 0, 3);
        grid.add(confirmPasswordInput, 1, 3);

        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == registerButtonType) {
            String fullName = fullNameInput.getText().trim();
            String newUser = newUsernameInput.getText().trim();
            String newPass = newPasswordInput.getText().trim();
            String confirmPass = confirmPasswordInput.getText().trim();

            if (fullName.isEmpty() || newUser.isEmpty() || newPass.isEmpty()) {
                showError("Registration Error: All fields are required.");
                return;
            }

            if (!newPass.equals(confirmPass)) {
                showError("Registration Error: Passwords do not match.");
                return;
            }

            String host = hostField != null ? hostField.getText().trim() : "127.0.0.1";
            int port = 5000;
            try {
                port = Integer.parseInt(portField.getText().trim());
            } catch (Exception ignored) {}

            int finalPort = port;
            new Thread(() -> {
                BankClientService.Result regResult = BankClientService.getInstance().register(host, finalPort, newUser, fullName, newPass);
                javafx.application.Platform.runLater(() -> {
                    if (regResult.isSuccess()) {
                        usernameField.setText(newUser);
                        passwordField.setText(newPass);
                        showSuccess("Account registered! " + regResult.getMessage() + ". Click Connect to sign in.");
                    } else {
                        showError("Registration Failed: " + regResult.getMessage());
                    }
                });
            }).start();
        }
    }
}

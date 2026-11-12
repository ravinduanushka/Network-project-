package lk.fraudshield.client.ui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
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
    private Label statusLabel;

    // Temporary in-memory user registry before TCP backend connection
    private static final Map<String, String> registeredUsers = new HashMap<>();

    static {
        registeredUsers.put("admin", "admin123");
        registeredUsers.put("ravindu", "pass123");
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setStyle("-fx-text-fill: #ef4444;");
            statusLabel.setText("Error: Please enter both username and password.");
            return;
        }

        if (registeredUsers.containsKey(username) && !registeredUsers.get(username).equals(password)) {
            statusLabel.setStyle("-fx-text-fill: #ef4444;");
            statusLabel.setText("Authentication Failed: Incorrect password.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("FraudShield - Customer Dashboard");
            stage.show();
        } catch (IOException e) {
            statusLabel.setStyle("-fx-text-fill: #ef4444;");
            statusLabel.setText("Error loading dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleOpenRegister() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("FraudShield - New Customer Registration");
        dialog.setHeaderText("Create a Secure Banking Account");

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
                statusLabel.setStyle("-fx-text-fill: #ef4444;");
                statusLabel.setText("Registration Error: All fields are required.");
                return;
            }

            if (!newPass.equals(confirmPass)) {
                statusLabel.setStyle("-fx-text-fill: #ef4444;");
                statusLabel.setText("Registration Error: Passwords do not match.");
                return;
            }

            if (registeredUsers.containsKey(newUser)) {
                statusLabel.setStyle("-fx-text-fill: #ef4444;");
                statusLabel.setText("Registration Error: Username already exists.");
                return;
            }

            registeredUsers.put(newUser, newPass);
            usernameField.setText(newUser);
            passwordField.clear();

            statusLabel.setStyle("-fx-text-fill: #22c55e;");
            statusLabel.setText("Account created for " + fullName + "! Enter password to login.");
        }
    }
}
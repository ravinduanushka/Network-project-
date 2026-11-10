package lk.fraudshield.client.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URL;

public class LoginController {

    @FXML private TextField serverIpField;
    @FXML private TextField serverPortField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label statusLabel;

    @FXML
    private void handleLogin(ActionEvent event) {
        System.out.println("[INFO] Connect button clicked. Loading dashboard...");

        try {
            URL dashboardFxmlUrl = getClass().getResource("/fxml/dashboard.fxml");
            if (dashboardFxmlUrl == null) {
                System.err.println("[ERROR] Cannot find /fxml/dashboard.fxml in resources!");
                return;
            }

            FXMLLoader loader = new FXMLLoader(dashboardFxmlUrl);
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle("FraudShield - Customer Dashboard");
            stage.setScene(new Scene(root, 1000, 720));
            stage.centerOnScreen();

            System.out.println("[SUCCESS] Dashboard loaded successfully.");
        } catch (Exception ex) {
            System.err.println("[ERROR] Exception loading dashboard:");
            ex.printStackTrace();
        }
    }
}
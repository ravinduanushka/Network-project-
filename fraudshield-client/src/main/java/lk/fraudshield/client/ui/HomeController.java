package lk.fraudshield.client.ui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

public class HomeController {

    @FXML
    private Button getStartedBtn;

    @FXML
    private Button signUpBtn;

    @FXML
    private VBox fraudToastBox;

    @FXML
    private void handleGetStarted() {
        navigateToLogin();
    }

    @FXML
    private void handleSignUp() {
        navigateToLogin();
    }

    @FXML
    private void handleShieldClick() {
        if (fraudToastBox != null) {
            fraudToastBox.setStyle(fraudToastBox.getStyle() + "; -fx-scale-x: 1.08; -fx-scale-y: 1.08;");
            new Thread(() -> {
                try {
                    Thread.sleep(300);
                    javafx.application.Platform.runLater(() -> {
                        fraudToastBox.setStyle(fraudToastBox.getStyle().replace("; -fx-scale-x: 1.08; -fx-scale-y: 1.08;", ""));
                    });
                } catch (InterruptedException ignored) {}
            }).start();
        }
    }

    private void navigateToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) getStartedBtn.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("FraudShield — Sign In");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

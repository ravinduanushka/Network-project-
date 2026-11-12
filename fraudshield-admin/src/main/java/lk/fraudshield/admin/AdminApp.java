package lk.fraudshield.admin;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lk.fraudshield.admin.ui.AdminController;

public class AdminApp extends Application {
    private AdminController controller;

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/admin_console.fxml"));
        Parent root = loader.load();
        controller = loader.getController();

        Scene scene = new Scene(root, 1000, 650);
        stage.setTitle("FraudShield — Security & Fraud Operations Console");
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> {
            if (controller != null) controller.shutdown();
        });
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
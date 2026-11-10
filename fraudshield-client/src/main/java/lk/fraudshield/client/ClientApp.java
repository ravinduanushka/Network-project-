package lk.fraudshield.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class ClientApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Direct testing for Dashboard view:
        URL fxmlLocation = getClass().getResource("/fxml/dashboard.fxml");

        // If testing Login view:
        // URL fxmlLocation = getClass().getResource("/fxml/login.fxml");

        if (fxmlLocation == null) {
            System.err.println("[ERROR] FXML location is null. Checking resource lookup path...");
            fxmlLocation = ClientApp.class.getResource("/fxml/dashboard.fxml");
        }

        if (fxmlLocation == null) {
            throw new IllegalStateException("FXML not found! Ensure dashboard.fxml is inside src/main/resources/fxml/");
        }

        FXMLLoader loader = new FXMLLoader(fxmlLocation);
        Parent root = loader.load();

        Scene scene = new Scene(root, 1000, 720);
        primaryStage.setTitle("FraudShield - Customer Dashboard");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(850);
        primaryStage.setMinHeight(650);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
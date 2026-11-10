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
        URL fxmlLocation = getClass().getResource("/fxml/login.fxml");

        if (fxmlLocation == null) {
            System.err.println("[ERROR] FXML location is null. Checking resource lookup path...");
            fxmlLocation = ClientApp.class.getResource("/fxml/login.fxml");
        }

        if (fxmlLocation == null) {
            throw new IllegalStateException("FXML not found! Ensure login.fxml is inside src/main/resources/fxml/");
        }

        FXMLLoader loader = new FXMLLoader(fxmlLocation);
        Parent root = loader.load();

        Scene scene = new Scene(root, 850, 650);
        primaryStage.setTitle("FraudShield - Customer Client");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(600);
        primaryStage.setMinHeight(500);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

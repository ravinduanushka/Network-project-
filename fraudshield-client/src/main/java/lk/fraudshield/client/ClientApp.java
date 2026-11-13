package lk.fraudshield.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class ClientApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Load the FraudShield Home Page on application startup
        URL fxmlLocation = getClass().getResource("/fxml/home.fxml");

        if (fxmlLocation == null) {
            fxmlLocation = ClientApp.class.getResource("/fxml/home.fxml");
        }

        // Fallback to login screen if home.fxml is not located
        if (fxmlLocation == null) {
            System.err.println("[WARN] /fxml/home.fxml not found, falling back to /fxml/login.fxml");
            fxmlLocation = getClass().getResource("/fxml/login.fxml");
        }

        if (fxmlLocation == null) {
            throw new IllegalStateException("Neither home.fxml nor login.fxml found in src/main/resources/fxml/");
        }

        FXMLLoader loader = new FXMLLoader(fxmlLocation);
        Parent root = loader.load();

        Scene scene = new Scene(root, 1180, 750);
        primaryStage.setTitle("FraudShield — Banking that guards itself");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(620);
        primaryStage.setMaximized(true);
        primaryStage.show();
        Platform.runLater(() -> primaryStage.setMaximized(true));
    }

    public static void main(String[] args) {
        launch(args);
    }
}


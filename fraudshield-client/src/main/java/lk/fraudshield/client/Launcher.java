package lk.fraudshield.client;

/**
 * Main application launcher.
 *
 * Does NOT extend javafx.application.Application.
 * This avoids the standard JavaFX runtime component check when running
 * in modern modular JDKs (Java 11+) without VM arguments.
 */
public class Launcher {

    public static void main(String[] args) {
        ClientApp.main(args);
    }
}

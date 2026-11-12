package lk.fraudshield.admin.net;

import com.google.gson.Gson;
import javafx.application.Platform;
import lk.fraudshield.admin.ui.AlertItem;
import lk.fraudshield.common.fraud.FraudAlert;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class AdminAlertListener extends Thread {
    public static final int ADMIN_PORT = 6001;
    private final Consumer<AlertItem> onAlertReceived;
    private volatile boolean running = true;
    private DatagramSocket socket;
    private final Gson gson = new Gson();

    public AdminAlertListener(Consumer<AlertItem> onAlertReceived) {
        super("AdminAlertListener-Thread");
        this.onAlertReceived = onAlertReceived;
        setDaemon(true);
    }

    @Override
    public void run() {
        try {
            socket = new DatagramSocket(ADMIN_PORT);
            byte[] buffer = new byte[2048];

            while (running) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String json = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                FraudAlert alert = gson.fromJson(json, FraudAlert.class);

                if (alert != null) {
                    String reasonsStr = alert.reasons() != null ? String.join(", ", alert.reasons()) : "";
                    AlertItem item = new AlertItem(
                            alert.txId(),
                            alert.account(),
                            alert.score(),
                            alert.decision() != null ? alert.decision().name() : "UNKNOWN",
                            reasonsStr,
                            alert.ts()
                    );

                    Platform.runLater(() -> onAlertReceived.accept(item));
                }
            }
        } catch (Exception e) {
            if (running) {
                System.err.println("Admin alert listener stopped: " + e.getMessage());
            }
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }

    public void stopListener() {
        running = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        interrupt();
    }
}
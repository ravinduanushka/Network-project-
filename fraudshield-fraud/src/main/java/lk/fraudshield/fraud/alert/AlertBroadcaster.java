package lk.fraudshield.fraud.alert;

import com.google.gson.Gson;
import lk.fraudshield.common.fraud.AlertSink;
import lk.fraudshield.common.fraud.FraudAlert;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class AlertBroadcaster extends Thread implements AlertSink {

    public static final int CLIENT_ALERT_PORT = 6000;
    public static final int ADMIN_ALERT_PORT = 6001;

    private final BlockingQueue<FraudAlert> queue = new LinkedBlockingQueue<>();
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Gson gson = new Gson();
    private DatagramSocket socket;
    private final String targetHost;

    public AlertBroadcaster() {
        this("127.0.0.1");
    }

    public AlertBroadcaster(String targetHost) {
        super("AlertBroadcaster-Worker");
        this.targetHost = targetHost;
        setDaemon(true);
        try {
            this.socket = new DatagramSocket();
        } catch (Exception e) {
            System.err.println("Could not initialize AlertBroadcaster socket: " + e.getMessage());
        }
    }

    @Override
    public void publish(FraudAlert alert) {
        if (alert != null) {
            queue.offer(alert);
        }
    }

    @Override
    public void run() {
        while (running.get() || !queue.isEmpty()) {
            try {
                FraudAlert alert = queue.take();
                sendDatagram(alert);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Error broadcasting alert: " + e.getMessage());
            }
        }
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    private void sendDatagram(FraudAlert alert) {
        if (socket == null || socket.isClosed()) return;
        try {
            String json = gson.toJson(alert);
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            InetAddress address = InetAddress.getByName(targetHost);

            // Send to customer alert port 6000
            DatagramPacket clientPacket = new DatagramPacket(bytes, bytes.length, address, CLIENT_ALERT_PORT);
            socket.send(clientPacket);

            // Send to admin console alert port 6001
            DatagramPacket adminPacket = new DatagramPacket(bytes, bytes.length, address, ADMIN_ALERT_PORT);
            socket.send(adminPacket);

        } catch (Exception e) {
            System.err.println("Failed to send UDP alert packet: " + e.getMessage());
        }
    }

    public void shutdown() {
        running.set(false);
        interrupt();
    }
}

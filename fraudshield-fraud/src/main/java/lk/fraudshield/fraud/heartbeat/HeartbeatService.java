package lk.fraudshield.fraud.heartbeat;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HeartbeatService {

    public static final int HEARTBEAT_PORT = 6002;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "FraudShield-Heartbeat");
        t.setDaemon(true);
        return t;
    });

    private DatagramSocket socket;
    private final String targetHost;

    public HeartbeatService() {
        this("127.0.0.1");
    }

    public HeartbeatService(String targetHost) {
        this.targetHost = targetHost;
        try {
            this.socket = new DatagramSocket();
        } catch (Exception e) {
            System.err.println("Could not initialize Heartbeat socket: " + e.getMessage());
        }
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::sendHeartbeat, 0, 3, TimeUnit.SECONDS);
    }

    private void sendHeartbeat() {
        if (socket == null || socket.isClosed()) return;
        try {
            long ts = System.currentTimeMillis();
            String payload = "{\"type\":\"HEARTBEAT\",\"status\":\"ONLINE\",\"ts\":" + ts + "}";
            byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
            InetAddress address = InetAddress.getByName(targetHost);
            DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, HEARTBEAT_PORT);
            socket.send(packet);
        } catch (Exception e) {
            System.err.println("Failed to broadcast UDP heartbeat: " + e.getMessage());
        }
    }

    public void stop() {
        scheduler.shutdownNow();
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}

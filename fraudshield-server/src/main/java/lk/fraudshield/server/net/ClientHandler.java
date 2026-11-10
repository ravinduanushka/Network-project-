package lk.fraudshield.server.net;

import lk.fraudshield.common.protocol.Message;
import lk.fraudshield.common.protocol.MessageType;
import lk.fraudshield.server.db.Database;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Serves one client connection: reads one JSON line, routes it, writes one JSON line back. */
public class ClientHandler implements Runnable {
    private static final Logger LOG = Logger.getLogger(ClientHandler.class.getName());
    private static final int MAX_LINE = 8192; // reject oversized lines

    private final Socket socket;
    private final Database db;

    // set after a successful LOGIN (Auth step)
    private String sessionToken;
    private Long userId;

    public ClientHandler(Socket socket, Database db) {
        this.socket = socket;
        this.db = db;
    }

    @Override
    public void run() {
        String peer = String.valueOf(socket.getRemoteSocketAddress());
        LOG.info("Client connected: " + peer);

        try (socket;
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(
                     new java.io.OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {

            String line;
            while ((line = in.readLine()) != null) {
                if (line.length() > MAX_LINE) {
                    out.println(Message.error(null, "Message too large").toJson());
                    break;
                }
                out.println(handle(line).toJson());
            }
        } catch (SocketTimeoutException e) {
            LOG.info("Idle timeout: " + peer);
        } catch (IOException e) {
            LOG.log(Level.FINE, "Connection error: " + peer, e);
        } finally {
            LOG.info("Client disconnected: " + peer);
        }
    }

    /** Parse + route. Never throws: any failure becomes an ERROR reply. */
    private Message handle(String line) {
        Message req;
        try {
            req = Message.fromJson(line);
        } catch (IllegalArgumentException e) {
            return Message.error(null, e.getMessage());
        }

        try {
            return switch (req.getType()) {
                case PING -> Message.ok(req.getReqId()).put("pong", "true");
                case REGISTER, LOGIN, LOGOUT, BALANCE, TRANSFER, HISTORY ->
                        Message.error(req.getReqId(), req.getType() + " not implemented yet");
                default -> Message.error(req.getReqId(), "Unsupported message type");
            };
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Handler failure for " + req, e);
            return Message.error(req.getReqId(), "Internal server error"); // don't leak details
        }
    }
}

package lk.fraudshield.server;

import lk.fraudshield.common.protocol.Message;
import lk.fraudshield.common.protocol.MessageType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Serves ONE client connection on a pool thread. Protocol: the client sends one JSON
 * {@link Message} per line and receives exactly one JSON line in reply.
 */
public class ClientHandler implements Runnable {

    private static final Logger LOG = Logger.getLogger(ClientHandler.class.getName());

    /** Longest accepted request line, to stop a client from exhausting server memory. */
    private static final int MAX_LINE_LENGTH = 8192;

    private final Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        String peer = String.valueOf(socket.getRemoteSocketAddress());
        LOG.info("Client connected: " + peer);

        // try-with-resources closes the reader, writer and socket on every exit path
        try (socket;
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {

            String line;
            while ((line = in.readLine()) != null) {          // null = client closed the connection
                if (line.length() > MAX_LINE_LENGTH) {
                    out.println(Message.error(null, "Request too large").toJson());
                    break;
                }
                out.println(handle(line).toJson());
            }
        } catch (SocketTimeoutException e) {
            LOG.info("Client idle too long, closing: " + peer);
        } catch (SocketException e) {
            LOG.info("Client dropped connection: " + peer);   // reset / abrupt disconnect
        } catch (IOException e) {
            LOG.log(Level.WARNING, "I/O error with client " + peer, e);
        } finally {
            LOG.info("Client disconnected: " + peer);
        }
    }

    /** Parses and routes one request. Never throws: any failure becomes an ERROR reply. */
    private Message handle(String line) {
        Message request;
        try {
            request = Message.fromJson(line);
        } catch (IllegalArgumentException e) {
            return Message.error(null, e.getMessage());
        }

        MessageType type = request.getMessageType();
        if (type == null) {
            return Message.error(request.getReqId(), "Unknown or missing message type");
        }

        try {
            return switch (type) {
                // TODO: wire these to the auth / account / transfer services
                case REGISTER, LOGIN, LOGOUT, GET_BALANCE, GET_HISTORY,
                     TRANSFER, VERIFY_OTP, ADMIN_LOGIN ->
                        Message.error(request.getReqId(), type + " not implemented yet");
                default -> Message.error(request.getReqId(), "Unsupported message type: " + type);
            };
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to handle " + request, e);
            return Message.error(request.getReqId(), "Internal server error"); // no internals leaked
        }
    }
}

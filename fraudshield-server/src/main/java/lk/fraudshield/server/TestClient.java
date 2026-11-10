package lk.fraudshield.server;

import lk.fraudshield.common.protocol.Message;
import lk.fraudshield.common.protocol.MessageType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Manual smoke test for {@link BankServer} / {@link ClientHandler}.
 *
 * <p>Start the server first (plain TCP mode), then run this class. It opens ONE connection and
 * sends two requests over it, which also proves the server keeps a connection open between
 * requests:
 * <ol>
 *   <li>REGISTER (reqId REQ-101) with a fresh username</li>
 *   <li>LOGIN    (reqId REQ-102) with the same credentials</li>
 * </ol>
 * Each request and its response are printed to the console.
 *
 * <p>Test credentials only - never use real passwords here, because requests are printed.
 */
public class TestClient {

    private static final String HOST = "localhost";
    private static final int PORT = 5000;
    private static final int TIMEOUT_MS = 5000;

    public static void main(String[] args) {
        // unique per run, so the REGISTER does not fail with "username already taken" on a re-run
        String username = "testuser" + (System.currentTimeMillis() % 100000);
        String password = "Test@1234";

        // try-with-resources closes the socket and both streams automatically, even on errors
        try (Socket socket = new Socket(HOST, PORT);
             PrintWriter out = new PrintWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            socket.setSoTimeout(TIMEOUT_MS); // never hang forever waiting for a reply
            System.out.println("Connected to " + HOST + ":" + PORT);

            // 1. REGISTER
            Message register = new Message(MessageType.REGISTER, null, "REQ-101", null)
                    .setPayload(Map.of(
                            "username", username,
                            "fullName", "Test User",
                            "password", password));
            exchange(out, in, register);

            // 2. LOGIN with the same credentials
            Message login = new Message(MessageType.LOGIN, null, "REQ-102", null)
                    .setPayload(Map.of(
                            "username", username,
                            "password", password));
            exchange(out, in, login);

        } catch (SocketTimeoutException e) {
            System.err.println("Server did not answer within " + TIMEOUT_MS + " ms: " + e.getMessage());
        } catch (IOException e) {
            // ConnectException (server not running), connection reset, etc.
            System.err.println("Could not talk to server at " + HOST + ":" + PORT + " -> " + e.getMessage());
            System.err.println("Is BankServer running in plain mode (without -Dfraudshield.tls=true)?");
        }
    }

    /** Sends one request line, then reads and prints the one-line reply. */
    private static void exchange(PrintWriter out, BufferedReader in, Message request) throws IOException {
        String requestJson = request.toJson(); // Gson serialisation happens inside Message
        System.out.println("\n>>> SENT     : " + requestJson);
        out.println(requestJson);

        String responseJson = in.readLine();
        if (responseJson == null) {
            throw new IOException("Server closed the connection before replying");
        }
        System.out.println("<<< RECEIVED : " + responseJson);

        // decode the envelope so the result is easy to read
        try {
            Message response = Message.fromJson(responseJson);
            System.out.println("    type=" + response.getType()
                    + ", reqId=" + response.getReqId()
                    + ", token=" + (response.getToken() == null ? "none" : "issued")
                    + ", data=" + response.getData());
        } catch (IllegalArgumentException e) {
            System.err.println("    (response was not a valid Message: " + e.getMessage() + ")");
        }
    }
}

package lk.fraudshield.server;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
 * Integration test: LOGIN -> validate session token -> authenticated TRANSFER, on ONE connection.
 *
 * <p>Start {@link BankServer} in plain TCP mode first. Usage (all arguments optional):
 * <pre>
 *   TestClient [username] [password] [targetAccount] [amount]
 * </pre>
 * Defaults: {@code testuser} / {@code Test@1234} / a freshly created receiver account / 500.0.
 *
 * <pre>
 * REQ-102  LOGIN     -> must return a session token (stops here if not)
 * REQ-103  TRANSFER  token + {targetAccount, amount} -> TRANSFER_RESULT
 * </pre>
 *
 * <p>Convenience for first runs (default credentials only): if the login fails because
 * {@code testuser} does not exist yet, the client registers it (REQ-101) and logs in again.
 * If no target account is given, a throwaway receiver is registered (REQ-100) to pay into.
 *
 * <p>Verify afterwards with {@code sqlite3 fraudshield.db "SELECT * FROM transactions;"}.
 * Dummy credentials only - requests are printed to the console.
 */
public class TestClient {

    private static final String HOST = "localhost";
    private static final int PORT = 5000;
    private static final int TIMEOUT_MS = 5000;

    private static final String DEFAULT_USER = "testuser";
    private static final String DEFAULT_PASSWORD = "Test@1234";

    public static void main(String[] args) {
        String username = args.length > 0 ? args[0] : DEFAULT_USER;
        String password = args.length > 1 ? args[1] : DEFAULT_PASSWORD;
        String targetAccount = args.length > 2 ? args[2] : null;
        double amount;
        try {
            amount = args.length > 3 ? Double.parseDouble(args[3]) : 500.0;
        } catch (NumberFormatException e) {
            System.err.println("Invalid amount: " + args[3]);
            return;
        }
        boolean usingDefaults = args.length == 0;

        try (Socket socket = new Socket(HOST, PORT);
             PrintWriter out = new PrintWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            socket.setSoTimeout(TIMEOUT_MS); // never hang forever waiting for a reply
            System.out.println("Connected to " + HOST + ":" + PORT);

            // ---- step 1: LOGIN and extract the session token ----
            String token = login(out, in, username, password);
            if (token == null && usingDefaults) {
                System.out.println("\nLogin failed - first run? Registering '" + username + "' and retrying.");
                exchange(out, in, register("REQ-101", username, password));
                token = login(out, in, username, password);
            }
            if (token == null) {
                System.err.println("\nNo valid session token - aborting before TRANSFER.");
                return;
            }
            System.out.println("\nSession token received (" + token.length() + " chars) - proceeding.");

            // ---- step 2: pick a target account ----
            if (targetAccount == null) {
                Message reply = exchange(out, in, register("REQ-100", "receiver" + System.currentTimeMillis() % 100000,
                        DEFAULT_PASSWORD));
                targetAccount = field(reply, "accountNo");
                if (targetAccount == null) {
                    System.err.println("\nCould not create a receiver account - aborting.");
                    return;
                }
            }

            // ---- step 3: authenticated TRANSFER (token goes in the envelope, details in the payload) ----
            exchange(out, in, new Message(MessageType.TRANSFER, token, "REQ-103", null)
                    .setPayload(Map.of("targetAccount", targetAccount, "amount", amount)));

        } catch (SocketTimeoutException e) {
            System.err.println("Server did not answer within " + TIMEOUT_MS + " ms: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Could not talk to server at " + HOST + ":" + PORT + " -> " + e.getMessage());
            System.err.println("Is BankServer running in plain mode (without -Dfraudshield.tls=true)?");
        } catch (IllegalStateException e) {
            System.err.println("Test aborted: " + e.getMessage());
        }
    }

    /**
     * Sends LOGIN (REQ-102) and returns the session token, or null if the reply is not a
     * successful LOGIN_RESULT carrying a non-blank token.
     */
    private static String login(PrintWriter out, BufferedReader in, String username, String password)
            throws IOException {
        Message reply = exchange(out, in, new Message(MessageType.LOGIN, null, "REQ-102", null)
                .setPayload(Map.of("username", username, "password", password)));

        if (reply.getMessageType() != MessageType.LOGIN_RESULT) {
            return null; // e.g. ERROR
        }
        if (!"true".equals(field(reply, "success"))) {
            return null;
        }
        String token = reply.getToken();
        return (token == null || token.isBlank()) ? null : token;
    }

    private static Message register(String reqId, String username, String password) {
        return new Message(MessageType.REGISTER, null, reqId, null)
                .setPayload(Map.of("username", username, "fullName", "Test User", "password", password));
    }

    /** Sends one request line, prints it, then reads, prints and returns the one-line reply. */
    private static Message exchange(PrintWriter out, BufferedReader in, Message request) throws IOException {
        String requestJson = request.toJson(); // Gson serialisation happens inside Message
        System.out.println("\n>>> SENT     : " + requestJson);
        out.println(requestJson);

        String responseJson = in.readLine();
        if (responseJson == null) {
            throw new IOException("Server closed the connection before replying");
        }
        System.out.println("<<< RECEIVED : " + responseJson);

        try {
            Message response = Message.fromJson(responseJson); // Gson parse into the DTO
            System.out.println("    -> " + response.getType() + " | data=" + response.getData());
            return response;
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("server sent an invalid message: " + e.getMessage());
        }
    }

    /** Reads one field from a response's JSON {@code data}; null if absent or unparsable. */
    private static String field(Message response, String name) {
        if (response.getData() == null) {
            return null;
        }
        try {
            JsonObject obj = JsonParser.parseString(response.getData()).getAsJsonObject();
            return obj.has(name) && !obj.get(name).isJsonNull() ? obj.get(name).getAsString() : null;
        } catch (RuntimeException e) {
            return null;
        }
    }
}

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
 * End-to-end smoke test: REGISTER x2 -> LOGIN -> TRANSFER, all over ONE connection.
 *
 * <p>Start {@link BankServer} in plain TCP mode first, then run this class.
 *
 * <pre>
 * REQ-100  REGISTER  receiver (we need a real account number to pay into)
 * REQ-101  REGISTER  sender
 * REQ-102  LOGIN     sender -> captures session token
 * REQ-103  TRANSFER  500.00 to the receiver, using the token
 * REQ-104  TRANSFER  999,999.00 -> must be REJECTED (insufficient funds, balance untouched)
 * </pre>
 *
 * <p>Both new accounts start with 10,000.00. After a run, verify with
 * <pre>sqlite3 fraudshield.db "SELECT account_no, balance FROM accounts;"</pre>
 * that the sender has 9,500.00 and the receiver 10,500.00, and that
 * <pre>sqlite3 fraudshield.db "SELECT * FROM transactions;"</pre>
 * has exactly ONE SUCCESS row (the rejected transfer must leave no trace).
 *
 * <p>Dummy credentials only - requests are printed to the console.
 */
public class TestClient {

    private static final String HOST = "localhost";
    private static final int PORT = 5000;
    private static final int TIMEOUT_MS = 5000;
    private static final String PASSWORD = "Test@1234";

    public static void main(String[] args) {
        // unique names so the script can be re-run against the same database
        String suffix = String.valueOf(System.currentTimeMillis() % 100000);
        String receiverName = "receiver" + suffix;
        String senderName = "sender" + suffix;

        try (Socket socket = new Socket(HOST, PORT);
             PrintWriter out = new PrintWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            socket.setSoTimeout(TIMEOUT_MS);
            System.out.println("Connected to " + HOST + ":" + PORT);

            // 1. receiver account (its account number is the transfer target)
            Message receiverReply = exchange(out, in, register("REQ-100", receiverName));
            String targetAccount = field(receiverReply, "accountNo");
            requireNotNull(targetAccount, "receiver registration did not return an accountNo");

            // 2. sender account
            exchange(out, in, register("REQ-101", senderName));

            // 3. login as sender and capture the session token
            Message loginReply = exchange(out, in, new Message(MessageType.LOGIN, null, "REQ-102", null)
                    .setPayload(Map.of("username", senderName, "password", PASSWORD)));
            String token = loginReply.getToken();
            requireNotNull(token, "login did not return a session token");

            // 4. valid transfer: token in the Message, target + amount in the payload
            exchange(out, in, new Message(MessageType.TRANSFER, token, "REQ-103", null)
                    .setPayload(Map.of("targetAccount", targetAccount, "amount", 500.0)));

            // 5. more than the balance: server must answer success=false and change nothing
            exchange(out, in, new Message(MessageType.TRANSFER, token, "REQ-104", null)
                    .setPayload(Map.of("targetAccount", targetAccount, "amount", 999_999.0)));

            System.out.println("\nDone. Check balances and the transactions table as described in the class comment.");

        } catch (SocketTimeoutException e) {
            System.err.println("Server did not answer within " + TIMEOUT_MS + " ms: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Could not talk to server at " + HOST + ":" + PORT + " -> " + e.getMessage());
            System.err.println("Is BankServer running in plain mode (without -Dfraudshield.tls=true)?");
        } catch (IllegalStateException e) {
            System.err.println("Test aborted: " + e.getMessage());
        }
    }

    private static Message register(String reqId, String username) {
        return new Message(MessageType.REGISTER, null, reqId, null)
                .setPayload(Map.of("username", username, "fullName", "Test User", "password", PASSWORD));
    }

    /** Sends one request line, prints it, reads and prints the one-line reply, and returns it decoded. */
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
            Message response = Message.fromJson(responseJson);
            System.out.println("    -> " + response.getType() + " | data=" + response.getData());
            return response;
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("server sent an invalid message: " + e.getMessage());
        }
    }

    /** Reads one field out of a response's JSON {@code data} payload; null if absent. */
    private static String field(Message response, String name) {
        if (response.getData() == null) {
            return null;
        }
        JsonObject obj = JsonParser.parseString(response.getData()).getAsJsonObject();
        return obj.has(name) && !obj.get(name).isJsonNull() ? obj.get(name).getAsString() : null;
    }

    private static void requireNotNull(Object value, String problem) {
        if (value == null) {
            throw new IllegalStateException(problem);
        }
    }
}

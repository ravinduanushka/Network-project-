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
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Integration test for authentication and the fraud rules, on ONE connection.
 *
 * <p>Start {@link BankServer} (TLS) first, then run this class from the same working directory
 * (it needs {@code keystore.jks} there, used as the trust store)
 * (optional arguments: {@code [username] [password] [targetAccount]}).
 *
 * <pre>
 * Phase 1  LOGIN (REQ-102)                       -> must return a session token, else abort
 * Test 1   TRANSFER 500      to a valid account  -> expect APPROVED
 * Test 2   TRANSFER 75,000   to a valid account  -> expect FLAGGED  (above the 50,000 limit)
 * Test 3   TRANSFER 100      to OWN username     -> expect BLOCKED  (self-transfer)
 * </pre>
 *
 * Each test prints PASS or FAIL against its expected status, and the process exits with code 1
 * if anything failed. With default credentials the user and a receiver account are created
 * automatically on the first run. Dummy credentials only: requests are printed to the console.
 */
public class TestClient {

    private static final String HOST = "localhost";
    private static final int PORT = 5000;
    private static final int TIMEOUT_MS = 5000;

    private static final String DEFAULT_USER = "testuser";
    private static final String DEFAULT_PASSWORD = "Test@1234";

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        String username = args.length > 0 ? args[0] : DEFAULT_USER;
        String password = args.length > 1 ? args[1] : DEFAULT_PASSWORD;
        String targetAccount = args.length > 2 ? args[2] : null;
        boolean usingDefaults = args.length == 0;

        try (Socket socket = openSocket();
             PrintWriter out = new PrintWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            socket.setSoTimeout(TIMEOUT_MS); // never hang forever waiting for a reply
            System.out.println("Connected to " + HOST + ":" + PORT
                    + (socket instanceof SSLSocket s ? " over " + s.getSession().getProtocol() : " (PLAINTEXT)"));

            // ================= Phase 1: authentication =================
            banner("PHASE 1 - LOGIN");
            String token = login(out, in, username, password);
            if (token == null && usingDefaults) {
                System.out.println("\nLogin failed - first run? Registering '" + username + "' and retrying.");
                exchange(out, in, register("REQ-101", username, password));
                token = login(out, in, username, password);
            }
            if (token == null) {
                System.err.println("\nNo valid session token - aborting before any TRANSFER.");
                System.exit(1);
            }
            System.out.println("\nSession token received (" + token.length() + " chars) - proceeding.");

            if (targetAccount == null) {
                // the server never exposes account numbers, so create a receiver to pay into
                Message reply = exchange(out, in, register("REQ-100",
                        "receiver" + System.currentTimeMillis() % 100000, DEFAULT_PASSWORD));
                targetAccount = field(reply, "accountNo");
                if (targetAccount == null) {
                    System.err.println("\nCould not create a receiver account - aborting.");
                    System.exit(1);
                }
            }

            // ================= Phase 2: fraud scenarios =================
            runCase(out, in, token, "TEST 1 - Normal transfer", "REQ-201",
                    targetAccount, 500.0, "APPROVED");

            runCase(out, in, token, "TEST 2 - Amount above the 50,000 limit", "REQ-202",
                    targetAccount, 75_000.0, "FLAGGED");

            runCase(out, in, token, "TEST 3 - Self-transfer (target = own username)", "REQ-203",
                    username, 100.0, "BLOCKED");

            banner("SUMMARY");
            System.out.println("Passed: " + passed + "   Failed: " + failed);

        } catch (SocketTimeoutException e) {
            System.err.println("Server did not answer within " + TIMEOUT_MS + " ms: " + e.getMessage());
            System.exit(1);
        } catch (SSLException e) {
            System.err.println("TLS handshake/session failed: " + e.getMessage());
            System.err.println("Check: keystore.jks exists in the working directory (password password123), "
                    + "it holds the server's self-signed certificate, and its CN/SAN is 'localhost'.");
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Could not talk to server at " + HOST + ":" + PORT + " -> " + e.getMessage());
            System.err.println("Is BankServer running on port " + PORT + "?");
            System.exit(1);
        }
        if (failed > 0) {
            System.exit(1);
        }
    }

    /**
     * Opens the connection: TLS by default, plaintext only with {@code -Dfraudshield.tls=false}.
     * The server certificate is verified against the truststore, found through the standard
     * {@code javax.net.ssl.trustStore*} properties (default {@code truststore.jks} / {@code changeit}).
     */
    private static Socket openSocket() throws IOException {
        // Must be set before the default SSL factory is first used. Development setup: the client
        // trusts the server's self-signed keystore directly. -D values on the command line win.
        if (System.getProperty("javax.net.ssl.trustStore") == null) {
            System.setProperty("javax.net.ssl.trustStore", "keystore.jks");
        }
        if (System.getProperty("javax.net.ssl.trustStorePassword") == null) {
            System.setProperty("javax.net.ssl.trustStorePassword", "password123");
        }

        SSLSocket ssl = (SSLSocket) SSLSocketFactory.getDefault().createSocket(); // unconnected
        try {
            ssl.connect(new InetSocketAddress(HOST, PORT), TIMEOUT_MS);
            ssl.setSoTimeout(TIMEOUT_MS); // also bounds the handshake

            SSLParameters params = ssl.getSSLParameters();
            params.setProtocols(new String[] {"TLSv1.3", "TLSv1.2"});
            // Without this, JSSE checks the certificate chain but NOT that it was issued for "localhost"
            params.setEndpointIdentificationAlgorithm("HTTPS");
            ssl.setSSLParameters(params);

            ssl.startHandshake(); // fail here, with a clear SSLException, rather than on the first read
            return ssl;
        } catch (IOException e) {
            ssl.close();
            throw e;
        }
    }

    /** Sends one TRANSFER and compares the status in the reply with the expected one. */
    private static void runCase(PrintWriter out, BufferedReader in, String token, String title, String reqId,
                                String target, double amount, String expectedStatus) throws IOException {
        banner(title + "  (expect " + expectedStatus + ")");
        Message reply = exchange(out, in, new Message(MessageType.TRANSFER, token, reqId, null)
                .setPayload(Map.of("targetAccount", target, "amount", amount)));

        String actual = field(reply, "status");
        boolean ok = reply.getMessageType() == MessageType.TRANSFER_RESULT && expectedStatus.equals(actual);
        System.out.println(ok ? "    RESULT: PASS" : "    RESULT: FAIL (got status=" + actual + ")");
        if (ok) {
            passed++;
        } else {
            failed++;
        }
    }

    /**
     * Sends LOGIN (REQ-102) and returns the session token, or null unless the reply is a
     * successful LOGIN_RESULT carrying a non-blank token.
     */
    private static String login(PrintWriter out, BufferedReader in, String username, String password)
            throws IOException {
        Message reply = exchange(out, in, new Message(MessageType.LOGIN, null, "REQ-102", null)
                .setPayload(Map.of("username", username, "password", password)));

        if (reply.getMessageType() != MessageType.LOGIN_RESULT || !"true".equals(field(reply, "success"))) {
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
        System.out.println(">>> SENT     : " + requestJson);
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
            throw new IOException("Server sent an invalid message: " + e.getMessage(), e);
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

    private static void banner(String text) {
        System.out.println("\n==== " + text + " ====");
    }
}

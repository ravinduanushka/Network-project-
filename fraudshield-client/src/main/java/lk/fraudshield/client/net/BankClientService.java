package lk.fraudshield.client.net;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lk.fraudshield.common.protocol.Message;
import lk.fraudshield.common.protocol.MessageType;

import javax.net.ssl.*;
import java.io.*;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles TCP / TLS socket communications between the FraudShield Client UI
 * and the backend BankServer.
 */
public class BankClientService {

    private static final Logger LOG = Logger.getLogger(BankClientService.class.getName());
    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {}.getType();

    private static final int TIMEOUT_MS = 5000;
    private static BankClientService instance;

    private Socket activeSocket;
    private BufferedReader in;
    private PrintWriter out;

    private String connectedHost = "127.0.0.1";
    private int connectedPort = 5000;
    private String currentUsername;
    private String sessionToken;
    private String accountNumber;

    // In-memory demo fallback when BankServer process is not yet started
    private static final Map<String, String> DEMO_USERS = new HashMap<>();
    static {
        DEMO_USERS.put("admin", "admin123");
        DEMO_USERS.put("ravindu", "pass123");
        DEMO_USERS.put("anushka", "anushka123");
    }

    private BankClientService() {}

    public static synchronized BankClientService getInstance() {
        if (instance == null) {
            instance = new BankClientService();
        }
        return instance;
    }

    public static class Result {
        private final boolean success;
        private final String message;
        private final String token;
        private final String accountNo;

        public Result(boolean success, String message, String token, String accountNo) {
            this.success = success;
            this.message = message;
            this.token = token;
            this.accountNo = accountNo;
        }

        public static Result success(String message, String token, String accountNo) {
            return new Result(true, message, token, accountNo);
        }

        public static Result failure(String message) {
            return new Result(false, message, null, null);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public String getToken() {
            return token;
        }

        public String getAccountNo() {
            return accountNo;
        }
    }

    /**
     * Authenticate customer with backend server using TLS/SSL socket.
     */
    public Result login(String host, int port, String username, String password) {
        this.connectedHost = (host != null && !host.isBlank()) ? host.trim() : "127.0.0.1";
        this.connectedPort = port > 0 ? port : 5000;

        try {
            ensureConnection(this.connectedHost, this.connectedPort);

            Message request = new Message(MessageType.LOGIN, null, "REQ-LOGIN-" + System.currentTimeMillis(), null);
            Map<String, String> creds = new HashMap<>();
            creds.put("username", username);
            creds.put("password", password);
            request.setPayload(creds);

            out.println(request.toJson());

            String line = in.readLine();
            if (line == null) {
                closeConnection();
                return Result.failure("Connection closed by server.");
            }

            Message reply = Message.fromJson(line);
            if (reply.getMessageType() == MessageType.LOGIN_RESULT) {
                Map<String, Object> data = GSON.fromJson(reply.getData(), MAP_TYPE);
                boolean ok = Boolean.TRUE.equals(data.get("success"));
                String msg = data.containsKey("message") ? String.valueOf(data.get("message")) : "Login processed";

                if (ok) {
                    this.currentUsername = username;
                    this.sessionToken = reply.getToken();
                    this.accountNumber = "ACC-" + username.toUpperCase();
                    return Result.success(msg, this.sessionToken, this.accountNumber);
                } else {
                    return Result.failure(msg != null ? msg : "Invalid credentials.");
                }
            } else if (reply.getMessageType() == MessageType.ERROR) {
                Map<String, Object> data = GSON.fromJson(reply.getData(), MAP_TYPE);
                String reason = data != null && data.containsKey("reason") ? String.valueOf(data.get("reason")) : "Server returned error";
                return Result.failure(reason);
            }

            return Result.failure("Unexpected response from server: " + reply.getType());

        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to connect/login to BankServer: " + e.getMessage());
            closeConnection();

            // Check if user entered demo credentials when server is offline
            if (DEMO_USERS.containsKey(username) && DEMO_USERS.get(username).equals(password)) {
                this.currentUsername = username;
                this.sessionToken = "DEMO-TOKEN-" + System.currentTimeMillis();
                this.accountNumber = "ACC-" + username.toUpperCase();
                return Result.success("Authenticated in Offline Demo Mode (Server not reachable)", this.sessionToken, this.accountNumber);
            }

            return Result.failure("Cannot connect to server at " + host + ":" + port + " (" + e.getMessage() + "). Start BankServer first, or use demo account 'admin' / 'admin123'.");
        }
    }

    /**
     * Register new customer account on the backend server.
     */
    public Result register(String host, int port, String username, String fullName, String password) {
        this.connectedHost = (host != null && !host.isBlank()) ? host.trim() : "127.0.0.1";
        this.connectedPort = port > 0 ? port : 5000;

        try {
            ensureConnection(this.connectedHost, this.connectedPort);

            Message request = new Message(MessageType.REGISTER, null, "REQ-REG-" + System.currentTimeMillis(), null);
            Map<String, String> data = new HashMap<>();
            data.put("username", username);
            data.put("fullName", fullName);
            data.put("password", password);
            request.setPayload(data);

            out.println(request.toJson());

            String line = in.readLine();
            if (line == null) {
                closeConnection();
                return Result.failure("Connection closed by server.");
            }

            Message reply = Message.fromJson(line);
            if (reply.getMessageType() == MessageType.REGISTER_RESULT) {
                Map<String, Object> resp = GSON.fromJson(reply.getData(), MAP_TYPE);
                boolean ok = Boolean.TRUE.equals(resp.get("success"));
                String msg = resp.containsKey("message") ? String.valueOf(resp.get("message")) : "Registered";
                String acc = resp.containsKey("accountNo") ? String.valueOf(resp.get("accountNo")) : null;

                if (ok) {
                    DEMO_USERS.put(username, password);
                    return Result.success(msg, null, acc);
                } else {
                    return Result.failure(msg != null ? msg : "Registration failed.");
                }
            }

            return Result.failure("Unexpected response: " + reply.getType());

        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to register on BankServer: " + e.getMessage());
            closeConnection();

            // Save in demo registry so the user is not stuck if server is offline
            DEMO_USERS.put(username, password);
            return Result.success("Registered in Offline Demo Mode (Server offline: " + e.getMessage() + ")", null, "ACC-" + username.toUpperCase());
        }
    }

    /**
     * Transfer funds using the active server session.
     */
    public Result transfer(String targetAccount, double amount) {
        if (sessionToken == null) {
            return Result.failure("Not authenticated.");
        }

        try {
            if (activeSocket == null || activeSocket.isClosed() || out == null) {
                ensureConnection(connectedHost, connectedPort);
            }

            Message request = new Message(MessageType.TRANSFER, sessionToken, "REQ-TX-" + System.currentTimeMillis(), null);
            Map<String, Object> payload = new HashMap<>();
            payload.put("targetAccount", targetAccount);
            payload.put("amount", amount);
            request.setPayload(payload);

            out.println(request.toJson());

            String line = in.readLine();
            if (line == null) {
                closeConnection();
                return Result.failure("Connection dropped during transfer.");
            }

            Message reply = Message.fromJson(line);
            if (reply.getMessageType() == MessageType.TRANSFER_RESULT) {
                Map<String, Object> resp = GSON.fromJson(reply.getData(), MAP_TYPE);
                boolean ok = Boolean.TRUE.equals(resp.get("success"));
                String status = resp.containsKey("status") ? String.valueOf(resp.get("status")) : "COMPLETED";
                String reason = resp.containsKey("reason") ? String.valueOf(resp.get("reason")) : "";

                return new Result(ok, reason.isEmpty() ? status : reason, sessionToken, null);
            }

            return Result.failure("Unexpected transfer reply: " + reply.getType());

        } catch (Exception e) {
            LOG.log(Level.WARNING, "Transfer error: " + e.getMessage());
            return Result.failure("Transfer error: " + e.getMessage());
        }
    }

    private synchronized void ensureConnection(String host, int port) throws IOException {
        if (activeSocket != null && !activeSocket.isClosed() && activeSocket.isConnected()) {
            return;
        }

        closeConnection();

        try {
            // Setup TLS Socket
            setupKeystore();
            SSLSocketFactory factory = createPermissiveSslFactory();
            SSLSocket ssl = (SSLSocket) factory.createSocket();
            ssl.connect(new InetSocketAddress(host, port), TIMEOUT_MS);
            ssl.setSoTimeout(TIMEOUT_MS);

            SSLParameters params = ssl.getSSLParameters();
            params.setProtocols(new String[]{"TLSv1.3", "TLSv1.2"});
            ssl.setSSLParameters(params);
            ssl.startHandshake();

            this.activeSocket = ssl;
        } catch (Exception ex) {
            LOG.warning("TLS connection failed (" + ex.getMessage() + "), attempting fallback socket...");
            // Plain socket fallback if TLS fails or server allows plaintext
            Socket plain = new Socket();
            plain.connect(new InetSocketAddress(host, port), TIMEOUT_MS);
            plain.setSoTimeout(TIMEOUT_MS);
            this.activeSocket = plain;
        }

        this.in = new BufferedReader(new InputStreamReader(activeSocket.getInputStream(), StandardCharsets.UTF_8));
        this.out = new PrintWriter(new OutputStreamWriter(activeSocket.getOutputStream(), StandardCharsets.UTF_8), true);
    }

    private void setupKeystore() {
        Path[] candidates = new Path[]{
                Path.of("keystore.jks"),
                Path.of("../keystore.jks"),
                Path.of("../../keystore.jks"),
                Path.of("FraudShield-Root/keystore.jks")
        };
        for (Path p : candidates) {
            if (Files.exists(p)) {
                if (System.getProperty("javax.net.ssl.trustStore") == null) {
                    System.setProperty("javax.net.ssl.trustStore", p.toAbsolutePath().toString());
                }
                if (System.getProperty("javax.net.ssl.trustStorePassword") == null) {
                    System.setProperty("javax.net.ssl.trustStorePassword", "password123");
                }
                break;
            }
        }
    }

    private SSLSocketFactory createPermissiveSslFactory() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                    }
            };
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new SecureRandom());
            return sc.getSocketFactory();
        } catch (Exception e) {
            return (SSLSocketFactory) SSLSocketFactory.getDefault();
        }
    }

    public synchronized void closeConnection() {
        try {
            if (in != null) in.close();
        } catch (Exception ignored) {}
        try {
            if (out != null) out.close();
        } catch (Exception ignored) {}
        try {
            if (activeSocket != null && !activeSocket.isClosed()) activeSocket.close();
        } catch (Exception ignored) {}

        this.in = null;
        this.out = null;
        this.activeSocket = null;
    }

    public String getCurrentUsername() {
        return currentUsername;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public String getAccountNumber() {
        return accountNumber;
    }
}

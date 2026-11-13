package lk.fraudshield.server;

import lk.fraudshield.common.protocol.Message;
import lk.fraudshield.common.protocol.MessageType;
import lk.fraudshield.server.dao.TransactionDao;
import lk.fraudshield.server.dao.UserDao;
import lk.fraudshield.server.security.SessionManager;
import lk.fraudshield.server.service.FraudDetector;
import lk.fraudshield.server.service.FraudDetector.FraudResult;

import javax.net.ssl.SSLException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientHandler implements Runnable {

    private static final Logger LOG = Logger.getLogger(ClientHandler.class.getName());

        private static final int MAX_LINE_LENGTH = 8192;

        private static final int READ_TIMEOUT_MS = 30_000;

        private static final SessionManager SESSIONS = new SessionManager();

        private static final FraudDetector FRAUD_DETECTOR = new FraudDetector();

    // ---- request payload shapes (deserialised by Gson) ----
    private static class RegisterRequest {
        String username;
        String fullName;
        String password;
        Double initialBalance;
    }

    private static class LoginRequest {
        String username;
        String password;
    }

    private static class TransferRequest {
        String targetAccount;
        double amount;
    }

    private final Socket socket;
    private final UserDao userDao = new UserDao();
    private final TransactionDao transactionDao = new TransactionDao();

        private String sessionToken;

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

            // Applies to every blocking read on this socket (SSLSocket included): readLine() throws
            // SocketTimeoutException after 30 s without data. Only this thread is affected.
            socket.setSoTimeout(READ_TIMEOUT_MS);

            String line;
            while ((line = in.readLine()) != null) {          // null = client closed the connection
                if (line.length() > MAX_LINE_LENGTH) {
                    out.println(Message.error(null, "Request too large").toJson());
                    break;
                }
                out.println(handle(line).toJson());
            }
        } catch (SocketTimeoutException e) {
            // Idle client. By the time we get here try-with-resources has already closed the
            // reader, writer and socket; the finally block below invalidates the session token.
            LOG.warning("Read timeout after " + READ_TIMEOUT_MS / 1000 + " s of inactivity, "
                    + "closing connection: " + peer);
        } catch (SSLException e) {
            // failed handshake (plaintext client, untrusted cert, bad protocol) or TLS alert
            LOG.warning("TLS error with " + peer + ": " + e.getMessage());
        } catch (SocketException e) {
            LOG.info("Client dropped connection: " + peer);   // reset / abrupt disconnect
        } catch (IOException e) {
            LOG.log(Level.WARNING, "I/O error with client " + peer, e);
        } finally {
            SESSIONS.invalidate(sessionToken);                // a dead connection must not keep a live token
            LOG.info("Client disconnected: " + peer);
        }
    }

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
                case REGISTER -> handleRegister(request);
                case LOGIN -> handleLogin(request);
                case TRANSFER -> handleTransfer(request);
                default -> Message.error(request.getReqId(), type + " is not supported yet");
            };
        } catch (IllegalArgumentException e) {
            return Message.error(request.getReqId(), "Malformed payload");
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to handle " + request, e);
            return Message.error(request.getReqId(), "Internal server error"); // no internals leaked
        }
    }

    // ------------------------------------------------------------------ handlers

    private Message handleRegister(Message request) {
        RegisterRequest body = request.getPayload(RegisterRequest.class);
        if (body == null) {
            return Message.error(request.getReqId(), "Missing payload");
        }

        // the server picks the id, so clients cannot choose (or collide with) someone else's id
        String userId = "U" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        double balance = (body.initialBalance != null && body.initialBalance >= 0) ? body.initialBalance : 250000.0;
        boolean ok = userDao.registerUser(userId, body.username, body.fullName, body.password, balance);

        Message reply = new Message(MessageType.REGISTER_RESULT, null, request.getReqId(), null);
        if (ok) {
            return reply.setPayload(Map.of("success", true, "message", "Registered",
                    "accountNo", "ACC-" + userId));
        }
        // one message for "taken" and "invalid" alike; no hints about existing usernames
        return reply.setPayload(Map.of("success", false, "message", "Registration failed"));
    }

    private Message handleLogin(Message request) {
        LoginRequest body = request.getPayload(LoginRequest.class);
        if (body == null) {
            return Message.error(request.getReqId(), "Missing payload");
        }

        Message reply = new Message(MessageType.LOGIN_RESULT, null, request.getReqId(), null);
        String userId = null;
        if (userDao.authenticate(body.username, body.password)) {
            userId = userDao.findUserId(body.username);
        }
        if (userId == null) {
            return reply.setPayload(Map.of("success", false, "message", "Invalid credentials or locked account"));
        }

        SESSIONS.invalidate(sessionToken);                    // replace any earlier login on this connection
        sessionToken = SESSIONS.create(userId, body.username);
        reply.setToken(sessionToken);
        return reply.setPayload(Map.of("success", true, "message", "Login successful"));
    }

    private Message handleTransfer(Message request) {
        // 1. must be logged in; the token must be one issued to this very connection
        SessionManager.Session session =
                (sessionToken != null && sessionToken.equals(request.getToken())) ? SESSIONS.get(sessionToken) : null;
        if (session == null) {
            return Message.error(request.getReqId(), "Not authenticated");
        }

        TransferRequest body = request.getPayload(TransferRequest.class);
        if (body == null || body.targetAccount == null) {
            return Message.error(request.getReqId(), "Missing payload");
        }

        // 2. fraud check BEFORE any money moves
        FraudResult verdict = FRAUD_DETECTOR.evaluateTransfer(session.username(), body.targetAccount, body.amount);
        Message reply = new Message(MessageType.TRANSFER_RESULT, null, request.getReqId(), null);

        // 3. one DAO call for every outcome: it always writes the audit row and moves money
        //    only for APPROVED. The sender is the logged-in user, never a client-supplied account.
        boolean moved = transactionDao.performTransfer(session.username(), body.targetAccount,
                body.amount, verdict.status(), verdict.riskScore());

        // 4. response: final status + human-readable reason
        String status = verdict.approved() ? (moved ? "APPROVED" : "FAILED") : verdict.status();
        String reason = !verdict.approved() ? verdict.reason()
                : moved ? "Transfer completed" : "Transfer failed: insufficient funds or invalid target account";

        LOG.info("Transfer " + status + " for " + session.username() + " (risk " + verdict.riskScore() + ")");
        return reply.setPayload(Map.of(
                "success", moved,
                "status", status,
                "reason", reason,
                "targetAccount", body.targetAccount,
                "amount", body.amount));
    }
}

package lk.fraudshield.admin.net;

import com.google.gson.Gson;
import lk.fraudshield.common.dto.AdminDecisionRequest;
import lk.fraudshield.common.protocol.Message;
import lk.fraudshield.common.protocol.MessageType;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class AdminNetworkService {

    public static final String SERVER_HOST = "127.0.0.1";
    public static final int ADMIN_PORT = 5001;

    private static final Gson GSON = new Gson();

    public static CompletableFuture<List<Map<String, Object>>> fetchFlaggedTransactionsAsync() {
        return CompletableFuture.supplyAsync(AdminNetworkService::fetchFlaggedTransactions);
    }

    public static List<Map<String, Object>> fetchFlaggedTransactions() {
        // 1. Try TCP 5001 first (live server)
        try (Socket socket = new Socket(SERVER_HOST, ADMIN_PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {

            Message msg = new Message(MessageType.GET_HISTORY, null, UUID.randomUUID().toString(), null);
            out.println(msg.toJson());

            String responseLine = in.readLine();
            if (responseLine != null) {
                Message resp = Message.fromJson(responseLine);
                if (resp.getMessageType() == MessageType.HISTORY) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> payload = resp.getPayload(Map.class);
                    if (payload != null && payload.get("transactions") instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> list = (List<Map<String, Object>>) payload.get("transactions");
                        return list;
                    }
                }
            }
        } catch (Exception e) {
            // Server offline or not yet started - fall through to local DB fallback
        }

        // 2. Local SQLite fallback (so Admin Console shows data even if BankServer is not yet running)
        return fetchFlaggedFromLocalDb();
    }

    private static List<Map<String, Object>> fetchFlaggedFromLocalDb() {
        List<Map<String, Object>> list = new ArrayList<>();
        String dbPath = findDatabasePath();
        if (dbPath == null) return list;

        try {
            Class.forName("org.sqlite.JDBC");
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT tx_id, from_acc, to_acc, amount, status, risk_score, timestamp " +
                         "FROM transactions WHERE status IN ('FLAGGED', 'HOLD', 'HELD', 'BLOCKED') " +
                         "ORDER BY timestamp DESC LIMIT 50")) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> map = new HashMap<>();
                        map.put("txId", rs.getString("tx_id"));
                        map.put("fromAcc", rs.getString("from_acc"));
                        map.put("toAcc", rs.getString("to_acc"));
                        map.put("amount", rs.getDouble("amount"));
                        map.put("status", rs.getString("status"));
                        map.put("riskScore", rs.getInt("risk_score"));
                        map.put("timestamp", rs.getLong("timestamp"));
                        list.add(map);
                    }
                }
            }
        } catch (Throwable t) {
            System.err.println("Local SQLite fallback notice: " + t.getMessage());
        }
        return list;
    }

    public static CompletableFuture<Boolean> sendDecisionAsync(String txId, String account, String decision, String adminUser) {
        return CompletableFuture.supplyAsync(() -> sendDecision(txId, account, decision, adminUser));
    }

    public static boolean sendDecision(String txId, String account, String decision, String adminUser) {
        // 1. Try sending over TCP 5001 to BankServer
        try (Socket socket = new Socket(SERVER_HOST, ADMIN_PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {

            AdminDecisionRequest reqBody = new AdminDecisionRequest(txId, account, decision, adminUser);
            Message msg = new Message(MessageType.ADMIN_DECISION, null, UUID.randomUUID().toString(), null);
            msg.setPayload(GSON.fromJson(GSON.toJson(reqBody), Map.class));

            out.println(msg.toJson());

            String responseLine = in.readLine();
            if (responseLine != null) {
                Message resp = Message.fromJson(responseLine);
                if (resp.getMessageType() == MessageType.ADMIN_DECISION_RESULT) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> payload = resp.getPayload(Map.class);
                    if (payload != null) {
                        Object successObj = payload.get("success");
                        if (Boolean.TRUE.equals(successObj)) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("TCP 5001 unreachable (" + e.getMessage() + "), checking local SQLite fallback...");
        }

        // 2. Direct SQLite fallback update
        return updateDecisionInLocalDb(txId, decision);
    }

    private static boolean updateDecisionInLocalDb(String txId, String decision) {
        String dbPath = findDatabasePath();
        if (dbPath == null) return false;

        try {
            Class.forName("org.sqlite.JDBC");
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath)) {
                conn.setAutoCommit(false);
                try {
                    String oldStatus = null;
                    String fromAcc = null;
                    String toAcc = null;
                    double amount = 0;

                    try (PreparedStatement checkPs = conn.prepareStatement(
                            "SELECT from_acc, to_acc, amount, status FROM transactions WHERE tx_id = ?")) {
                        checkPs.setString(1, txId);
                        try (ResultSet rs = checkPs.executeQuery()) {
                            if (rs.next()) {
                                fromAcc = rs.getString("from_acc");
                                toAcc = rs.getString("to_acc");
                                amount = rs.getDouble("amount");
                                oldStatus = rs.getString("status");
                            }
                        }
                    }

                    if (oldStatus == null) {
                        conn.rollback();
                        return false;
                    }

                    if ("APPROVED".equalsIgnoreCase(decision) &&
                            ("FLAGGED".equalsIgnoreCase(oldStatus) || "HOLD".equalsIgnoreCase(oldStatus) || "HELD".equalsIgnoreCase(oldStatus))) {
                        // Move funds
                        try (PreparedStatement debitPs = conn.prepareStatement(
                                "UPDATE accounts SET balance = balance - ? WHERE account_no = ? AND status = 'ACTIVE' AND balance >= ?")) {
                            debitPs.setDouble(1, amount);
                            debitPs.setString(2, fromAcc);
                            debitPs.setDouble(3, amount);
                            int debited = debitPs.executeUpdate();

                            try (PreparedStatement creditPs = conn.prepareStatement(
                                    "UPDATE accounts SET balance = balance + ? WHERE account_no = ? AND status = 'ACTIVE'")) {
                                creditPs.setDouble(1, amount);
                                creditPs.setString(2, toAcc);
                                int credited = creditPs.executeUpdate();

                                if (debited <= 0 || credited <= 0) {
                                    conn.rollback();
                                    return false;
                                }
                            }
                        }
                    } else if ("BLOCKED".equalsIgnoreCase(decision) && fromAcc != null) {
                        try (PreparedStatement freezePs = conn.prepareStatement(
                                "UPDATE accounts SET status = 'FROZEN' WHERE account_no = ?")) {
                            freezePs.setString(1, fromAcc);
                            freezePs.executeUpdate();
                        }
                    }

                    try (PreparedStatement updatePs = conn.prepareStatement(
                            "UPDATE transactions SET status = ? WHERE tx_id = ?")) {
                        updatePs.setString(1, decision.toUpperCase());
                        updatePs.setString(2, txId);
                        int rows = updatePs.executeUpdate();
                        conn.commit();
                        return rows > 0;
                    }
                } catch (Exception ex) {
                    conn.rollback();
                    throw ex;
                }
            }
        } catch (Throwable t) {
            System.err.println("Local SQLite update note: " + t.getMessage());
            return false;
        }
    }

    private static String findDatabasePath() {
        String[] candidatePaths = {
                "fraudshield.db",
                "../fraudshield.db",
                "FraudShield-Root/fraudshield.db",
                "../../fraudshield.db"
        };
        for (String p : candidatePaths) {
            File f = new File(p);
            if (f.exists() && f.isFile()) {
                return p;
            }
        }
        return null;
    }
}

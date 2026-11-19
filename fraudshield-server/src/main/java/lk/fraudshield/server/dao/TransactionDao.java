package lk.fraudshield.server.dao;

import lk.fraudshield.server.db.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TransactionDao {

    private static final Logger LOG = Logger.getLogger(TransactionDao.class.getName());

        public boolean transferFunds(String fromAcc, String toAcc, double amount) {
        return transferFunds(fromAcc, toAcc, amount, 0);
    }

        public boolean transferFunds(String fromAcc, String toAcc, double amount, int riskScore) {
        if (fromAcc == null || toAcc == null || fromAcc.equals(toAcc)
                || Double.isNaN(amount) || Double.isInfinite(amount)) {
            return false;
        }
        // keep to whole cents; rejects 0, negatives and sub-cent amounts
        double value = Math.round(amount * 100.0) / 100.0;
        if (value <= 0) {
            return false;
        }

        Connection conn = null;
        try {
            conn = Database.getConnection();
            conn.setAutoCommit(false);

            // 1. debit: succeeds only if the account is active AND has enough money
            if (!updateOne(conn,
                    "UPDATE accounts SET balance = balance - ? "
                            + "WHERE account_no = ? AND status = 'ACTIVE' AND balance >= ?",
                    value, fromAcc, value)) {
                conn.rollback();
                LOG.info("Transfer rejected (insufficient funds or bad sender account)");
                return false;
            }

            // 2. credit: receiver must exist and be active
            if (!updateOne(conn,
                    "UPDATE accounts SET balance = balance + ? "
                            + "WHERE account_no = ? AND status = 'ACTIVE'",
                    value, toAcc, null)) {
                conn.rollback();
                LOG.info("Transfer rejected (bad receiver account)");
                return false;
            }

            // 3. audit record
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO transactions (tx_id, from_acc, to_acc, amount, status, risk_score, timestamp) "
                            + "VALUES (?, ?, ?, ?, 'SUCCESS', ?, ?)")) {
                ps.setString(1, UUID.randomUUID().toString());
                ps.setString(2, fromAcc);
                ps.setString(3, toAcc);
                ps.setDouble(4, value);
                ps.setInt(5, riskScore);
                ps.setLong(6, System.currentTimeMillis());
                ps.executeUpdate();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Transfer failed, rolling back", e);
            rollbackQuietly(conn);
            return false;
        } finally {
            closeQuietly(conn);
        }
    }

        private boolean updateOne(Connection conn, String sql, double amount, String account, Double minBalance)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, amount);
            ps.setString(2, account);
            if (minBalance != null) {
                ps.setDouble(3, minBalance);
            }
            return ps.executeUpdate() == 1;
        }
    }

        public boolean performTransfer(String senderUsername, String targetAccount, double amount, String status) {
        return performTransfer(null, senderUsername, targetAccount, amount, status, 0);
    }

        public boolean performTransfer(String senderUsername, String targetAccount, double amount,
                                   String status, int riskScore) {
        return performTransfer(null, senderUsername, targetAccount, amount, status, riskScore);
    }

        public boolean performTransfer(String txId, String senderUsername, String targetAccount, double amount,
                                   String status, int riskScore) {
        if (senderUsername == null || targetAccount == null || status == null
                || Double.isNaN(amount) || Double.isInfinite(amount)
                || !(status.equals("APPROVED") || status.equals("FLAGGED") || status.equals("BLOCKED"))) {
            return false;
        }
        double value = Math.round(amount * 100.0) / 100.0; // whole cents
        if (value <= 0) {
            return false; // the table's CHECK (amount > 0) would reject the log row anyway
        }

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String senderAccount = findAccountByUsername(conn, senderUsername);
                if (senderAccount == null) {
                    conn.rollback();
                    LOG.warning("performTransfer: no account for the sender '" + senderUsername + "'");
                    return false;
                }

                String actualTarget = resolveAccount(conn, targetAccount);
                if (actualTarget == null) {
                    conn.rollback();
                    LOG.warning("performTransfer: target account not found for '" + targetAccount + "'");
                    return false;
                }

                String loggedStatus = status;
                boolean moved = false;

                if (status.equals("APPROVED")) {
                    // conditional debit: the balance check and the update are one atomic statement
                    boolean debited = updateOne(conn,
                            "UPDATE accounts SET balance = balance - ? "
                                    + "WHERE account_no = ? AND status = 'ACTIVE' AND balance >= ?",
                            value, senderAccount, value);
                    boolean credited = debited && updateOne(conn,
                            "UPDATE accounts SET balance = balance + ? "
                                    + "WHERE account_no = ? AND status = 'ACTIVE'",
                            value, actualTarget, null);

                    if (credited) {
                        moved = true;
                    } else {
                        conn.rollback();          // undo a debit whose credit failed
                        loggedStatus = "FAILED";  // insufficient funds or bad target
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO transactions (tx_id, from_acc, to_acc, amount, status, risk_score, timestamp) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                    ps.setString(1, (txId != null && !txId.isBlank()) ? txId : UUID.randomUUID().toString());
                    ps.setString(2, senderAccount);
                    ps.setString(3, actualTarget);
                    ps.setDouble(4, value);
                    ps.setString(5, loggedStatus);
                    ps.setInt(6, riskScore);
                    ps.setLong(7, System.currentTimeMillis());
                    ps.executeUpdate();
                }

                conn.commit();
                return moved;

            } catch (SQLException e) {
                // includes the case where the log insert fails, which also undoes the balance change
                LOG.log(Level.SEVERE, "performTransfer failed, rolling back", e);
                e.printStackTrace();
                rollbackQuietly(conn);
                return false;
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "performTransfer: database unavailable", e);
            e.printStackTrace();
            return false;
        }
    }

        public String findAccountByUsername(String username) {
        try (Connection conn = Database.getConnection()) {
            return findAccountByUsername(conn, username);
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Failed to find account for username: " + username, e);
            return null;
        }
    }

    public Double getBalanceByUsername(String username) {
        if (username == null) return null;
        String sql = "SELECT a.balance FROM accounts a JOIN users u ON a.user_id = u.user_id WHERE u.username = ? LIMIT 1";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : null;
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to get balance for: " + username, e);
            return null;
        }
    }

    public List<Map<String, Object>> getTransactionHistory(String username, int limit) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (username == null) return list;
        String sql = "SELECT t.tx_id, t.from_acc, t.to_acc, t.amount, t.status, t.risk_score, t.timestamp "
                + "FROM transactions t JOIN accounts a ON (t.from_acc = a.account_no OR t.to_acc = a.account_no) "
                + "JOIN users u ON a.user_id = u.user_id "
                + "WHERE u.username = ? "
                + "ORDER BY t.timestamp DESC LIMIT ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setInt(2, limit > 0 ? limit : 20);
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
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to get history for: " + username, e);
        }
        return list;
    }

    public String resolveAccount(Connection conn, String identifier) throws SQLException {
        if (identifier == null || identifier.isBlank()) return null;
        String clean = identifier.trim();

        // 1. Direct match on account_no (case-insensitive)
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT account_no FROM accounts WHERE LOWER(account_no) = LOWER(?) LIMIT 1")) {
            ps.setString(1, clean);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        }

        // 2. Match by username (e.g. "chalindu12", "anushka", "dasuni25")
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT a.account_no FROM accounts a JOIN users u ON a.user_id = u.user_id "
                        + "WHERE LOWER(u.username) = LOWER(?) LIMIT 1")) {
            ps.setString(1, clean);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        }

        // 3. Match if identifier was "ACC-<username>" (e.g. "ACC-CHALINDU12")
        if (clean.toUpperCase().startsWith("ACC-")) {
            String subUser = clean.substring(4);
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT a.account_no FROM accounts a JOIN users u ON a.user_id = u.user_id "
                            + "WHERE LOWER(u.username) = LOWER(?) LIMIT 1")) {
                ps.setString(1, subUser);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getString(1);
                }
            }
        }

        return null;
    }

    private String findAccountByUsername(Connection conn, String username) throws SQLException {
        if (username == null) return null;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT a.account_no FROM accounts a JOIN users u ON a.user_id = u.user_id "
                        + "WHERE LOWER(u.username) = LOWER(?) ORDER BY a.account_no LIMIT 1")) {
            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

        public int countRecentTransactions(String account, long sinceMillis) {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) FROM transactions WHERE from_acc = ? AND timestamp >= ?")) {
            ps.setString(1, account);
            ps.setLong(2, sinceMillis);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "countRecentTransactions failed", e);
            return Integer.MAX_VALUE;
        }
    }

        public boolean updateTransactionStatus(String txId, String newStatus, String adminUser) {
        if (txId == null || newStatus == null) return false;
        try (Connection conn = Database.getConnection()) {
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
                    LOG.warning("Transaction not found for tx_id: " + txId);
                    return false;
                }

                // If currently FLAGGED/HOLD/HELD and admin APPROVES, move the funds now
                if ("APPROVED".equalsIgnoreCase(newStatus) && 
                    ("FLAGGED".equalsIgnoreCase(oldStatus) || "HOLD".equalsIgnoreCase(oldStatus) || "HELD".equalsIgnoreCase(oldStatus))) {
                    boolean debited = updateOne(conn,
                            "UPDATE accounts SET balance = balance - ? WHERE account_no = ? AND status = 'ACTIVE' AND balance >= ?",
                            amount, fromAcc, amount);
                    boolean credited = debited && updateOne(conn,
                            "UPDATE accounts SET balance = balance + ? WHERE account_no = ? AND status = 'ACTIVE'",
                            amount, toAcc, null);
                    if (!credited) {
                        conn.rollback();
                        LOG.warning("Approval failed: Insufficient funds or receiver inactive for tx " + txId);
                        return false;
                    }
                } else if ("BLOCKED".equalsIgnoreCase(newStatus) && fromAcc != null) {
                    // Freeze sender account on block
                    try (PreparedStatement freezePs = conn.prepareStatement(
                            "UPDATE accounts SET status = 'FROZEN' WHERE account_no = ?")) {
                        freezePs.setString(1, fromAcc);
                        freezePs.executeUpdate();
                    }
                }

                try (PreparedStatement updatePs = conn.prepareStatement(
                        "UPDATE transactions SET status = ? WHERE tx_id = ?")) {
                    updatePs.setString(1, newStatus.toUpperCase());
                    updatePs.setString(2, txId);
                    int rows = updatePs.executeUpdate();
                    conn.commit();
                    LOG.info("Transaction " + txId + " updated to " + newStatus + " by admin: " + adminUser);
                    return rows > 0;
                }
            } catch (SQLException e) {
                rollbackQuietly(conn);
                LOG.log(Level.SEVERE, "Failed to update status for tx " + txId, e);
                return false;
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Database connection error in updateTransactionStatus", e);
            return false;
        }
    }

    public List<Map<String, Object>> getAllTransactions(int limit) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT tx_id, from_acc, to_acc, amount, status, risk_score, timestamp "
                + "FROM transactions ORDER BY timestamp DESC LIMIT ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit > 0 ? limit : 100);
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
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to get all transactions", e);
        }
        return list;
    }

    public List<Map<String, Object>> getFlaggedTransactions(int limit) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT tx_id, from_acc, to_acc, amount, status, risk_score, timestamp "
                + "FROM transactions WHERE status IN ('FLAGGED', 'HOLD', 'HELD', 'BLOCKED') "
                + "ORDER BY timestamp DESC LIMIT ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit > 0 ? limit : 50);
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
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to get flagged transactions", e);
        }
        // If no flagged transactions exist, return all recent transactions so Admin Console never starts empty
        if (list.isEmpty()) {
            return getAllTransactions(limit);
        }
        return list;
    }

    public void recordAlert(String txId, String accountNo, int riskScore, String decision, String reasons, long timestamp) {
        String sql = "INSERT INTO alerts (tx_id, account_no, risk_score, decision, reasons, timestamp) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, txId);
            ps.setString(2, accountNo);
            ps.setInt(3, riskScore);
            ps.setString(4, decision);
            ps.setString(5, reasons);
            ps.setLong(6, timestamp);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Failed to record alert in database: " + e.getMessage());
        }
    }

    public void logAttempt(String fromAcc, String toAcc, double amount, String status, int riskScore) {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO transactions (tx_id, from_acc, to_acc, amount, status, risk_score, timestamp) "
                             + "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, fromAcc);
            ps.setString(3, toAcc);
            ps.setDouble(4, amount);
            ps.setString(5, status);
            ps.setInt(6, riskScore);
            ps.setLong(7, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            LOG.log(Level.INFO, "Could not log " + status + " attempt: " + e.getMessage());
        }
    }

    private void rollbackQuietly(Connection conn) {
        if (conn == null) {
            return;
        }
        try {
            conn.rollback();
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Rollback failed", e);
        }
    }

    private void closeQuietly(Connection conn) {
        if (conn == null) {
            return;
        }
        try {
            conn.close();
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Closing connection failed", e);
        }
    }
}

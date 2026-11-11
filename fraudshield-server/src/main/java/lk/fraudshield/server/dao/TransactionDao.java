package lk.fraudshield.server.dao;

import lk.fraudshield.server.db.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Data access for money movement. Every transfer is one atomic database transaction. */
public class TransactionDao {

    private static final Logger LOG = Logger.getLogger(TransactionDao.class.getName());

    /**
     * Atomically moves {@code amount} from one account to another.
     *
     * <p>Debit, credit and the audit row are committed together or not at all.
     *
     * <p>The balance check is part of the debit statement itself
     * ({@code WHERE balance >= ?}). Checking in Java and then updating would let two concurrent
     * transfers both pass the check and overdraw the account; a single conditional UPDATE cannot.
     *
     * @return true if the transfer was committed; false if it was rejected or failed
     *         (invalid input, unknown/inactive account, insufficient funds, or a database error)
     */
    public boolean transferFunds(String fromAcc, String toAcc, double amount) {
        return transferFunds(fromAcc, toAcc, amount, 0);
    }

    /** Same as above, but records the fraud engine's {@code riskScore} on the transaction row. */
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

    /**
     * Runs an UPDATE with (amount, account[, amountAgain]) parameters.
     *
     * @return true if exactly one row changed
     */
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

    /** Convenience overload with risk score 0. */
    public boolean performTransfer(String senderUsername, String targetAccount, double amount, String status) {
        return performTransfer(senderUsername, targetAccount, amount, status, 0);
    }

    /**
     * Logs a transfer attempt and, only if {@code status} is "APPROVED", moves the money - all in
     * ONE database transaction.
     *
     * <ul>
     *   <li>Every attempt (APPROVED, FLAGGED, BLOCKED) gets a row in {@code transactions}.</li>
     *   <li>APPROVED: debit sender (only if active and balance >= amount), credit target (must exist
     *       and be active). If either step fails, both are undone and the row is logged as
     *       "FAILED" instead, so the log never claims money moved when it did not.</li>
     *   <li>FLAGGED / BLOCKED: only the log row is written; no balance changes.</li>
     * </ul>
     *
     * Schema mapping: the sender's account is found via {@code users.username -> accounts.user_id};
     * columns are {@code from_acc, to_acc, amount, status, risk_score} and {@code timestamp} holds
     * epoch milliseconds (the schema uses a LONG, so {@code datetime('now')} is not used).
     * The foreign keys mean an attempt to a NON-EXISTENT target cannot be logged.
     *
     * @return true only if money actually moved; false for flagged/blocked/failed attempts and
     *         for any database error (everything rolled back)
     */
    public boolean performTransfer(String senderUsername, String targetAccount, double amount,
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
                    LOG.warning("performTransfer: no account for the sender");
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
                            value, targetAccount, null);

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
                    ps.setString(1, UUID.randomUUID().toString());
                    ps.setString(2, senderAccount);
                    ps.setString(3, targetAccount);
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

    /** @return the user's account number, or null if the user or account does not exist */
    private String findAccountByUsername(Connection conn, String username) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT a.account_no FROM accounts a JOIN users u ON a.user_id = u.user_id "
                        + "WHERE u.username = ? ORDER BY a.account_no LIMIT 1")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    /**
     * Counts transactions (any status) sent FROM {@code account} since {@code sinceMillis}.
     * Used by the velocity rule.
     *
     * @return the count; on a database error {@link Integer#MAX_VALUE}, so the fraud check
     *         fails closed (blocks) rather than silently skipping the rule
     */
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

    /**
     * Writes an audit row for a transfer that was NOT executed (status FLAGGED or BLOCKED).
     * No balances change. Fails quietly if the accounts do not exist (foreign keys).
     */
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

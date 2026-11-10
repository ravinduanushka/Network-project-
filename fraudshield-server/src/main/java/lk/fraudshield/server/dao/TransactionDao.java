package lk.fraudshield.server.dao;

import lk.fraudshield.server.db.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
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
                            + "VALUES (?, ?, ?, ?, 'SUCCESS', 0, ?)")) {
                ps.setString(1, UUID.randomUUID().toString());
                ps.setString(2, fromAcc);
                ps.setString(3, toAcc);
                ps.setDouble(4, value);
                ps.setLong(5, System.currentTimeMillis());
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

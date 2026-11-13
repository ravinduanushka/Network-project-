package lk.fraudshield.server.dao;

import lk.fraudshield.server.db.Database;
import lk.fraudshield.server.security.PasswordUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Data access for user registration and login. Passwords are only ever stored salted + hashed. */
public class UserDao {

    private static final Logger LOG = Logger.getLogger(UserDao.class.getName());

    /** Opening balance given to every new customer. */
    private static final double DEFAULT_BALANCE = 10000.0;

    /** Consecutive wrong passwords before the account is locked. */
    private static final int MAX_FAILED_LOGINS = 5;

    /** Valid-looking salt used to burn the same hashing time for unknown usernames. */
    private static final String DUMMY_SALT = PasswordUtils.generateSalt();

    /**
     * Creates a user and their default account in ONE atomic transaction: either both rows exist
     * or neither does.
     *
     * @return true if committed; false on invalid input, duplicate user id / username, or any
     *         database error (the transaction is rolled back)
     */
    public boolean registerUser(String userId, String username, String fullName, String password) {
        return registerUser(userId, username, fullName, password, DEFAULT_BALANCE);
    }

    public boolean registerUser(String userId, String username, String fullName, String password, double initialBalance) {
        if (isBlank(userId) || isBlank(username) || isBlank(password)) {
            return false;
        }

        double balance = initialBalance >= 0 ? initialBalance : DEFAULT_BALANCE;

        // hash before opening the connection so the DB lock is held as briefly as possible
        String salt = PasswordUtils.generateSalt();
        String hash = PasswordUtils.hashPassword(password, salt);

        Connection conn = null;
        try {
            conn = Database.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO users (user_id, username, full_name, pw_hash, salt, failed_logins, locked) "
                            + "VALUES (?, ?, ?, ?, ?, 0, 0)")) {
                ps.setString(1, userId);
                ps.setString(2, username);
                ps.setString(3, fullName);
                ps.setString(4, hash);
                ps.setString(5, salt);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO accounts (account_no, user_id, balance, status) VALUES (?, ?, ?, 'ACTIVE')")) {
                ps.setString(1, "ACC-" + userId);
                ps.setString(2, userId);
                ps.setDouble(3, balance);
                ps.executeUpdate();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            // a UNIQUE violation (duplicate username/id) is an expected outcome, not a server fault
            LOG.log(Level.WARNING, "Registration failed for username '" + username + "': " + e.getMessage());
            rollbackQuietly(conn);
            return false;
        } finally {
            closeQuietly(conn);
        }
    }

    /**
     * Checks a username/password pair.
     *
     * <ul>
     *   <li>Locked accounts are always refused, even with the right password.</li>
     *   <li>A wrong password increments {@code failed_logins}; reaching
     *       {@value #MAX_FAILED_LOGINS} locks the account. A correct one resets the counter.</li>
     *   <li>Unknown usernames and wrong passwords look identical to the caller (same result,
     *       same hashing work), so the method does not reveal which usernames exist.</li>
     * </ul>
     *
     * @return true only if the account exists, is not locked and the password matches
     */
    public boolean authenticate(String username, String password) {
        if (isBlank(username) || password == null) {
            return false;
        }

        try (Connection conn = Database.getConnection()) {
            String storedHash = null;
            String storedSalt = null;
            boolean locked = false;
            boolean found = false;

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT pw_hash, salt, locked FROM users WHERE username = ?")) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        found = true;
                        storedHash = rs.getString("pw_hash");
                        storedSalt = rs.getString("salt");
                        locked = rs.getInt("locked") == 1;
                    }
                }
            }

            if (!found) {
                PasswordUtils.hashPassword(password, DUMMY_SALT); // equalise timing, discard result
                return false;
            }
            if (locked) {
                return false;
            }

            boolean ok = PasswordUtils.verifyPassword(password, storedHash, storedSalt);
            recordLoginAttempt(conn, username, ok);
            return ok;

        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Authentication query failed", e);
            return false; // fail closed
        }
    }

    /** Resets the failure counter on success; otherwise increments it and locks at the limit. */
    private void recordLoginAttempt(Connection conn, String username, boolean success) throws SQLException {
        String sql = success
                ? "UPDATE users SET failed_logins = 0 WHERE username = ?"
                : "UPDATE users SET failed_logins = failed_logins + 1, "
                  + "locked = CASE WHEN failed_logins + 1 >= ? THEN 1 ELSE locked END WHERE username = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (success) {
                ps.setString(1, username);
            } else {
                ps.setInt(1, MAX_FAILED_LOGINS);
                ps.setString(2, username);
            }
            ps.executeUpdate();
        }
    }

    /**
     * Looks up the internal user id for a username (used after a successful login).
     *
     * @return the user id, or null if the user does not exist or on a database error
     */
    public String findUserId(String username) {
        if (isBlank(username)) {
            return null;
        }
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT user_id FROM users WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("user_id") : null;
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "findUserId failed", e);
            return null;
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
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

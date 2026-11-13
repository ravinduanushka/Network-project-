package lk.fraudshield.server.dao;

import lk.fraudshield.server.db.Database;
import lk.fraudshield.server.security.PasswordUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserDao {

    private static final Logger LOG = Logger.getLogger(UserDao.class.getName());

        private static final double DEFAULT_BALANCE = 10000.0;

        private static final int MAX_FAILED_LOGINS = 5;

        private static final String DUMMY_SALT = PasswordUtils.generateSalt();

        public boolean registerUser(String userId, String username, String fullName, String password) {
        if (isBlank(userId) || isBlank(username) || isBlank(password)) {
            return false;
        }

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
                ps.setDouble(3, DEFAULT_BALANCE);
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

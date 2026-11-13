package lk.fraudshield.server.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

public final class Database {

    private static final Logger LOG = Logger.getLogger(Database.class.getName());

        public static final String URL = "jdbc:sqlite:fraudshield.db";

    private Database() {
        // static utility class
    }

        public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(URL);
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
            st.execute("PRAGMA busy_timeout = 5000");
        } catch (SQLException e) {
            conn.close();
            throw e;
        }
        return conn;
    }

        public static void initializeDatabase() throws SQLException {
        try (Connection conn = getConnection(); Statement st = conn.createStatement()) {
            // WAL lets readers proceed while a writer is active
            st.execute("PRAGMA journal_mode = WAL");

            st.execute("""
                    CREATE TABLE IF NOT EXISTS users (
                        user_id       TEXT PRIMARY KEY,
                        username      TEXT UNIQUE NOT NULL,
                        full_name     TEXT,
                        pw_hash       TEXT NOT NULL,
                        salt          TEXT NOT NULL,
                        failed_logins INT  NOT NULL DEFAULT 0,
                        locked        INT  NOT NULL DEFAULT 0
                    )""");

            st.execute("""
                    CREATE TABLE IF NOT EXISTS accounts (
                        account_no TEXT PRIMARY KEY,
                        user_id    TEXT NOT NULL REFERENCES users(user_id),
                        balance    REAL NOT NULL DEFAULT 0 CHECK (balance >= 0),
                        status     TEXT NOT NULL DEFAULT 'ACTIVE'
                    )""");

            st.execute("""
                    CREATE TABLE IF NOT EXISTS transactions (
                        tx_id      TEXT PRIMARY KEY,
                        from_acc   TEXT NOT NULL REFERENCES accounts(account_no),
                        to_acc     TEXT NOT NULL REFERENCES accounts(account_no),
                        amount     REAL NOT NULL CHECK (amount > 0),
                        status     TEXT NOT NULL,
                        risk_score INT  NOT NULL DEFAULT 0,
                        timestamp  LONG NOT NULL
                    )""");

            st.execute("CREATE INDEX IF NOT EXISTS idx_tx_from ON transactions(from_acc, timestamp)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_acc_user ON accounts(user_id)");

            LOG.info("Database initialised: " + URL);
        }
    }
}

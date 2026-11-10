package lk.fraudshield.server.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

/**
 * SQLite connection factory + schema bootstrap.
 * SQLite connections are cheap, so each DAO call/transaction opens its own Connection
 * (try-with-resources) which keeps threads from sharing connection state.
 * Money is stored as integer cents.
 */
public final class Database {
    private static final Logger LOG = Logger.getLogger(Database.class.getName());

    private final String url;

    public Database(String dbFile) {
        this.url = "jdbc:sqlite:" + dbFile;
    }

    /** New connection with foreign keys on and a busy timeout so writers wait instead of failing. */
    public Connection getConnection() throws SQLException {
        Connection c = DriverManager.getConnection(url);
        try (Statement st = c.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
            st.execute("PRAGMA busy_timeout = 5000");
        }
        return c;
    }

    /** Creates tables if they do not exist. Call once at startup. */
    public void init() throws SQLException {
        try (Connection c = getConnection(); Statement st = c.createStatement()) {
            st.execute("PRAGMA journal_mode = WAL");

            st.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id            INTEGER PRIMARY KEY AUTOINCREMENT,
                    username      TEXT NOT NULL UNIQUE,
                    password_hash TEXT NOT NULL,
                    salt          TEXT NOT NULL,
                    created_at    TEXT NOT NULL DEFAULT (datetime('now'))
                )""");

            st.execute("""
                CREATE TABLE IF NOT EXISTS accounts (
                    id             INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id        INTEGER NOT NULL REFERENCES users(id),
                    account_no     TEXT NOT NULL UNIQUE,
                    balance_cents  INTEGER NOT NULL DEFAULT 0 CHECK (balance_cents >= 0)
                )""");

            st.execute("""
                CREATE TABLE IF NOT EXISTS transactions (
                    id            INTEGER PRIMARY KEY AUTOINCREMENT,
                    from_account  INTEGER NOT NULL REFERENCES accounts(id),
                    to_account    INTEGER NOT NULL REFERENCES accounts(id),
                    amount_cents  INTEGER NOT NULL CHECK (amount_cents > 0),
                    status        TEXT NOT NULL,           -- COMPLETED | FLAGGED | REJECTED
                    created_at    TEXT NOT NULL DEFAULT (datetime('now'))
                )""");

            st.execute("CREATE INDEX IF NOT EXISTS idx_tx_from ON transactions(from_account, created_at)");
            LOG.info("Database ready: " + url);
        }
    }
}

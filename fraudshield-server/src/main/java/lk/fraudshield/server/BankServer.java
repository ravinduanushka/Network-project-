package lk.fraudshield.server;

import lk.fraudshield.server.db.Database;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.BindException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * FraudShield server entry point. Accepts TLS/SSL clients on port 5000 and hands each
 * {@link SSLSocket} to a fixed pool of worker threads running {@link ClientHandler}.
 *
 * <p>KeyStore: {@code keystore.jks} / {@code password123} in the working directory, unless overridden
 * with {@code -Djavax.net.ssl.keyStore=...} and {@code -Djavax.net.ssl.keyStorePassword=...}.
 * (Hard-coded password is acceptable for the university project only.)
 */
public class BankServer {

    private static final Logger LOG = Logger.getLogger(BankServer.class.getName());

    private static final int PORT = 5000;
    private static final int POOL_SIZE = 20;
    private static final int HANDSHAKE_TIMEOUT_MS = 10_000;
    private static final int CLIENT_TIMEOUT_MS = 30_000; // same as ClientHandler.READ_TIMEOUT_MS

    private final ExecutorService pool = Executors.newFixedThreadPool(POOL_SIZE);
    private volatile boolean running = true;
    private volatile SSLServerSocket serverSocket;

    public static void main(String[] args) {
        try {
            configureKeyStore();
            Database.initializeDatabase();
            new BankServer().start();
        } catch (BindException e) {
            LOG.log(Level.SEVERE, "Cannot bind to port " + PORT + " - is another server already running?", e);
            System.exit(1);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Server failed to start", e);
            System.exit(1);
        }
    }

    /** Must run before the first use of the default SSL factory. Command-line -D values win. */
    private static void configureKeyStore() throws IOException {
        if (System.getProperty("javax.net.ssl.keyStore") == null) {
            System.setProperty("javax.net.ssl.keyStore", "keystore.jks");
        }
        if (System.getProperty("javax.net.ssl.keyStorePassword") == null) {
            System.setProperty("javax.net.ssl.keyStorePassword", "password123");
        }
        Path ks = Path.of(System.getProperty("javax.net.ssl.keyStore"));
        if (!Files.isReadable(ks)) {
            throw new IOException("Keystore not found or unreadable: " + ks.toAbsolutePath());
        }
    }

    /** Blocks, accepting connections until {@link #stop()} is called. */
    public void start() throws IOException {
        SSLServerSocketFactory factory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        SSLServerSocket ss = (SSLServerSocket) factory.createServerSocket(PORT); // may throw BindException
        ss.setEnabledProtocols(new String[] {"TLSv1.3", "TLSv1.2"});             // no legacy protocols
        serverSocket = ss;
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

        LOG.info("Secure FraudShield server listening on port " + PORT + " (TLS/SSL), pool size " + POOL_SIZE);

        while (running) {
            SSLSocket client = null;
            try {
                client = (SSLSocket) ss.accept(); // TCP accepted; TLS handshake happens on the worker thread
                client.setSoTimeout(HANDSHAKE_TIMEOUT_MS);
                final SSLSocket accepted = client;
                pool.execute(() -> serve(accepted));
            } catch (RejectedExecutionException e) {
                closeQuietly(client); // pool shut down while accepting
            } catch (IOException e) {
                closeQuietly(client);
                if (running) {
                    LOG.log(Level.WARNING, "Failed to accept connection", e);
                }
            } catch (RuntimeException e) {
                closeQuietly(client);
                LOG.log(Level.SEVERE, "Unexpected error in accept loop", e);
            }
        }
    }

    /** Worker-thread entry: complete the handshake (bounded by a timeout), then run the handler. */
    private void serve(SSLSocket client) {
        String peer = String.valueOf(client.getRemoteSocketAddress());
        try {
            client.startHandshake();
            client.setSoTimeout(CLIENT_TIMEOUT_MS);
            LOG.info("TLS handshake OK with " + peer + " (" + client.getSession().getProtocol() + ", "
                    + client.getSession().getCipherSuite() + ")");
        } catch (SSLException e) {
            LOG.log(Level.WARNING, "TLS handshake failed with " + peer + ": " + e.getMessage());
            closeQuietly(client);
            return;
        } catch (IOException e) {
            LOG.log(Level.WARNING, "I/O error during handshake with " + peer + ": " + e.getMessage());
            closeQuietly(client);
            return;
        }
        try {
            new ClientHandler(client).run();
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Unhandled error serving " + peer, e);
        } finally {
            closeQuietly(client);
        }
    }

    /** Idempotent; stops accepting and lets running handlers finish (up to 5 s). */
    public void stop() {
        running = false;
        closeQuietly(serverSocket);
        pool.shutdown();
        try {
            if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private static void closeQuietly(java.io.Closeable c) {
        if (c == null) {
            return;
        }
        try {
            c.close();
        } catch (IOException ignored) {
            // nothing useful to do
        }
    }
}

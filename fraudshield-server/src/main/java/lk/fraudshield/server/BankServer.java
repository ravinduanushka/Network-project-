package lk.fraudshield.server;

import lk.fraudshield.server.db.Database;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server entry point: initialises the database, then accepts clients in a loop and hands each
 * connection to a fixed pool of 20 worker threads.
 *
 * <p><b>TLS is ON by default</b> (port 5000). The server certificate comes from a keystore,
 * located through the standard JSSE system properties:
 * <pre>
 *   -Djavax.net.ssl.keyStore=keystore.jks  -Djavax.net.ssl.keyStorePassword=changeit
 * </pre>
 * If they are not given, {@code keystore.jks} / {@code changeit} in the working directory are used
 * (development only). Start with {@code -Dfraudshield.tls=false} to fall back to plaintext for
 * debugging.
 *
 * <p>The TLS layer is invisible to {@link ClientHandler}: an {@code SSLSocket} is a
 * {@code Socket}, so the JSON protocol and the thread pool are unchanged.
 */
public class BankServer {

    private static final Logger LOG = Logger.getLogger(BankServer.class.getName());

    private static final int PORT = 5000;
    private static final int POOL_SIZE = 20;
    /** Also bounds the TLS handshake, so a client that stalls mid-handshake cannot hold a thread. */
    private static final int CLIENT_TIMEOUT_MS = 5 * 60 * 1000;

    private final ExecutorService pool = Executors.newFixedThreadPool(POOL_SIZE);
    private volatile boolean running = true;
    private ServerSocket serverSocket;

    public static void main(String[] args) {
        try {
            Database.initializeDatabase();
            new BankServer().start();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Server failed to start", e);
            System.exit(1);
        }
    }

    /** Blocks, accepting connections until {@link #stop()} is called. */
    public void start() throws Exception {
        boolean tls = !"false".equalsIgnoreCase(System.getProperty("fraudshield.tls", "true"));
        serverSocket = tls ? createTlsServerSocket() : new ServerSocket(PORT);
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

        LOG.info("FraudShield server listening on port " + PORT
                + (tls ? " (TLS)" : " (PLAINTEXT - debug only)") + ", pool size " + POOL_SIZE);

        while (running) {
            try {
                // For TLS this returns an SSLSocket. The handshake has NOT happened yet: it runs on
                // the worker thread at the first read, so a slow client never blocks accept().
                Socket client = serverSocket.accept();
                client.setSoTimeout(CLIENT_TIMEOUT_MS);
                pool.execute(new ClientHandler(client));
            } catch (IOException e) {
                // accept() throws when the socket is closed during shutdown - that is expected
                if (running) {
                    LOG.log(Level.WARNING, "Failed to accept connection", e);
                }
            }
        }
    }

    /** Stops accepting connections and lets running handlers finish (up to 5 s). */
    public void stop() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
            // nothing useful to do while shutting down
        }
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

    /** Builds the SSLServerSocket from the JVM's default SSLContext (configured via system properties). */
    private ServerSocket createTlsServerSocket() throws IOException {
        // These must be set BEFORE the first use of the default SSL factory.
        String keystore = System.getProperty("javax.net.ssl.keyStore");
        if (keystore == null) {
            keystore = "keystore.jks";
            System.setProperty("javax.net.ssl.keyStore", keystore);
        }
        if (System.getProperty("javax.net.ssl.keyStorePassword") == null) {
            System.setProperty("javax.net.ssl.keyStorePassword", "changeit");
            LOG.warning("Using the DEFAULT development keystore password - set "
                    + "-Djavax.net.ssl.keyStorePassword for anything beyond local testing");
        }
        if (!Files.isReadable(Path.of(keystore))) {
            throw new IOException("Keystore not found: " + Path.of(keystore).toAbsolutePath()
                    + " (generate it with keytool, see the project notes)");
        }

        SSLServerSocketFactory factory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        SSLServerSocket ssl = (SSLServerSocket) factory.createServerSocket(PORT);
        ssl.setEnabledProtocols(new String[] {"TLSv1.3", "TLSv1.2"}); // no SSLv3 / TLS 1.0 / 1.1
        return ssl;
    }
}

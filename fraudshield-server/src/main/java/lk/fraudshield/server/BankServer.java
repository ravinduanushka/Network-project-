package lk.fraudshield.server;

import lk.fraudshield.server.db.Database;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server entry point: initialises the database, then accepts clients in a loop and hands each
 * connection to a fixed pool of 20 worker threads.
 *
 * <p>Listens on plain TCP port 5000 by default. For TLS (required for the real deployment)
 * start the JVM with:
 * <pre>
 *   -Dfraudshield.tls=true -Dfraudshield.keystore=keystore.p12 -Dfraudshield.keystore.password=changeit
 * </pre>
 */
public class BankServer {

    private static final Logger LOG = Logger.getLogger(BankServer.class.getName());

    private static final int PORT = 5000;
    private static final int POOL_SIZE = 20;
    /** Drop clients that stay silent for 5 minutes so threads are not held forever. */
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
        boolean tls = Boolean.getBoolean("fraudshield.tls");
        serverSocket = tls ? createTlsServerSocket() : new ServerSocket(PORT);
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

        LOG.info("FraudShield server listening on port " + PORT
                + (tls ? " (TLS)" : " (PLAINTEXT)") + ", pool size " + POOL_SIZE);

        while (running) {
            try {
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

    private ServerSocket createTlsServerSocket() throws Exception {
        String keystore = System.getProperty("fraudshield.keystore", "keystore.p12");
        char[] password = System.getProperty("fraudshield.keystore.password", "changeit").toCharArray();

        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (InputStream in = Files.newInputStream(Path.of(keystore))) {
            ks.load(in, password);
        }
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, password);

        SSLContext ctx = SSLContext.getInstance("TLSv1.3");
        ctx.init(kmf.getKeyManagers(), null, null);
        return ctx.getServerSocketFactory().createServerSocket(PORT);
    }
}

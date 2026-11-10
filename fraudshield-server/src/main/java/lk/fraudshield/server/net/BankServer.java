package lk.fraudshield.server.net;

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
 * Multi-threaded TLS server. One pooled worker thread per connected client.
 *
 * Run with:
 *   -Dfraudshield.keystore=keystore.p12 -Dfraudshield.keystore.password=changeit
 * Optional: -Dfraudshield.port=5000  -Dfraudshield.db=fraudshield.db
 * For local debugging ONLY, plain TCP: -Dfraudshield.tls=false
 */
public class BankServer {
    private static final Logger LOG = Logger.getLogger(BankServer.class.getName());
    private static final int SOCKET_TIMEOUT_MS = 5 * 60 * 1000;

    private final int port;
    private final Database db;
    private final boolean useTls;
    private final ExecutorService pool = Executors.newFixedThreadPool(50);
    private volatile boolean running = true;
    private ServerSocket serverSocket;

    public BankServer(int port, Database db, boolean useTls) {
        this.port = port;
        this.db = db;
        this.useTls = useTls;
    }

    public void start() throws Exception {
        serverSocket = useTls ? createTlsSocket() : new ServerSocket(port);
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
        LOG.info("FraudShield server listening on port " + port + (useTls ? " (TLS)" : " (PLAINTEXT - debug only)"));

        while (running) {
            try {
                Socket client = serverSocket.accept();
                client.setSoTimeout(SOCKET_TIMEOUT_MS);
                pool.execute(new ClientHandler(client, db));
            } catch (IOException e) {
                if (running) {
                    LOG.log(Level.WARNING, "accept failed", e);
                }
            }
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ignored) { }
        pool.shutdown();
        try {
            if (!pool.awaitTermination(5, TimeUnit.SECONDS)) pool.shutdownNow();
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private ServerSocket createTlsSocket() throws Exception {
        String ksPath = System.getProperty("fraudshield.keystore", "keystore.p12");
        char[] ksPass = System.getProperty("fraudshield.keystore.password", "changeit").toCharArray();

        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (InputStream in = Files.newInputStream(Path.of(ksPath))) {
            ks.load(in, ksPass);
        }
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, ksPass);

        SSLContext ctx = SSLContext.getInstance("TLSv1.3");
        ctx.init(kmf.getKeyManagers(), null, null);
        return ctx.getServerSocketFactory().createServerSocket(port);
    }

    public static void main(String[] args) throws Exception {
        int port = Integer.getInteger("fraudshield.port", 5000);
        String dbFile = System.getProperty("fraudshield.db", "fraudshield.db");
        boolean tls = !"false".equalsIgnoreCase(System.getProperty("fraudshield.tls", "true"));

        Database db = new Database(dbFile);
        db.init();
        new BankServer(port, db, tls).start();
    }
}

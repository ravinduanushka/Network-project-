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

public class BankServer {

    private static final Logger LOG = Logger.getLogger(BankServer.class.getName());

    private static final int PORT = 5000;
    private static final int POOL_SIZE = 20;
    private static final int HANDSHAKE_TIMEOUT_MS = 10_000;
    private static final int CLIENT_TIMEOUT_MS = 30 * 60 * 1000; // 30 minutes persistent session

    private final ExecutorService pool = Executors.newFixedThreadPool(POOL_SIZE);
    private volatile boolean running = true;
    private volatile SSLServerSocket serverSocket;

    public static void main(String[] args) {
        try {
            configureKeyStore();
            Database.initializeDatabase();
            seedDefaultAccounts();
            new BankServer().start();
        } catch (BindException e) {
            LOG.log(Level.SEVERE, "Cannot bind to port " + PORT + " - is another server already running?", e);
            System.exit(1);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Server failed to start", e);
            System.exit(1);
        }
    }

    private static void seedDefaultAccounts() {
        lk.fraudshield.server.dao.UserDao userDao = new lk.fraudshield.server.dao.UserDao();
        if (userDao.findUserId("admin") == null) {
            userDao.registerUser("U000000001", "admin", "System Administrator", "admin123", 1000000.0);
            LOG.info("Seeded default account: admin");
        }
        if (userDao.findUserId("testuser") == null) {
            userDao.registerUser("U000000002", "testuser", "Test User", "Test@1234", 250000.0);
            LOG.info("Seeded default account: testuser");
        }
        if (userDao.findUserId("anushka") == null) {
            userDao.registerUser("U000000003", "anushka", "Anushka Silva", "anushka123", 250000.0);
            LOG.info("Seeded default account: anushka");
        }
        if (userDao.findUserId("ravindu") == null) {
            userDao.registerUser("U000000004", "ravindu", "Ravindu Anushka", "pass123", 250000.0);
            LOG.info("Seeded default account: ravindu");
        }
        if (userDao.findUserId("dasuni") == null) {
            userDao.registerUser("U000000005", "dasuni", "Dasuni Tharakie", "dasuni1234", 250000.0);
            LOG.info("Seeded default account: dasuni");
        }
        if (userDao.findUserId("dasuni25") == null) {
            userDao.registerUser("U000000006", "dasuni25", "Dasuni Tharakie", "dasuni1234", 250000.0);
            LOG.info("Seeded default account: dasuni25");
        }
    }

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

        public void start() throws IOException {
        SSLServerSocketFactory factory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        SSLServerSocket ss = (SSLServerSocket) factory.createServerSocket(PORT); // may throw BindException
        ss.setEnabledProtocols(new String[] {"TLSv1.3", "TLSv1.2"});             // no legacy protocols
        serverSocket = ss;
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

        LOG.info("Secure FraudShield server listening on port " + PORT + " (TLS/SSL), pool size " + POOL_SIZE);

        new lk.fraudshield.server.net.AdminChannelServer().start();
        startHeartbeat();

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

    private void startHeartbeat() {
        Thread t = new Thread(() -> {
            try (java.net.DatagramSocket ds = new java.net.DatagramSocket()) {
                byte[] data = "{\"type\":\"HEARTBEAT\",\"status\":\"ONLINE\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
                java.net.InetAddress addr = java.net.InetAddress.getByName("127.0.0.1");
                java.net.DatagramPacket packet = new java.net.DatagramPacket(data, data.length, addr, 6002);
                while (running) {
                    try {
                        ds.send(packet);
                        Thread.sleep(3000);
                    } catch (Exception ignored) {}
                }
            } catch (Exception e) {
                LOG.warning("Heartbeat service disabled: " + e.getMessage());
            }
        }, "BankServer-Heartbeat");
        t.setDaemon(true);
        t.start();
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

package lk.fraudshield.server;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lk.fraudshield.common.protocol.Message;
import lk.fraudshield.common.protocol.MessageType;

import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Load test: many clients, each on its own TLS connection, hit {@link BankServer} at the same time.
 *
 * <p>Start the server first, then run from the directory containing {@code keystore.jks}
 * (used as the trust store). Optional arguments: {@code [clients=50] [poolThreads=20]}.
 *
 * <p>Every client opens a fresh TLS connection, sends one LOGIN request, reads the one-line JSON
 * reply, closes the socket and records its latency. A request counts as successful when the server
 * answers with a LOGIN_RESULT carrying {@code success=true}. The test user is registered once
 * before the load starts. Dummy credentials only.
 */
public class StressTestClient {

    private static final String HOST = "localhost";
    private static final int PORT = 5000;
    private static final int TIMEOUT_MS = 10_000;

    private static final String USER = "stressuser";
    private static final String PASSWORD = "Stress@1234";

    /** Outcome of one client task. */
    private record Result(int id, boolean ok, long latencyMs, String detail) { }

    public static void main(String[] args) throws InterruptedException {
        int clients = clampArg(args, 0, 50, 1, 500);
        int threads = clampArg(args, 1, 20, 1, 200);

        // Must be set before the default SSL factory is first used; -D values win.
        if (System.getProperty("javax.net.ssl.trustStore") == null) {
            System.setProperty("javax.net.ssl.trustStore", "keystore.jks");
        }
        if (System.getProperty("javax.net.ssl.trustStorePassword") == null) {
            System.setProperty("javax.net.ssl.trustStorePassword", "password123");
        }

        // Setup (not timed): make sure the account exists. "Already exists" is fine.
        try {
            roundTrip(new Message(MessageType.REGISTER, null, "SETUP", null)
                    .setPayload(Map.of("username", USER, "fullName", "Stress User", "password", PASSWORD)));
        } catch (IOException e) {
            System.err.println("Cannot reach the secure server at " + HOST + ":" + PORT + " -> " + e);
            System.err.println("Is BankServer running, and is keystore.jks in the working directory?");
            System.exit(1);
        }

        System.out.println("Starting " + clients + " clients on " + threads + " threads against "
                + HOST + ":" + PORT + " ...");

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Callable<Result>> tasks = new ArrayList<>();
        for (int i = 1; i <= clients; i++) {
            final int id = i;
            tasks.add(() -> runClient(id));
        }

        long start = System.nanoTime();
        List<Future<Result>> futures;
        try {
            futures = pool.invokeAll(tasks); // blocks until every task has finished
        } finally {
            pool.shutdown();
        }
        long totalMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        pool.awaitTermination(5, TimeUnit.SECONDS);

        report(futures, clients, totalMs);
    }

    /** One client: connect, send LOGIN, read the reply, close. Never throws. */
    private static Result runClient(int id) {
        long t0 = System.nanoTime();
        try {
            Message reply = roundTrip(new Message(MessageType.LOGIN, null, "STRESS-" + id, null)
                    .setPayload(Map.of("username", USER, "password", PASSWORD)));
            long ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);

            boolean ok = reply.getMessageType() == MessageType.LOGIN_RESULT
                    && "true".equals(field(reply, "success"));
            String detail = ok ? "login ok" : "unexpected reply: " + reply.getType() + " " + reply.getData();
            System.out.printf("[client %02d] %s in %d ms (%s)%n", id, ok ? "OK  " : "FAIL", ms, detail);
            return new Result(id, ok, ms, detail);
        } catch (Exception e) {
            long ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
            String detail = e.getClass().getSimpleName() + ": " + e.getMessage();
            System.out.printf("[client %02d] FAIL in %d ms (%s)%n", id, ms, detail);
            return new Result(id, false, ms, detail);
        }
    }

    /** Opens a TLS connection, sends one request line, reads one reply line, closes everything. */
    private static Message roundTrip(Message request) throws IOException {
        SSLSocket socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(); // unconnected
        try (socket) {
            socket.connect(new InetSocketAddress(HOST, PORT), TIMEOUT_MS);
            socket.setSoTimeout(TIMEOUT_MS); // bounds the handshake and every read

            SSLParameters params = socket.getSSLParameters();
            params.setProtocols(new String[] {"TLSv1.3", "TLSv1.2"});
            params.setEndpointIdentificationAlgorithm("HTTPS"); // verify the cert matches "localhost"
            socket.setSSLParameters(params);
            socket.startHandshake();

            try (PrintWriter out = new PrintWriter(
                         new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
                 BufferedReader in = new BufferedReader(
                         new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

                out.println(request.toJson());
                String line = in.readLine();
                if (line == null) {
                    throw new IOException("Server closed the connection before replying");
                }
                try {
                    return Message.fromJson(line);
                } catch (IllegalArgumentException e) {
                    throw new IOException("Invalid JSON reply: " + e.getMessage(), e);
                }
            }
        }
    }

    private static void report(List<Future<Result>> futures, int clients, long totalMs) {
        int ok = 0;
        int failed = 0;
        long sum = 0;
        long min = Long.MAX_VALUE;
        long max = 0;
        List<Long> okLatencies = new ArrayList<>();

        for (Future<Result> f : futures) {
            try {
                Result r = f.get();
                if (r.ok()) {
                    ok++;
                    okLatencies.add(r.latencyMs());
                    sum += r.latencyMs();
                    min = Math.min(min, r.latencyMs());
                    max = Math.max(max, r.latencyMs());
                } else {
                    failed++;
                }
            } catch (Exception e) { // task cancelled or crashed: count it as failed
                failed++;
            }
        }

        System.out.println("\n==== STRESS TEST SUMMARY ====");
        System.out.println("Total requests sent : " + clients);
        System.out.println("Successful responses: " + ok);
        System.out.println("Failed requests     : " + failed);
        System.out.println("Total execution time: " + totalMs + " ms");
        if (!okLatencies.isEmpty()) {
            okLatencies.sort(null);
            long p95 = okLatencies.get(Math.min(okLatencies.size() - 1, (int) Math.ceil(okLatencies.size() * 0.95) - 1));
            System.out.printf("Latency (successful): min %d ms | avg %d ms | p95 %d ms | max %d ms%n",
                    min, sum / ok, p95, max);
        }
        if (totalMs > 0) {
            System.out.printf("Throughput          : %.1f requests/s%n", clients * 1000.0 / totalMs);
        }
        if (failed > 0) {
            System.exit(1);
        }
    }

    /** Reads one field from a response's JSON {@code data}; null if absent or unparsable. */
    private static String field(Message response, String name) {
        if (response.getData() == null) {
            return null;
        }
        try {
            JsonObject obj = JsonParser.parseString(response.getData()).getAsJsonObject();
            return obj.has(name) && !obj.get(name).isJsonNull() ? obj.get(name).getAsString() : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static int clampArg(String[] args, int index, int def, int min, int max) {
        if (args.length <= index) {
            return def;
        }
        try {
            return Math.max(min, Math.min(max, Integer.parseInt(args[index])));
        } catch (NumberFormatException e) {
            return def;
        }
    }
}

package lk.fraudshield.server.net;

import com.google.gson.Gson;
import lk.fraudshield.common.dto.AdminDecisionRequest;
import lk.fraudshield.common.protocol.Message;
import lk.fraudshield.common.protocol.MessageType;
import lk.fraudshield.server.dao.TransactionDao;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AdminChannelServer extends Thread {

    private static final Logger LOG = Logger.getLogger(AdminChannelServer.class.getName());
    public static final int ADMIN_PORT = 5001;

    private final ExecutorService pool = Executors.newCachedThreadPool();
    private final TransactionDao transactionDao = new TransactionDao();
    private final Gson gson = new Gson();
    private volatile boolean running = true;
    private ServerSocket serverSocket;

    public AdminChannelServer() {
        super("AdminChannelServer-Listener");
        setDaemon(true);
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(ADMIN_PORT);
            LOG.info("Admin TCP Action Channel listening on port " + ADMIN_PORT);

            while (running) {
                Socket client = serverSocket.accept();
                pool.execute(() -> handleClient(client));
            }
        } catch (Exception e) {
            if (running) {
                LOG.log(Level.SEVERE, "AdminChannelServer stopped: " + e.getMessage(), e);
            }
        } finally {
            closeSocket();
        }
    }

    private void handleClient(Socket socket) {
        try (socket;
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {

            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                Message response = processMessage(line);
                out.println(response.toJson());
            }
        } catch (Exception e) {
            LOG.log(Level.FINE, "Admin client disconnected: " + e.getMessage());
        }
    }

    private Message processMessage(String jsonLine) {
        try {
            Message req = Message.fromJson(jsonLine);
            if (req.getMessageType() == MessageType.ADMIN_DECISION) {
                AdminDecisionRequest body = req.getPayload(AdminDecisionRequest.class);
                if (body != null) {
                    boolean ok = transactionDao.updateTransactionStatus(body.getTxId(), body.getDecision(), body.getAdminUser());
                    Message reply = new Message(MessageType.ADMIN_DECISION_RESULT, null, req.getReqId(), null);
                    return reply.setPayload(Map.of(
                            "success", ok,
                            "txId", body.getTxId() != null ? body.getTxId() : "",
                            "decision", body.getDecision() != null ? body.getDecision() : "",
                            "message", ok ? "Transaction status updated successfully" : "Update failed or tx not found"
                    ));
                }
            } else if (req.getMessageType() == MessageType.GET_HISTORY) {
                java.util.List<Map<String, Object>> flagged = transactionDao.getFlaggedTransactions(50);
                Message reply = new Message(MessageType.HISTORY, null, req.getReqId(), null);
                return reply.setPayload(Map.of("transactions", flagged));
            }
            return Message.error(req.getReqId(), "Unsupported admin message");
        } catch (Exception e) {
            return Message.error(null, "Malformed admin request: " + e.getMessage());
        }
    }

    public void stopServer() {
        running = false;
        closeSocket();
        pool.shutdownNow();
    }

    private void closeSocket() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (Exception ignored) {}
    }
}

package lk.fraudshield.admin.net;

import com.google.gson.Gson;
import lk.fraudshield.common.dto.AdminDecisionRequest;
import lk.fraudshield.common.protocol.Message;
import lk.fraudshield.common.protocol.MessageType;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class AdminNetworkService {

    public static final String SERVER_HOST = "127.0.0.1";
    public static final int ADMIN_PORT = 5001;

    private static final Gson GSON = new Gson();

    public static CompletableFuture<Boolean> sendDecisionAsync(String txId, String account, String decision, String adminUser) {
        return CompletableFuture.supplyAsync(() -> sendDecision(txId, account, decision, adminUser));
    }

    public static boolean sendDecision(String txId, String account, String decision, String adminUser) {
        try (Socket socket = new Socket(SERVER_HOST, ADMIN_PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {

            AdminDecisionRequest reqBody = new AdminDecisionRequest(txId, account, decision, adminUser);
            Message msg = new Message(MessageType.ADMIN_DECISION, null, UUID.randomUUID().toString(), null);
            msg.setPayload(GSON.fromJson(GSON.toJson(reqBody), Map.class));

            out.println(msg.toJson());

            String responseLine = in.readLine();
            if (responseLine != null) {
                Message resp = Message.fromJson(responseLine);
                if (resp.getMessageType() == MessageType.ADMIN_DECISION_RESULT) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> payload = resp.getPayload(Map.class);
                    if (payload != null) {
                        Object successObj = payload.get("success");
                        return Boolean.TRUE.equals(successObj);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to send admin decision over TCP 5001: " + e.getMessage());
        }
        return false;
    }
}

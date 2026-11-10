package lk.fraudshield.common.protocol;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.util.HashMap;
import java.util.Map;

/**
 * Wire DTO. One Message is serialised as ONE line of JSON (newline-delimited protocol).
 *
 * <pre>{"type":"TRANSFER","token":"abc","reqId":"42","data":{"to":"ACC2","amount":"1500"}}</pre>
 */
public class Message {
    private static final Gson GSON = new Gson();

    private MessageType type;
    private String token;               // session token (null before login)
    private String reqId;               // echoed back so the client can match replies
    private Map<String, String> data = new HashMap<>();

    public Message() { }

    public Message(MessageType type, String token, String reqId) {
        this.type = type;
        this.token = token;
        this.reqId = reqId;
    }

    // ---- factories for server replies ----
    public static Message ok(String reqId) {
        return new Message(MessageType.OK, null, reqId);
    }

    public static Message error(String reqId, String reason) {
        Message m = new Message(MessageType.ERROR, null, reqId);
        m.put("reason", reason);
        return m;
    }

    // ---- fluent helpers ----
    public Message put(String key, String value) {
        data.put(key, value);
        return this;
    }

    public String get(String key) {
        return data == null ? null : data.get(key);
    }

    // ---- JSON ----
    public String toJson() {
        return GSON.toJson(this);
    }

    /** @throws IllegalArgumentException if the line is not a valid message */
    public static Message fromJson(String json) {
        try {
            Message m = GSON.fromJson(json, Message.class);
            if (m == null || m.type == null) {
                throw new IllegalArgumentException("Missing message type");
            }
            if (m.data == null) {
                m.data = new HashMap<>();
            }
            return m;
        } catch (JsonSyntaxException e) {
            throw new IllegalArgumentException("Malformed JSON", e);
        }
    }

    // ---- getters / setters ----
    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getReqId() { return reqId; }
    public void setReqId(String reqId) { this.reqId = reqId; }
    public Map<String, String> getData() { return data; }
    public void setData(Map<String, String> data) { this.data = data; }

    @Override
    public String toString() {
        return "Message{type=" + type + ", reqId=" + reqId + "}"; // never log token/data
    }
}

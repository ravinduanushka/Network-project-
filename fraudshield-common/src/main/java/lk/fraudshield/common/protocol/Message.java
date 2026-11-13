package lk.fraudshield.common.protocol;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

public class Message {

        private static final Gson GSON = new Gson();

        private String type;
        private String token;
        private String reqId;
        private String data;

        public Message() {
    }

    public Message(String type, String token, String reqId, String data) {
        this.type = type;
        this.token = token;
        this.reqId = reqId;
        this.data = data;
    }

        public Message(MessageType type, String token, String reqId, String data) {
        this(type == null ? null : type.name(), token, reqId, data);
    }

    // ------------------------------------------------------------------ factories

        public static Message error(String reqId, String reason) {
        JsonObject body = new JsonObject();
        body.addProperty("reason", reason);
        return new Message(MessageType.ERROR, null, reqId, body.toString());
    }

    // ------------------------------------------------------------------ JSON helpers

        public String toJson() {
        return GSON.toJson(this);
    }

        public static Message fromJson(String json) {
        try {
            Message m = GSON.fromJson(json, Message.class);
            if (m == null) {
                throw new IllegalArgumentException("Empty message");
            }
            return m;
        } catch (JsonSyntaxException e) {
            throw new IllegalArgumentException("Malformed JSON", e);
        }
    }

        public Message setPayload(Object payload) {
        this.data = payload == null ? null : GSON.toJson(payload);
        return this;
    }

        public <T> T getPayload(Class<T> clazz) {
        if (data == null || data.isBlank()) {
            return null;
        }
        try {
            return GSON.fromJson(data, clazz);
        } catch (JsonSyntaxException e) {
            throw new IllegalArgumentException("Malformed payload", e);
        }
    }

        public MessageType getMessageType() {
        return MessageType.fromString(type);
    }

    // ------------------------------------------------------------------ getters / setters

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getReqId() {
        return reqId;
    }

    public void setReqId(String reqId) {
        this.reqId = reqId;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

        @Override
    public String toString() {
        return "Message{type=" + type + ", reqId=" + reqId + "}";
    }
}

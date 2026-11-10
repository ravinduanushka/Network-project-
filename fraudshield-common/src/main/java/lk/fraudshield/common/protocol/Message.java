package lk.fraudshield.common.protocol;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

/**
 * Standard envelope for every FraudShield network message.
 *
 * <p>Sent as ONE line of JSON terminated by a newline:
 * <pre>{"type":"TRANSFER","token":"abc123","reqId":"42","data":"{\"to\":\"ACC2\",\"amount\":1500}"}</pre>
 *
 * <p>{@code data} is itself a JSON document stored as a String, so each message type can define its
 * own payload without changing this envelope. Use {@link #setPayload(Object)} /
 * {@link #getPayload(Class)} to convert to and from typed objects.
 */
public class Message {

    /** Gson is thread-safe; share one instance. */
    private static final Gson GSON = new Gson();

    /** Name of a {@link MessageType} constant. */
    private String type;
    /** Session token for authenticated requests; null before login. */
    private String token;
    /** Client-generated id, echoed in the reply so responses can be matched to requests. */
    private String reqId;
    /** Payload as a JSON string; may be null. */
    private String data;

    /** Required by Gson. */
    public Message() {
    }

    public Message(String type, String token, String reqId, String data) {
        this.type = type;
        this.token = token;
        this.reqId = reqId;
        this.data = data;
    }

    /** Convenience overload taking the enum. */
    public Message(MessageType type, String token, String reqId, String data) {
        this(type == null ? null : type.name(), token, reqId, data);
    }

    // ------------------------------------------------------------------ factories

    /** Builds an {@link MessageType#ERROR} reply with payload {@code {"reason":"..."}}. */
    public static Message error(String reqId, String reason) {
        JsonObject body = new JsonObject();
        body.addProperty("reason", reason);
        return new Message(MessageType.ERROR, null, reqId, body.toString());
    }

    // ------------------------------------------------------------------ JSON helpers

    /** Serialises this message to a single-line JSON string. */
    public String toJson() {
        return GSON.toJson(this);
    }

    /**
     * Parses one JSON line into a Message.
     *
     * @throws IllegalArgumentException if the text is not valid JSON or is empty
     */
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

    /** Serialises {@code payload} to JSON and stores it in {@link #data}. */
    public Message setPayload(Object payload) {
        this.data = payload == null ? null : GSON.toJson(payload);
        return this;
    }

    /**
     * Deserialises {@link #data} into {@code clazz}.
     *
     * @return the object, or null if there is no payload
     * @throws IllegalArgumentException if the payload does not match the class
     */
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

    /** @return the parsed type, or null if {@link #type} is missing or unknown. */
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

    /** Deliberately omits token and data so secrets never reach logs. */
    @Override
    public String toString() {
        return "Message{type=" + type + ", reqId=" + reqId + "}";
    }
}

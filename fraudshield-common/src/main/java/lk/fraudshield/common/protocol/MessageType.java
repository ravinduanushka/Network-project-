package lk.fraudshield.common.protocol;

/** All message kinds exchanged between client and server. */
public enum MessageType {
    // client -> server
    REGISTER,
    LOGIN,
    LOGOUT,
    BALANCE,
    TRANSFER,
    HISTORY,
    PING,

    // server -> client
    OK,
    ERROR,
    FRAUD_ALERT
}

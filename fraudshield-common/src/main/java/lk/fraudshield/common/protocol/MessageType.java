package lk.fraudshield.common.protocol;

/**
 * Every message kind exchanged between FraudShield clients and the server.
 *
 * <p>Convention: a client request has a plain name (e.g. {@link #TRANSFER}); the server's
 * answer is the matching {@code *_RESULT} (or a dedicated reply type such as {@link #BALANCE}).
 * {@link #ERROR} may be returned in reply to any request.
 */
public enum MessageType {

    // ---- authentication ----
    /** Client -> server: create a new customer account. */
    REGISTER,
    /** Server -> client: outcome of {@link #REGISTER}. */
    REGISTER_RESULT,
    /** Client -> server: customer login with credentials. */
    LOGIN,
    /** Server -> client: outcome of {@link #LOGIN}; carries the session token on success. */
    LOGIN_RESULT,
    /** Client -> server: end the current session. */
    LOGOUT,

    // ---- account queries ----
    /** Client -> server: request the current balance. */
    GET_BALANCE,
    /** Server -> client: balance reply to {@link #GET_BALANCE}. */
    BALANCE,
    /** Client -> server: request transaction history. */
    GET_HISTORY,
    /** Server -> client: history reply to {@link #GET_HISTORY}. */
    HISTORY,

    // ---- transactions ----
    /** Client -> server: transfer money to another account. */
    TRANSFER,
    /** Server -> client: outcome of {@link #TRANSFER} (completed, rejected, or OTP required). */
    TRANSFER_RESULT,
    /** Client -> server: submit a one-time password to confirm a flagged transfer. */
    VERIFY_OTP,
    /** Server -> client: outcome of {@link #VERIFY_OTP}. */
    OTP_RESULT,

    // ---- administration ----
    /** Client -> server: administrator login. */
    ADMIN_LOGIN,
    /** Server -> client: outcome of {@link #ADMIN_LOGIN}. */
    ADMIN_LOGIN_RESULT,

    // ---- generic ----
    /** Server -> client: request failed; payload holds the reason. */
    ERROR;

    /**
     * Safe lookup from the wire string.
     *
     * @param name value of {@link Message#getType()}; may be null
     * @return the matching constant, or {@code null} if the name is null or unknown
     */
    public static MessageType fromString(String name) {
        if (name == null) {
            return null;
        }
        try {
            return valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

package lk.fraudshield.common.protocol;

public enum MessageType {

    // ---- authentication ----
        REGISTER,
        REGISTER_RESULT,
        LOGIN,
        LOGIN_RESULT,
        LOGOUT,

    // ---- account queries ----
        GET_BALANCE,
        BALANCE,
        GET_HISTORY,
        HISTORY,

    // ---- transactions ----
        TRANSFER,
        TRANSFER_RESULT,
        VERIFY_OTP,
        OTP_RESULT,

    // ---- administration ----
        ADMIN_LOGIN,
        ADMIN_LOGIN_RESULT,

    // ---- generic ----
        ERROR;

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

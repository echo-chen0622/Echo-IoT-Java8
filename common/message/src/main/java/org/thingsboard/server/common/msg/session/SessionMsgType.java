package org.thingsboard.server.common.msg.session;

public enum SessionMsgType {
    GET_ATTRIBUTES_REQUEST(true), POST_ATTRIBUTES_REQUEST(true), GET_ATTRIBUTES_RESPONSE,
    SUBSCRIBE_ATTRIBUTES_REQUEST, UNSUBSCRIBE_ATTRIBUTES_REQUEST, ATTRIBUTES_UPDATE_NOTIFICATION,

    POST_TELEMETRY_REQUEST(true), STATUS_CODE_RESPONSE,

    SUBSCRIBE_RPC_COMMANDS_REQUEST, UNSUBSCRIBE_RPC_COMMANDS_REQUEST,
    TO_DEVICE_RPC_REQUEST, TO_DEVICE_RPC_RESPONSE, TO_DEVICE_RPC_RESPONSE_ACK,

    TO_SERVER_RPC_REQUEST(true), TO_SERVER_RPC_RESPONSE,

    RULE_ENGINE_ERROR,

    SESSION_OPEN, SESSION_CLOSE,

    CLAIM_REQUEST();

    private final boolean requiresRulesProcessing;

    SessionMsgType() {
        this(false);
    }

    SessionMsgType(boolean requiresRulesProcessing) {
        this.requiresRulesProcessing = requiresRulesProcessing;
    }

    public boolean requiresRulesProcessing() {
        return requiresRulesProcessing;
    }
}

package org.thingsboard.server.common.data.transport.snmp;

public enum SnmpMethod {
    GET(-96),
    SET(-93);

    // codes taken from org.snmp4j.PDU class
    private final int code;

    SnmpMethod(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}

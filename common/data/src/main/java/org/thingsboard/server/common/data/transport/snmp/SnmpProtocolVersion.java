package org.thingsboard.server.common.data.transport.snmp;

public enum SnmpProtocolVersion {
    V1(0),
    V2C(1),
    V3(3);

    private final int code;

    SnmpProtocolVersion(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}

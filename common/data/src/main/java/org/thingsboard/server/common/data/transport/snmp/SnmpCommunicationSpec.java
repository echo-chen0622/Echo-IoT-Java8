package org.thingsboard.server.common.data.transport.snmp;

public enum SnmpCommunicationSpec {
    TELEMETRY_QUERYING,

    CLIENT_ATTRIBUTES_QUERYING,
    SHARED_ATTRIBUTES_SETTING,

    TO_DEVICE_RPC_REQUEST,
}

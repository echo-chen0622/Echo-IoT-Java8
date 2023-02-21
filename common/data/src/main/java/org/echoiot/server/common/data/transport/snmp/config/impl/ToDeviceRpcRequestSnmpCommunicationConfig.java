package org.echoiot.server.common.data.transport.snmp.config.impl;

import org.echoiot.server.common.data.transport.snmp.SnmpCommunicationSpec;
import org.echoiot.server.common.data.transport.snmp.config.MultipleMappingsSnmpCommunicationConfig;
import org.jetbrains.annotations.NotNull;

public class ToDeviceRpcRequestSnmpCommunicationConfig extends MultipleMappingsSnmpCommunicationConfig {

    private static final long serialVersionUID = -8607312598073845460L;

    @NotNull
    @Override
    public SnmpCommunicationSpec getSpec() {
        return SnmpCommunicationSpec.TO_DEVICE_RPC_REQUEST;
    }
}

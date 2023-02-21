package org.echoiot.server.common.data.transport.snmp.config.impl;

import org.echoiot.server.common.data.transport.snmp.SnmpCommunicationSpec;
import org.echoiot.server.common.data.transport.snmp.config.RepeatingQueryingSnmpCommunicationConfig;
import org.jetbrains.annotations.NotNull;

public class ClientAttributesQueryingSnmpCommunicationConfig extends RepeatingQueryingSnmpCommunicationConfig {

    private static final long serialVersionUID = 536740834893462914L;

    @NotNull
    @Override
    public SnmpCommunicationSpec getSpec() {
        return SnmpCommunicationSpec.CLIENT_ATTRIBUTES_QUERYING;
    }

}

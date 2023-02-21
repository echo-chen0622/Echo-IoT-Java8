package org.echoiot.server.common.data.transport.snmp.config.impl;

import org.echoiot.server.common.data.transport.snmp.SnmpCommunicationSpec;
import org.echoiot.server.common.data.transport.snmp.SnmpMethod;
import org.echoiot.server.common.data.transport.snmp.config.MultipleMappingsSnmpCommunicationConfig;
import org.jetbrains.annotations.NotNull;

public class SharedAttributesSettingSnmpCommunicationConfig extends MultipleMappingsSnmpCommunicationConfig {

    private static final long serialVersionUID = 8981224974190924703L;

    @NotNull
    @Override
    public SnmpCommunicationSpec getSpec() {
        return SnmpCommunicationSpec.SHARED_ATTRIBUTES_SETTING;
    }

    @NotNull
    @Override
    public SnmpMethod getMethod() {
        return SnmpMethod.SET;
    }

}

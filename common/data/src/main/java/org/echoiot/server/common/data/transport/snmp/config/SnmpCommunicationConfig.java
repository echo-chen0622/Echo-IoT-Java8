package org.echoiot.server.common.data.transport.snmp.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.echoiot.server.common.data.transport.snmp.SnmpCommunicationSpec;
import org.echoiot.server.common.data.transport.snmp.SnmpMapping;
import org.echoiot.server.common.data.transport.snmp.SnmpMethod;
import org.echoiot.server.common.data.transport.snmp.config.impl.ClientAttributesQueryingSnmpCommunicationConfig;
import org.echoiot.server.common.data.transport.snmp.config.impl.SharedAttributesSettingSnmpCommunicationConfig;
import org.echoiot.server.common.data.transport.snmp.config.impl.TelemetryQueryingSnmpCommunicationConfig;
import org.echoiot.server.common.data.transport.snmp.config.impl.ToDeviceRpcRequestSnmpCommunicationConfig;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "spec")
@JsonSubTypes({
        @Type(value = TelemetryQueryingSnmpCommunicationConfig.class, name = "TELEMETRY_QUERYING"),
        @Type(value = ClientAttributesQueryingSnmpCommunicationConfig.class, name = "CLIENT_ATTRIBUTES_QUERYING"),
        @Type(value = SharedAttributesSettingSnmpCommunicationConfig.class, name = "SHARED_ATTRIBUTES_SETTING"),
        @Type(value = ToDeviceRpcRequestSnmpCommunicationConfig.class, name = "TO_DEVICE_RPC_REQUEST")
})
public interface SnmpCommunicationConfig extends Serializable {

    SnmpCommunicationSpec getSpec();

    @Nullable
    @JsonIgnore
    default SnmpMethod getMethod() {
        return null;
    }

    @JsonIgnore
    List<SnmpMapping> getAllMappings();

    @JsonIgnore
    boolean isValid();

}

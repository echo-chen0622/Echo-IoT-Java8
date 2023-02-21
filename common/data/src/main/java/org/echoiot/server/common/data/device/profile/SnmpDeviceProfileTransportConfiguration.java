package org.echoiot.server.common.data.device.profile;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.echoiot.server.common.data.DeviceTransportType;
import org.echoiot.server.common.data.transport.snmp.SnmpMapping;
import org.echoiot.server.common.data.transport.snmp.config.SnmpCommunicationConfig;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Data
public class SnmpDeviceProfileTransportConfiguration implements DeviceProfileTransportConfiguration {
    private Integer timeoutMs;
    private Integer retries;
    private List<SnmpCommunicationConfig> communicationConfigs;

    @NotNull
    @Override
    public DeviceTransportType getType() {
        return DeviceTransportType.SNMP;
    }

    @Override
    public void validate() {
        if (!isValid()) {
            throw new IllegalArgumentException("SNMP transport configuration is not valid");
        }
    }

    @JsonIgnore
    private boolean isValid() {
        return timeoutMs != null && timeoutMs >= 0 && retries != null && retries >= 0
                && communicationConfigs != null
                && communicationConfigs.stream().allMatch(config -> config != null && config.isValid())
                && communicationConfigs.stream().flatMap(config -> config.getAllMappings().stream()).map(SnmpMapping::getOid)
                .distinct().count() == communicationConfigs.stream().mapToInt(config -> config.getAllMappings().size()).sum();
    }
}

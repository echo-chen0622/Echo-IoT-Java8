package org.echoiot.server.common.data.transport.snmp.config;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.echoiot.server.common.data.transport.snmp.SnmpMethod;
import org.jetbrains.annotations.NotNull;

@EqualsAndHashCode(callSuper = true)
@Data
public abstract class RepeatingQueryingSnmpCommunicationConfig extends MultipleMappingsSnmpCommunicationConfig {
    private Long queryingFrequencyMs;

    @NotNull
    @Override
    public SnmpMethod getMethod() {
        return SnmpMethod.GET;
    }

    @Override
    public boolean isValid() {
        return queryingFrequencyMs != null && queryingFrequencyMs > 0 && super.isValid();
    }
}

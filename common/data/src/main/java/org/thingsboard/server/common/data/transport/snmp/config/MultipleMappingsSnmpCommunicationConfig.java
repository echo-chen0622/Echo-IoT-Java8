package org.thingsboard.server.common.data.transport.snmp.config;

import lombok.Data;
import org.thingsboard.server.common.data.transport.snmp.SnmpMapping;

import java.util.List;

@Data
public abstract class MultipleMappingsSnmpCommunicationConfig implements SnmpCommunicationConfig {
    protected List<SnmpMapping> mappings;

    @Override
    public boolean isValid() {
        return mappings != null && !mappings.isEmpty() && mappings.stream().allMatch(mapping -> mapping != null && mapping.isValid());
    }

    @Override
    public List<SnmpMapping> getAllMappings() {
        return mappings;
    }
}

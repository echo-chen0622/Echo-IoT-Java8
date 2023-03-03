package org.echoiot.rule.engine.transform;

import lombok.Data;
import org.echoiot.rule.engine.api.NodeConfiguration;

import java.util.Collections;
import java.util.Set;

@Data
public class TbCopyKeysNodeConfiguration implements NodeConfiguration {

    private boolean fromMetadata;
    private Set<String> keys;

    @Override
    public TbCopyKeysNodeConfiguration defaultConfiguration() {
        TbCopyKeysNodeConfiguration configuration = new TbCopyKeysNodeConfiguration();
        configuration.setKeys(Collections.emptySet());
        configuration.setFromMetadata(false);
        return configuration;
    }

}

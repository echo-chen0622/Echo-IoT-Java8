package org.echoiot.rule.engine.transform;

import lombok.Data;
import org.echoiot.rule.engine.api.NodeConfiguration;

import java.util.Collections;
import java.util.Set;

@Data
public class TbDeleteKeysNodeConfiguration implements NodeConfiguration<TbDeleteKeysNodeConfiguration> {

    private boolean fromMetadata;
    private Set<String> keys;

    @Override
    public TbDeleteKeysNodeConfiguration defaultConfiguration() {
        TbDeleteKeysNodeConfiguration configuration = new TbDeleteKeysNodeConfiguration();
        configuration.setKeys(Collections.emptySet());
        configuration.setFromMetadata(false);
        return configuration;
    }

}

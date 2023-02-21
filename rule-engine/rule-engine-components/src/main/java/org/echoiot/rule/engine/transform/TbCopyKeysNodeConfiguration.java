package org.echoiot.rule.engine.transform;

import lombok.Data;
import org.echoiot.rule.engine.api.NodeConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;

@Data
public class TbCopyKeysNodeConfiguration implements NodeConfiguration<TbCopyKeysNodeConfiguration> {

    private boolean fromMetadata;
    private Set<String> keys;

    @NotNull
    @Override
    public TbCopyKeysNodeConfiguration defaultConfiguration() {
        @NotNull TbCopyKeysNodeConfiguration configuration = new TbCopyKeysNodeConfiguration();
        configuration.setKeys(Collections.emptySet());
        configuration.setFromMetadata(false);
        return configuration;
    }

}

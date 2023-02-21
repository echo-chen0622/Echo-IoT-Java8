package org.echoiot.rule.engine.transform;

import lombok.Data;
import org.echoiot.rule.engine.api.NodeConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@Data
public class TbRenameKeysNodeConfiguration implements NodeConfiguration<TbRenameKeysNodeConfiguration> {

    private boolean fromMetadata;
    private Map<String, String> renameKeysMapping;

    @NotNull
    @Override
    public TbRenameKeysNodeConfiguration defaultConfiguration() {
        @NotNull TbRenameKeysNodeConfiguration configuration = new TbRenameKeysNodeConfiguration();
        configuration.setRenameKeysMapping(Map.of("temp", "temperature"));
        configuration.setFromMetadata(false);
        return configuration;
    }

}

package org.echoiot.rule.engine.transform;

import lombok.Data;
import org.echoiot.rule.engine.api.NodeConfiguration;

import java.util.Map;

@Data
public class TbRenameKeysNodeConfiguration implements NodeConfiguration {

    private boolean fromMetadata;
    private Map<String, String> renameKeysMapping;

    @Override
    public TbRenameKeysNodeConfiguration defaultConfiguration() {
        TbRenameKeysNodeConfiguration configuration = new TbRenameKeysNodeConfiguration();
        configuration.setRenameKeysMapping(Map.of("temp", "temperature"));
        configuration.setFromMetadata(false);
        return configuration;
    }

}

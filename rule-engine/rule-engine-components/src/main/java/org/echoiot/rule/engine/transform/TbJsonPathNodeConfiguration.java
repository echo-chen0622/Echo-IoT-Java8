package org.echoiot.rule.engine.transform;

import lombok.Data;
import org.echoiot.rule.engine.api.NodeConfiguration;

@Data
public class TbJsonPathNodeConfiguration implements NodeConfiguration {

    static final String DEFAULT_JSON_PATH = "$";
    private String jsonPath;

    @Override
    public TbJsonPathNodeConfiguration defaultConfiguration() {
        TbJsonPathNodeConfiguration configuration = new TbJsonPathNodeConfiguration();
        configuration.setJsonPath(DEFAULT_JSON_PATH);
        return configuration;
    }

}

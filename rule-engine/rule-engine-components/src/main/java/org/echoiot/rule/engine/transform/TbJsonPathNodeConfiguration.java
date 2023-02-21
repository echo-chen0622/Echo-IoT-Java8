package org.echoiot.rule.engine.transform;

import lombok.Data;
import org.echoiot.rule.engine.api.NodeConfiguration;
import org.jetbrains.annotations.NotNull;

@Data
public class TbJsonPathNodeConfiguration implements NodeConfiguration<TbJsonPathNodeConfiguration> {

    static final String DEFAULT_JSON_PATH = "$";
    private String jsonPath;

    @NotNull
    @Override
    public TbJsonPathNodeConfiguration defaultConfiguration() {
        @NotNull TbJsonPathNodeConfiguration configuration = new TbJsonPathNodeConfiguration();
        configuration.setJsonPath(DEFAULT_JSON_PATH);
        return configuration;
    }

}

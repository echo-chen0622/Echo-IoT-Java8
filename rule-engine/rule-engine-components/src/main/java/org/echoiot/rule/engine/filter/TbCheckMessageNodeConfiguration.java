package org.echoiot.rule.engine.filter;

import lombok.Data;
import org.echoiot.rule.engine.api.NodeConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

@Data
public class TbCheckMessageNodeConfiguration implements NodeConfiguration {

    private List<String> messageNames;
    private List<String> metadataNames;

    private boolean checkAllKeys;


    @NotNull
    @Override
    public TbCheckMessageNodeConfiguration defaultConfiguration() {
        @NotNull TbCheckMessageNodeConfiguration configuration = new TbCheckMessageNodeConfiguration();
        configuration.setMessageNames(Collections.emptyList());
        configuration.setMetadataNames(Collections.emptyList());
        configuration.setCheckAllKeys(true);
        return configuration;
    }
}

package org.thingsboard.rule.engine.filter;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;

import java.util.Collections;
import java.util.List;

@Data
public class TbCheckMessageNodeConfiguration implements NodeConfiguration {

    private List<String> messageNames;
    private List<String> metadataNames;

    private boolean checkAllKeys;


    @Override
    public TbCheckMessageNodeConfiguration defaultConfiguration() {
        TbCheckMessageNodeConfiguration configuration = new TbCheckMessageNodeConfiguration();
        configuration.setMessageNames(Collections.emptyList());
        configuration.setMetadataNames(Collections.emptyList());
        configuration.setCheckAllKeys(true);
        return configuration;
    }
}

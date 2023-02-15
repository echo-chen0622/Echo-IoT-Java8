package org.thingsboard.rule.engine.metadata;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;

import java.util.Collections;
import java.util.List;

/**
 * Created by Echo on 19.01.18.
 */
@Data
public class TbGetAttributesNodeConfiguration implements NodeConfiguration<TbGetAttributesNodeConfiguration> {

    private List<String> clientAttributeNames;
    private List<String> sharedAttributeNames;
    private List<String> serverAttributeNames;

    private List<String> latestTsKeyNames;

    private boolean tellFailureIfAbsent;
    private boolean getLatestValueWithTs;
    private boolean fetchToData;

    @Override
    public TbGetAttributesNodeConfiguration defaultConfiguration() {
        TbGetAttributesNodeConfiguration configuration = new TbGetAttributesNodeConfiguration();
        configuration.setClientAttributeNames(Collections.emptyList());
        configuration.setSharedAttributeNames(Collections.emptyList());
        configuration.setServerAttributeNames(Collections.emptyList());
        configuration.setLatestTsKeyNames(Collections.emptyList());
        configuration.setTellFailureIfAbsent(true);
        configuration.setGetLatestValueWithTs(false);
        configuration.setFetchToData(false);
        return configuration;
    }
}

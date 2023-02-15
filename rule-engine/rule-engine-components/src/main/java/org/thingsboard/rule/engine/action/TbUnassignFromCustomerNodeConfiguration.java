package org.thingsboard.rule.engine.action;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;

@Data
public class TbUnassignFromCustomerNodeConfiguration extends TbAbstractCustomerActionNodeConfiguration implements NodeConfiguration<TbUnassignFromCustomerNodeConfiguration> {

    @Override
    public TbUnassignFromCustomerNodeConfiguration defaultConfiguration() {
        TbUnassignFromCustomerNodeConfiguration configuration = new TbUnassignFromCustomerNodeConfiguration();
        configuration.setCustomerNamePattern("");
        configuration.setCustomerCacheExpiration(300);
        return configuration;
    }
}

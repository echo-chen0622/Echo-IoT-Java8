package org.thingsboard.rule.engine.action;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;

@Data
public class TbAssignToCustomerNodeConfiguration extends TbAbstractCustomerActionNodeConfiguration implements NodeConfiguration<TbAssignToCustomerNodeConfiguration> {

    private boolean createCustomerIfNotExists;

    @Override
    public TbAssignToCustomerNodeConfiguration defaultConfiguration() {
        TbAssignToCustomerNodeConfiguration configuration = new TbAssignToCustomerNodeConfiguration();
        configuration.setCustomerNamePattern("");
        configuration.setCreateCustomerIfNotExists(false);
        configuration.setCustomerCacheExpiration(300);
        return configuration;
    }
}

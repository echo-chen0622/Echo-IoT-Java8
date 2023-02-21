package org.echoiot.rule.engine.action;

import lombok.Data;
import org.echoiot.rule.engine.api.NodeConfiguration;
import org.jetbrains.annotations.NotNull;

@Data
public class TbAssignToCustomerNodeConfiguration extends TbAbstractCustomerActionNodeConfiguration implements NodeConfiguration<TbAssignToCustomerNodeConfiguration> {

    private boolean createCustomerIfNotExists;

    @NotNull
    @Override
    public TbAssignToCustomerNodeConfiguration defaultConfiguration() {
        @NotNull TbAssignToCustomerNodeConfiguration configuration = new TbAssignToCustomerNodeConfiguration();
        configuration.setCustomerNamePattern("");
        configuration.setCreateCustomerIfNotExists(false);
        configuration.setCustomerCacheExpiration(300);
        return configuration;
    }
}

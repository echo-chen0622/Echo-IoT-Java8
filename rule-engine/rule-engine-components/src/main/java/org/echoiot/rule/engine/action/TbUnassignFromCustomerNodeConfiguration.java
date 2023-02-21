package org.echoiot.rule.engine.action;

import lombok.Data;
import org.echoiot.rule.engine.api.NodeConfiguration;
import org.jetbrains.annotations.NotNull;

@Data
public class TbUnassignFromCustomerNodeConfiguration extends TbAbstractCustomerActionNodeConfiguration implements NodeConfiguration<TbUnassignFromCustomerNodeConfiguration> {

    @NotNull
    @Override
    public TbUnassignFromCustomerNodeConfiguration defaultConfiguration() {
        @NotNull TbUnassignFromCustomerNodeConfiguration configuration = new TbUnassignFromCustomerNodeConfiguration();
        configuration.setCustomerNamePattern("");
        configuration.setCustomerCacheExpiration(300);
        return configuration;
    }
}

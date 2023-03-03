package org.echoiot.rule.engine.action;

import lombok.Data;
import org.echoiot.rule.engine.api.NodeConfiguration;

@Data
public class TbUnassignFromCustomerNodeConfiguration extends TbAbstractCustomerActionNodeConfiguration implements NodeConfiguration {

    @Override
    public TbUnassignFromCustomerNodeConfiguration defaultConfiguration() {
        TbUnassignFromCustomerNodeConfiguration configuration = new TbUnassignFromCustomerNodeConfiguration();
        configuration.setCustomerNamePattern("");
        configuration.setCustomerCacheExpiration(300);
        return configuration;
    }
}

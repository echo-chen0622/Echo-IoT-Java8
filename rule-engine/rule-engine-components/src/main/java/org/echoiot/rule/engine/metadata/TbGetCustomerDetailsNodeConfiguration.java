package org.echoiot.rule.engine.metadata;

import lombok.Data;
import org.echoiot.rule.engine.api.NodeConfiguration;

import java.util.Collections;

@Data
public class TbGetCustomerDetailsNodeConfiguration extends TbAbstractGetEntityDetailsNodeConfiguration implements NodeConfiguration<TbGetCustomerDetailsNodeConfiguration> {


    @Override
    public TbGetCustomerDetailsNodeConfiguration defaultConfiguration() {
        TbGetCustomerDetailsNodeConfiguration configuration = new TbGetCustomerDetailsNodeConfiguration();
        configuration.setDetailsList(Collections.emptyList());
        return configuration;
    }
}

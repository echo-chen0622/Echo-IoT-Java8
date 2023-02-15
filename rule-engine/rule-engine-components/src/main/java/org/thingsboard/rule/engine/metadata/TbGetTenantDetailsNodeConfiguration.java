package org.thingsboard.rule.engine.metadata;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;

import java.util.Collections;

@Data
public class TbGetTenantDetailsNodeConfiguration extends TbAbstractGetEntityDetailsNodeConfiguration implements NodeConfiguration<TbGetTenantDetailsNodeConfiguration> {


    @Override
    public TbGetTenantDetailsNodeConfiguration defaultConfiguration() {
        TbGetTenantDetailsNodeConfiguration configuration = new TbGetTenantDetailsNodeConfiguration();
        configuration.setDetailsList(Collections.emptyList());
        return configuration;
    }
}

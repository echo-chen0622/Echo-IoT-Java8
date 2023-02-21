package org.echoiot.rule.engine.metadata;

import lombok.Data;
import org.echoiot.rule.engine.api.NodeConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

@Data
public class TbGetTenantDetailsNodeConfiguration extends TbAbstractGetEntityDetailsNodeConfiguration implements NodeConfiguration<TbGetTenantDetailsNodeConfiguration> {


    @NotNull
    @Override
    public TbGetTenantDetailsNodeConfiguration defaultConfiguration() {
        @NotNull TbGetTenantDetailsNodeConfiguration configuration = new TbGetTenantDetailsNodeConfiguration();
        configuration.setDetailsList(Collections.emptyList());
        return configuration;
    }
}

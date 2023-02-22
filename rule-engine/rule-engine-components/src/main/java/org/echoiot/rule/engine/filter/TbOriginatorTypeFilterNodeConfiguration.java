package org.echoiot.rule.engine.filter;

import lombok.Data;
import org.echoiot.rule.engine.api.NodeConfiguration;
import org.echoiot.server.common.data.EntityType;

import java.util.List;

@Data
public class TbOriginatorTypeFilterNodeConfiguration implements NodeConfiguration<TbOriginatorTypeFilterNodeConfiguration> {

    private List<EntityType> originatorTypes;

    @Override
    public TbOriginatorTypeFilterNodeConfiguration defaultConfiguration() {
        TbOriginatorTypeFilterNodeConfiguration configuration = new TbOriginatorTypeFilterNodeConfiguration();
        configuration.setOriginatorTypes(List.of(EntityType.DEVICE));
        return configuration;
    }
}

package org.echoiot.rule.engine.filter;

import lombok.Data;
import org.echoiot.rule.engine.api.NodeConfiguration;
import org.echoiot.server.common.data.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

@Data
public class TbOriginatorTypeFilterNodeConfiguration implements NodeConfiguration<TbOriginatorTypeFilterNodeConfiguration> {

    private List<EntityType> originatorTypes;

    @NotNull
    @Override
    public TbOriginatorTypeFilterNodeConfiguration defaultConfiguration() {
        @NotNull TbOriginatorTypeFilterNodeConfiguration configuration = new TbOriginatorTypeFilterNodeConfiguration();
        configuration.setOriginatorTypes(List.of(EntityType.DEVICE));
        return configuration;
    }
}

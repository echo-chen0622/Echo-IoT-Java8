package org.echoiot.rule.engine.action;

import lombok.Data;
import org.echoiot.rule.engine.api.NodeConfiguration;
import org.echoiot.server.common.data.relation.EntitySearchDirection;
import org.jetbrains.annotations.NotNull;

@Data
public class TbDeleteRelationNodeConfiguration extends TbAbstractRelationActionNodeConfiguration implements NodeConfiguration<TbDeleteRelationNodeConfiguration> {

    private boolean deleteForSingleEntity;

    @NotNull
    @Override
    public TbDeleteRelationNodeConfiguration defaultConfiguration() {
        @NotNull TbDeleteRelationNodeConfiguration configuration = new TbDeleteRelationNodeConfiguration();
        configuration.setDeleteForSingleEntity(true);
        configuration.setDirection(EntitySearchDirection.FROM.name());
        configuration.setRelationType("Contains");
        configuration.setEntityNamePattern("");
        configuration.setEntityCacheExpiration(300);
        return configuration;
    }
}

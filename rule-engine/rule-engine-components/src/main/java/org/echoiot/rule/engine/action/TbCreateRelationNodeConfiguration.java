package org.echoiot.rule.engine.action;

import lombok.Data;
import org.echoiot.rule.engine.api.NodeConfiguration;
import org.echoiot.server.common.data.relation.EntitySearchDirection;
import org.jetbrains.annotations.NotNull;

@Data
public class TbCreateRelationNodeConfiguration extends TbAbstractRelationActionNodeConfiguration implements NodeConfiguration<TbCreateRelationNodeConfiguration> {

    private boolean createEntityIfNotExists;
    private boolean changeOriginatorToRelatedEntity;
    private boolean removeCurrentRelations;

    @NotNull
    @Override
    public TbCreateRelationNodeConfiguration defaultConfiguration() {
        @NotNull TbCreateRelationNodeConfiguration configuration = new TbCreateRelationNodeConfiguration();
        configuration.setDirection(EntitySearchDirection.FROM.name());
        configuration.setRelationType("Contains");
        configuration.setEntityNamePattern("");
        configuration.setEntityCacheExpiration(300);
        configuration.setCreateEntityIfNotExists(false);
        configuration.setRemoveCurrentRelations(false);
        configuration.setChangeOriginatorToRelatedEntity(false);
        return configuration;
    }
}

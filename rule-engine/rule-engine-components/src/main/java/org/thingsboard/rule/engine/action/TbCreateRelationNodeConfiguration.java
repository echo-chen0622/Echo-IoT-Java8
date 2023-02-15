package org.thingsboard.rule.engine.action;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;

@Data
public class TbCreateRelationNodeConfiguration extends TbAbstractRelationActionNodeConfiguration implements NodeConfiguration<TbCreateRelationNodeConfiguration> {

    private boolean createEntityIfNotExists;
    private boolean changeOriginatorToRelatedEntity;
    private boolean removeCurrentRelations;

    @Override
    public TbCreateRelationNodeConfiguration defaultConfiguration() {
        TbCreateRelationNodeConfiguration configuration = new TbCreateRelationNodeConfiguration();
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

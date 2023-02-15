package org.thingsboard.rule.engine.action;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;

@Data
public class TbDeleteRelationNodeConfiguration extends TbAbstractRelationActionNodeConfiguration implements NodeConfiguration<TbDeleteRelationNodeConfiguration> {

    private boolean deleteForSingleEntity;

    @Override
    public TbDeleteRelationNodeConfiguration defaultConfiguration() {
        TbDeleteRelationNodeConfiguration configuration = new TbDeleteRelationNodeConfiguration();
        configuration.setDeleteForSingleEntity(true);
        configuration.setDirection(EntitySearchDirection.FROM.name());
        configuration.setRelationType("Contains");
        configuration.setEntityNamePattern("");
        configuration.setEntityCacheExpiration(300);
        return configuration;
    }
}

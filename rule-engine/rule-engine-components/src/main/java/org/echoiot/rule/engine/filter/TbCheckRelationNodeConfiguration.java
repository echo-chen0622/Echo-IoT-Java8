package org.echoiot.rule.engine.filter;

import lombok.Data;
import org.echoiot.rule.engine.api.NodeConfiguration;
import org.echoiot.server.common.data.relation.EntitySearchDirection;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Echo on 19.01.18.
 */
@Data
public class TbCheckRelationNodeConfiguration implements NodeConfiguration<TbCheckRelationNodeConfiguration> {

    private String direction;
    private String entityId;
    private String entityType;
    private String relationType;
    private boolean checkForSingleEntity;

    @NotNull
    @Override
    public TbCheckRelationNodeConfiguration defaultConfiguration() {
        @NotNull TbCheckRelationNodeConfiguration configuration = new TbCheckRelationNodeConfiguration();
        configuration.setDirection(EntitySearchDirection.FROM.name());
        configuration.setRelationType("Contains");
        configuration.setCheckForSingleEntity(true);
        return configuration;
    }
}

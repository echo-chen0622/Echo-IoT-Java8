package org.echoiot.rule.engine.transform;

import lombok.Data;
import org.echoiot.rule.engine.api.NodeConfiguration;
import org.echoiot.rule.engine.data.RelationsQuery;
import org.echoiot.server.common.data.relation.EntityRelation;
import org.echoiot.server.common.data.relation.EntitySearchDirection;
import org.echoiot.server.common.data.relation.RelationEntityTypeFilter;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

@Data
public class TbChangeOriginatorNodeConfiguration extends TbTransformNodeConfiguration implements NodeConfiguration {

    private String originatorSource;

    private RelationsQuery relationsQuery;
    private String entityType;
    private String entityNamePattern;

    @NotNull
    @Override
    public TbChangeOriginatorNodeConfiguration defaultConfiguration() {
        @NotNull TbChangeOriginatorNodeConfiguration configuration = new TbChangeOriginatorNodeConfiguration();
        configuration.setOriginatorSource(TbChangeOriginatorNode.CUSTOMER_SOURCE);

        @NotNull RelationsQuery relationsQuery = new RelationsQuery();
        relationsQuery.setDirection(EntitySearchDirection.FROM);
        relationsQuery.setMaxLevel(1);
        @NotNull RelationEntityTypeFilter relationEntityTypeFilter = new RelationEntityTypeFilter(EntityRelation.CONTAINS_TYPE, Collections.emptyList());
        relationsQuery.setFilters(Collections.singletonList(relationEntityTypeFilter));
        configuration.setRelationsQuery(relationsQuery);

        return configuration;
    }
}

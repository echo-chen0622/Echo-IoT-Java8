package org.echoiot.rule.engine.metadata;

import lombok.Data;
import org.echoiot.rule.engine.data.RelationsQuery;
import org.echoiot.server.common.data.relation.EntityRelation;
import org.echoiot.server.common.data.relation.EntitySearchDirection;
import org.echoiot.server.common.data.relation.RelationEntityTypeFilter;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Data
public class TbGetRelatedAttrNodeConfiguration extends TbGetEntityAttrNodeConfiguration {

    private RelationsQuery relationsQuery;

    @NotNull
    @Override
    public TbGetRelatedAttrNodeConfiguration defaultConfiguration() {
        @NotNull TbGetRelatedAttrNodeConfiguration configuration = new TbGetRelatedAttrNodeConfiguration();
        @NotNull Map<String, String> attrMapping = new HashMap<>();
        attrMapping.putIfAbsent("temperature", "tempo");
        configuration.setAttrMapping(attrMapping);
        configuration.setTelemetry(false);

        @NotNull RelationsQuery relationsQuery = new RelationsQuery();
        relationsQuery.setDirection(EntitySearchDirection.FROM);
        relationsQuery.setMaxLevel(1);
        @NotNull RelationEntityTypeFilter relationEntityTypeFilter = new RelationEntityTypeFilter(EntityRelation.CONTAINS_TYPE, Collections.emptyList());
        relationsQuery.setFilters(Collections.singletonList(relationEntityTypeFilter));
        configuration.setRelationsQuery(relationsQuery);

        return configuration;
    }
}

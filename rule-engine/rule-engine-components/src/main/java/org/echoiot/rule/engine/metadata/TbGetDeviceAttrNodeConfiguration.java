package org.echoiot.rule.engine.metadata;

import lombok.Data;
import org.echoiot.rule.engine.data.DeviceRelationsQuery;
import org.echoiot.server.common.data.relation.EntityRelation;
import org.echoiot.server.common.data.relation.EntitySearchDirection;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

@Data
public class TbGetDeviceAttrNodeConfiguration extends TbGetAttributesNodeConfiguration {

    private DeviceRelationsQuery deviceRelationsQuery;

    @NotNull
    @Override
    public TbGetDeviceAttrNodeConfiguration defaultConfiguration() {
        @NotNull TbGetDeviceAttrNodeConfiguration configuration = new TbGetDeviceAttrNodeConfiguration();
        configuration.setClientAttributeNames(Collections.emptyList());
        configuration.setSharedAttributeNames(Collections.emptyList());
        configuration.setServerAttributeNames(Collections.emptyList());
        configuration.setLatestTsKeyNames(Collections.emptyList());
        configuration.setTellFailureIfAbsent(true);
        configuration.setGetLatestValueWithTs(false);
        configuration.setFetchToData(false);

        @NotNull DeviceRelationsQuery deviceRelationsQuery = new DeviceRelationsQuery();
        deviceRelationsQuery.setDirection(EntitySearchDirection.FROM);
        deviceRelationsQuery.setMaxLevel(1);
        deviceRelationsQuery.setRelationType(EntityRelation.CONTAINS_TYPE);
        deviceRelationsQuery.setDeviceTypes(Collections.singletonList("default"));

        configuration.setDeviceRelationsQuery(deviceRelationsQuery);

        return configuration;
    }
}

package org.thingsboard.rule.engine.metadata;

import lombok.Data;
import org.thingsboard.rule.engine.data.DeviceRelationsQuery;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;

import java.util.Collections;

@Data
public class TbGetDeviceAttrNodeConfiguration extends TbGetAttributesNodeConfiguration {

    private DeviceRelationsQuery deviceRelationsQuery;

    @Override
    public TbGetDeviceAttrNodeConfiguration defaultConfiguration() {
        TbGetDeviceAttrNodeConfiguration configuration = new TbGetDeviceAttrNodeConfiguration();
        configuration.setClientAttributeNames(Collections.emptyList());
        configuration.setSharedAttributeNames(Collections.emptyList());
        configuration.setServerAttributeNames(Collections.emptyList());
        configuration.setLatestTsKeyNames(Collections.emptyList());
        configuration.setTellFailureIfAbsent(true);
        configuration.setGetLatestValueWithTs(false);
        configuration.setFetchToData(false);

        DeviceRelationsQuery deviceRelationsQuery = new DeviceRelationsQuery();
        deviceRelationsQuery.setDirection(EntitySearchDirection.FROM);
        deviceRelationsQuery.setMaxLevel(1);
        deviceRelationsQuery.setRelationType(EntityRelation.CONTAINS_TYPE);
        deviceRelationsQuery.setDeviceTypes(Collections.singletonList("default"));

        configuration.setDeviceRelationsQuery(deviceRelationsQuery);

        return configuration;
    }
}

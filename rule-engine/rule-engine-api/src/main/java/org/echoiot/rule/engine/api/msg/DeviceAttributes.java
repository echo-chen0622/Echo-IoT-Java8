package org.echoiot.rule.engine.api.msg;

import org.echoiot.server.common.data.DataConstants;
import org.echoiot.server.common.data.kv.AttributeKey;
import org.echoiot.server.common.data.kv.AttributeKvEntry;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author Echo
 */
public class DeviceAttributes {

    private final Map<String, AttributeKvEntry> clientSideAttributesMap;
    private final Map<String, AttributeKvEntry> serverPrivateAttributesMap;
    private final Map<String, AttributeKvEntry> serverPublicAttributesMap;

    public DeviceAttributes(List<AttributeKvEntry> clientSideAttributes, List<AttributeKvEntry> serverPrivateAttributes, List<AttributeKvEntry> serverPublicAttributes) {
        this.clientSideAttributesMap = mapAttributes(clientSideAttributes);
        this.serverPrivateAttributesMap = mapAttributes(serverPrivateAttributes);
        this.serverPublicAttributesMap = mapAttributes(serverPublicAttributes);
    }

    private static Map<String, AttributeKvEntry> mapAttributes(List<AttributeKvEntry> attributes) {
        Map<String, AttributeKvEntry> result = new HashMap<>();
        for (AttributeKvEntry attribute : attributes) {
            result.put(attribute.getKey(), attribute);
        }
        return result;
    }

    public Collection<AttributeKvEntry> getClientSideAttributes() {
        return clientSideAttributesMap.values();
    }

    public Collection<AttributeKvEntry> getServerSideAttributes() {
        return serverPrivateAttributesMap.values();
    }

    public Collection<AttributeKvEntry> getServerSidePublicAttributes() {
        return serverPublicAttributesMap.values();
    }

    public Optional<AttributeKvEntry> getClientSideAttribute(String attribute) {
        return Optional.ofNullable(clientSideAttributesMap.get(attribute));
    }

    public Optional<AttributeKvEntry> getServerPrivateAttribute(String attribute) {
        return Optional.ofNullable(serverPrivateAttributesMap.get(attribute));
    }

    public Optional<AttributeKvEntry> getServerPublicAttribute(String attribute) {
        return Optional.ofNullable(serverPublicAttributesMap.get(attribute));
    }

    public void remove(AttributeKey key) {
        @Nullable Map<String, AttributeKvEntry> map = getMapByScope(key.getScope());
        if (map != null) {
            map.remove(key.getAttributeKey());
        }
    }

    public void update(String scope, List<AttributeKvEntry> values) {
        @Nullable Map<String, AttributeKvEntry> map = getMapByScope(scope);
        values.forEach(v -> map.put(v.getKey(), v));
    }

    @Nullable
    private Map<String, AttributeKvEntry> getMapByScope(String scope) {
        @Nullable Map<String, AttributeKvEntry> map = null;
        if (scope.equalsIgnoreCase(DataConstants.CLIENT_SCOPE)) {
            map = clientSideAttributesMap;
        } else if (scope.equalsIgnoreCase(DataConstants.SHARED_SCOPE)) {
            map = serverPublicAttributesMap;
        } else if (scope.equalsIgnoreCase(DataConstants.SERVER_SCOPE)) {
            map = serverPrivateAttributesMap;
        }
        return map;
    }

    @Override
    public String toString() {
        return "DeviceAttributes{" +
                "clientSideAttributesMap=" + clientSideAttributesMap +
                ", serverPrivateAttributesMap=" + serverPrivateAttributesMap +
                ", serverPublicAttributesMap=" + serverPublicAttributesMap +
                '}';
    }
}

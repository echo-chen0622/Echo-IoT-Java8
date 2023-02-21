package org.echoiot.rule.engine.api.msg;

import org.echoiot.server.common.data.DataConstants;
import org.echoiot.server.common.data.kv.AttributeKey;
import org.echoiot.server.common.data.kv.AttributeKvEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author Andrew Shvayka
 */
public class DeviceAttributes {

    @NotNull
    private final Map<String, AttributeKvEntry> clientSideAttributesMap;
    @NotNull
    private final Map<String, AttributeKvEntry> serverPrivateAttributesMap;
    @NotNull
    private final Map<String, AttributeKvEntry> serverPublicAttributesMap;

    public DeviceAttributes(@NotNull List<AttributeKvEntry> clientSideAttributes, @NotNull List<AttributeKvEntry> serverPrivateAttributes, @NotNull List<AttributeKvEntry> serverPublicAttributes) {
        this.clientSideAttributesMap = mapAttributes(clientSideAttributes);
        this.serverPrivateAttributesMap = mapAttributes(serverPrivateAttributes);
        this.serverPublicAttributesMap = mapAttributes(serverPublicAttributes);
    }

    @NotNull
    private static Map<String, AttributeKvEntry> mapAttributes(@NotNull List<AttributeKvEntry> attributes) {
        @NotNull Map<String, AttributeKvEntry> result = new HashMap<>();
        for (@NotNull AttributeKvEntry attribute : attributes) {
            result.put(attribute.getKey(), attribute);
        }
        return result;
    }

    @NotNull
    public Collection<AttributeKvEntry> getClientSideAttributes() {
        return clientSideAttributesMap.values();
    }

    @NotNull
    public Collection<AttributeKvEntry> getServerSideAttributes() {
        return serverPrivateAttributesMap.values();
    }

    @NotNull
    public Collection<AttributeKvEntry> getServerSidePublicAttributes() {
        return serverPublicAttributesMap.values();
    }

    @NotNull
    public Optional<AttributeKvEntry> getClientSideAttribute(String attribute) {
        return Optional.ofNullable(clientSideAttributesMap.get(attribute));
    }

    @NotNull
    public Optional<AttributeKvEntry> getServerPrivateAttribute(String attribute) {
        return Optional.ofNullable(serverPrivateAttributesMap.get(attribute));
    }

    @NotNull
    public Optional<AttributeKvEntry> getServerPublicAttribute(String attribute) {
        return Optional.ofNullable(serverPublicAttributesMap.get(attribute));
    }

    public void remove(@NotNull AttributeKey key) {
        @Nullable Map<String, AttributeKvEntry> map = getMapByScope(key.getScope());
        if (map != null) {
            map.remove(key.getAttributeKey());
        }
    }

    public void update(@NotNull String scope, @NotNull List<AttributeKvEntry> values) {
        @Nullable Map<String, AttributeKvEntry> map = getMapByScope(scope);
        values.forEach(v -> map.put(v.getKey(), v));
    }

    @Nullable
    private Map<String, AttributeKvEntry> getMapByScope(@NotNull String scope) {
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

    @NotNull
    @Override
    public String toString() {
        return "DeviceAttributes{" +
                "clientSideAttributesMap=" + clientSideAttributesMap +
                ", serverPrivateAttributesMap=" + serverPrivateAttributesMap +
                ", serverPublicAttributesMap=" + serverPublicAttributesMap +
                '}';
    }
}

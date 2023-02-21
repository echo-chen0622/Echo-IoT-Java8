package org.echoiot.server.dao.attributes;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.id.DeviceProfileId;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.kv.AttributeKvEntry;
import org.echoiot.server.dao.service.Validator;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.echoiot.server.dao.attributes.AttributeUtils.validate;

/**
 * @author Andrew Shvayka
 */
@Service
@ConditionalOnProperty(prefix = "cache.attributes", value = "enabled", havingValue = "false", matchIfMissing = true)
@Primary
@Slf4j
public class BaseAttributesService implements AttributesService {
    private final AttributesDao attributesDao;

    public BaseAttributesService(AttributesDao attributesDao) {
        this.attributesDao = attributesDao;
    }

    @NotNull
    @Override
    public ListenableFuture<Optional<AttributeKvEntry>> find(TenantId tenantId, @NotNull EntityId entityId, @NotNull String scope, @NotNull String attributeKey) {
        AttributeUtils.validate(entityId, scope);
        Validator.validateString(attributeKey, "Incorrect attribute key " + attributeKey);
        return Futures.immediateFuture(attributesDao.find(tenantId, entityId, scope, attributeKey));
    }

    @NotNull
    @Override
    public ListenableFuture<List<AttributeKvEntry>> find(TenantId tenantId, @NotNull EntityId entityId, @NotNull String scope, @NotNull Collection<String> attributeKeys) {
        AttributeUtils.validate(entityId, scope);
        attributeKeys.forEach(attributeKey -> Validator.validateString(attributeKey, "Incorrect attribute key " + attributeKey));
        return Futures.immediateFuture(attributesDao.find(tenantId, entityId, scope, attributeKeys));
    }

    @NotNull
    @Override
    public ListenableFuture<List<AttributeKvEntry>> findAll(TenantId tenantId, @NotNull EntityId entityId, @NotNull String scope) {
        AttributeUtils.validate(entityId, scope);
        return Futures.immediateFuture(attributesDao.findAll(tenantId, entityId, scope));
    }

    @Override
    public List<String> findAllKeysByDeviceProfileId(TenantId tenantId, DeviceProfileId deviceProfileId) {
        return attributesDao.findAllKeysByDeviceProfileId(tenantId, deviceProfileId);
    }

    @Override
    public List<String> findAllKeysByEntityIds(TenantId tenantId, EntityType entityType, List<EntityId> entityIds) {
        return attributesDao.findAllKeysByEntityIds(tenantId, entityType, entityIds);
    }

    @Override
    public ListenableFuture<String> save(TenantId tenantId, @NotNull EntityId entityId, @NotNull String scope, @NotNull AttributeKvEntry attribute) {
        AttributeUtils.validate(entityId, scope);
        AttributeUtils.validate(attribute);
        return attributesDao.save(tenantId, entityId, scope, attribute);
    }

    @NotNull
    @Override
    public ListenableFuture<List<String>> save(TenantId tenantId, @NotNull EntityId entityId, @NotNull String scope, @NotNull List<AttributeKvEntry> attributes) {
        AttributeUtils.validate(entityId, scope);
        attributes.forEach(AttributeUtils::validate);
        @NotNull List<ListenableFuture<String>> saveFutures = attributes.stream().map(attribute -> attributesDao.save(tenantId, entityId, scope, attribute)).collect(Collectors.toList());
        return Futures.allAsList(saveFutures);
    }

    @NotNull
    @Override
    public ListenableFuture<List<String>> removeAll(TenantId tenantId, @NotNull EntityId entityId, @NotNull String scope, List<String> attributeKeys) {
        AttributeUtils.validate(entityId, scope);
        return Futures.allAsList(attributesDao.removeAll(tenantId, entityId, scope, attributeKeys));
    }
}

package org.thingsboard.server.dao.attributes;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * @author Andrew Shvayka
 */
public interface AttributesDao {

    Optional<AttributeKvEntry> find(TenantId tenantId, EntityId entityId, String attributeType, String attributeKey);

    List<AttributeKvEntry> find(TenantId tenantId, EntityId entityId, String attributeType, Collection<String> attributeKey);

    List<AttributeKvEntry> findAll(TenantId tenantId, EntityId entityId, String attributeType);

    ListenableFuture<String> save(TenantId tenantId, EntityId entityId, String attributeType, AttributeKvEntry attribute);

    List<ListenableFuture<String>> removeAll(TenantId tenantId, EntityId entityId, String attributeType, List<String> keys);

    List<String> findAllKeysByDeviceProfileId(TenantId tenantId, DeviceProfileId deviceProfileId);

    List<String> findAllKeysByEntityIds(TenantId tenantId, EntityType entityType, List<EntityId> entityIds);
}

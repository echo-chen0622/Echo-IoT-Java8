package org.echoiot.server.dao.sql.attributes;

import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.dao.model.sql.AttributeKvCompositeKey;
import org.echoiot.server.dao.model.sql.AttributeKvEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface AttributeKvRepository extends JpaRepository<AttributeKvEntity, AttributeKvCompositeKey> {

    @Query("SELECT a FROM AttributeKvEntity a WHERE a.id.entityType = :entityType " +
            "AND a.id.entityId = :entityId " +
            "AND a.id.attributeType = :attributeType")
    List<AttributeKvEntity> findAllByEntityTypeAndEntityIdAndAttributeType(@Param("entityType") EntityType entityType,
                                                                           @Param("entityId") UUID entityId,
                                                                           @Param("attributeType") String attributeType);

    @Transactional
    @Modifying
    @Query("DELETE FROM AttributeKvEntity a WHERE a.id.entityType = :entityType " +
            "AND a.id.entityId = :entityId " +
            "AND a.id.attributeType = :attributeType " +
            "AND a.id.attributeKey = :attributeKey")
    void delete(@Param("entityType") EntityType entityType,
                @Param("entityId") UUID entityId,
                @Param("attributeType") String attributeType,
                @Param("attributeKey") String attributeKey);

    @Query(value = "SELECT DISTINCT attribute_key FROM attribute_kv WHERE entity_type = 'DEVICE' " +
            "AND entity_id in (SELECT id FROM device WHERE tenant_id = :tenantId and device_profile_id = :deviceProfileId limit 100) ORDER BY attribute_key", nativeQuery = true)
    List<String> findAllKeysByDeviceProfileId(@Param("tenantId") UUID tenantId,
                                              @Param("deviceProfileId") UUID deviceProfileId);

    @Query(value = "SELECT DISTINCT attribute_key FROM attribute_kv WHERE entity_type = 'DEVICE' " +
            "AND entity_id in (SELECT id FROM device WHERE tenant_id = :tenantId limit 100) ORDER BY attribute_key", nativeQuery = true)
    List<String> findAllKeysByTenantId(@Param("tenantId") UUID tenantId);

    @Query(value = "SELECT DISTINCT attribute_key FROM attribute_kv WHERE entity_type = :entityType " +
            "AND entity_id in :entityIds ORDER BY attribute_key", nativeQuery = true)
    List<String> findAllKeysByEntityIds(@Param("entityType") String entityType, @Param("entityIds") List<UUID> entityIds);
}

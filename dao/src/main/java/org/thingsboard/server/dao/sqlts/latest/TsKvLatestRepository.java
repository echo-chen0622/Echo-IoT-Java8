package org.thingsboard.server.dao.sqlts.latest;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sqlts.latest.TsKvLatestCompositeKey;
import org.thingsboard.server.dao.model.sqlts.latest.TsKvLatestEntity;

import java.util.List;
import java.util.UUID;

public interface TsKvLatestRepository extends JpaRepository<TsKvLatestEntity, TsKvLatestCompositeKey> {

    @Query(value = "SELECT DISTINCT ts_kv_dictionary.key AS strKey FROM ts_kv_latest " +
            "INNER JOIN ts_kv_dictionary ON ts_kv_latest.key = ts_kv_dictionary.key_id " +
            "WHERE ts_kv_latest.entity_id IN (SELECT id FROM device WHERE device_profile_id = :device_profile_id AND tenant_id = :tenant_id limit 100) ORDER BY ts_kv_dictionary.key", nativeQuery = true)
    List<String> getKeysByDeviceProfileId(@Param("tenant_id") UUID tenantId, @Param("device_profile_id") UUID deviceProfileId);

    @Query(value = "SELECT DISTINCT ts_kv_dictionary.key AS strKey FROM ts_kv_latest " +
            "INNER JOIN ts_kv_dictionary ON ts_kv_latest.key = ts_kv_dictionary.key_id " +
            "WHERE ts_kv_latest.entity_id IN (SELECT id FROM device WHERE tenant_id = :tenant_id limit 100) ORDER BY ts_kv_dictionary.key", nativeQuery = true)
    List<String> getKeysByTenantId(@Param("tenant_id") UUID tenantId);

    @Query(value = "SELECT DISTINCT ts_kv_dictionary.key AS strKey FROM ts_kv_latest " +
            "INNER JOIN ts_kv_dictionary ON ts_kv_latest.key = ts_kv_dictionary.key_id " +
            "WHERE ts_kv_latest.entity_id IN :entityIds ORDER BY ts_kv_dictionary.key", nativeQuery = true)
    List<String> findAllKeysByEntityIds(@Param("entityIds") List<UUID> entityIds);

}

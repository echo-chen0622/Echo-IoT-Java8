package org.thingsboard.server.dao.sql.ota;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.OtaPackageEntity;

import java.util.UUID;

public interface OtaPackageRepository extends JpaRepository<OtaPackageEntity, UUID> {
    @Query(value = "SELECT COALESCE(SUM(ota.data_size), 0) FROM ota_package ota WHERE ota.tenant_id = :tenantId AND ota.data IS NOT NULL", nativeQuery = true)
    Long sumDataSizeByTenantId(@Param("tenantId") UUID tenantId);
}

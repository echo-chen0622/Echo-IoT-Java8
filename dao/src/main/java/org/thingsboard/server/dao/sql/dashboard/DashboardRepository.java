package org.thingsboard.server.dao.sql.dashboard;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.ExportableEntityRepository;
import org.thingsboard.server.dao.model.sql.DashboardEntity;

import java.util.List;
import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
public interface DashboardRepository extends JpaRepository<DashboardEntity, UUID>, ExportableEntityRepository<DashboardEntity> {

    Long countByTenantId(UUID tenantId);

    List<DashboardEntity> findByTenantIdAndTitle(UUID tenantId, String title);

    Page<DashboardEntity> findByTenantId(UUID tenantId, Pageable pageable);

    @Query("SELECT externalId FROM DashboardEntity WHERE id = :id")
    UUID getExternalIdById(@Param("id") UUID id);

}

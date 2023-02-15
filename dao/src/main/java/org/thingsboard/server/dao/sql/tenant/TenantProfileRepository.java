package org.thingsboard.server.dao.sql.tenant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.dao.model.sql.TenantProfileEntity;

import java.util.UUID;

public interface TenantProfileRepository extends JpaRepository<TenantProfileEntity, UUID> {

    @Query("SELECT new org.thingsboard.server.common.data.EntityInfo(t.id, 'TENANT_PROFILE', t.name) " +
            "FROM TenantProfileEntity t " +
            "WHERE t.id = :tenantProfileId")
    EntityInfo findTenantProfileInfoById(@Param("tenantProfileId") UUID tenantProfileId);

    @Query("SELECT t FROM TenantProfileEntity t WHERE " +
            "LOWER(t.searchText) LIKE LOWER(CONCAT('%', :textSearch, '%'))")
    Page<TenantProfileEntity> findTenantProfiles(@Param("textSearch") String textSearch,
                                                 Pageable pageable);

    @Query("SELECT new org.thingsboard.server.common.data.EntityInfo(t.id, 'TENANT_PROFILE', t.name) " +
            "FROM TenantProfileEntity t " +
            "WHERE LOWER(t.searchText) LIKE LOWER(CONCAT('%', :textSearch, '%'))")
    Page<EntityInfo> findTenantProfileInfos(@Param("textSearch") String textSearch,
                                            Pageable pageable);

    @Query("SELECT t FROM TenantProfileEntity t " +
            "WHERE t.isDefault = true")
    TenantProfileEntity findByDefaultTrue();

    @Query("SELECT new org.thingsboard.server.common.data.EntityInfo(t.id, 'TENANT_PROFILE', t.name) " +
            "FROM TenantProfileEntity t " +
            "WHERE t.isDefault = true")
    EntityInfo findDefaultTenantProfileInfo();

}

package org.thingsboard.server.dao.sql.resource;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.TbResourceEntity;

import java.util.List;
import java.util.UUID;

public interface TbResourceRepository extends JpaRepository<TbResourceEntity, UUID> {

    TbResourceEntity findByTenantIdAndResourceTypeAndResourceKey(UUID tenantId, String resourceType, String resourceKey);

    Page<TbResourceEntity> findAllByTenantId(UUID tenantId, Pageable pageable);

    @Query("SELECT tr FROM TbResourceEntity tr " +
            "WHERE tr.resourceType = :resourceType " +
            "AND LOWER(tr.searchText) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
            "AND (tr.tenantId = :tenantId " +
            "OR (tr.tenantId = :systemAdminId " +
            "AND NOT EXISTS " +
            "(SELECT sr FROM TbResourceEntity sr " +
            "WHERE sr.tenantId = :tenantId " +
            "AND sr.resourceType = :resourceType " +
            "AND tr.resourceKey = sr.resourceKey)))")
    Page<TbResourceEntity> findResourcesPage(
            @Param("tenantId") UUID tenantId,
            @Param("systemAdminId") UUID sysAdminId,
            @Param("resourceType") String resourceType,
            @Param("searchText") String search,
            Pageable pageable);

    @Query("SELECT tr FROM TbResourceEntity tr " +
            "WHERE tr.resourceType = :resourceType " +
            "AND LOWER(tr.searchText) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
            "AND (tr.tenantId = :tenantId " +
            "OR (tr.tenantId = :systemAdminId " +
            "AND NOT EXISTS " +
            "(SELECT sr FROM TbResourceEntity sr " +
            "WHERE sr.tenantId = :tenantId " +
            "AND sr.resourceType = :resourceType " +
            "AND tr.resourceKey = sr.resourceKey)))")
    List<TbResourceEntity> findResources(@Param("tenantId") UUID tenantId,
                                         @Param("systemAdminId") UUID sysAdminId,
                                         @Param("resourceType") String resourceType,
                                         @Param("searchText") String search);

    @Query("SELECT tr FROM TbResourceEntity tr " +
            "WHERE tr.resourceType = :resourceType " +
            "AND tr.resourceKey in (:resourceIds) " +
            "AND (tr.tenantId = :tenantId " +
            "OR (tr.tenantId = :systemAdminId " +
            "AND NOT EXISTS " +
            "(SELECT sr FROM TbResourceEntity sr " +
            "WHERE sr.tenantId = :tenantId " +
            "AND sr.resourceType = :resourceType " +
            "AND tr.resourceKey = sr.resourceKey)))")
    List<TbResourceEntity> findResourcesByIds(@Param("tenantId") UUID tenantId,
                                              @Param("systemAdminId") UUID sysAdminId,
                                              @Param("resourceType") String resourceType,
                                              @Param("resourceIds") String[] objectIds);

    @Query(value = "SELECT COALESCE(SUM(LENGTH(r.data)), 0) FROM resource r WHERE r.tenant_id = :tenantId", nativeQuery = true)
    Long sumDataSizeByTenantId(@Param("tenantId") UUID tenantId);
}

package org.thingsboard.server.dao.sql.resource;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.TbResourceInfoEntity;

import java.util.UUID;

public interface TbResourceInfoRepository extends JpaRepository<TbResourceInfoEntity, UUID> {

    @Query("SELECT tr FROM TbResourceInfoEntity tr WHERE " +
            "LOWER(tr.title) LIKE LOWER(CONCAT('%', :searchText, '%'))" +
            "AND (tr.tenantId = :tenantId " +
            "OR (tr.tenantId = :systemAdminId " +
            "AND NOT EXISTS " +
            "(SELECT sr FROM TbResourceEntity sr " +
            "WHERE sr.tenantId = :tenantId " +
            "AND tr.resourceType = sr.resourceType " +
            "AND tr.resourceKey = sr.resourceKey)))")
    Page<TbResourceInfoEntity> findAllTenantResourcesByTenantId(@Param("tenantId") UUID tenantId,
                                                                @Param("systemAdminId") UUID sysadminId,
                                                                @Param("searchText") String searchText,
                                                                Pageable pageable);

    @Query("SELECT ri FROM TbResourceInfoEntity ri WHERE " +
            "ri.tenantId = :tenantId " +
            "AND LOWER(ri.title) LIKE LOWER(CONCAT('%', :searchText, '%'))")
    Page<TbResourceInfoEntity> findTenantResourcesByTenantId(@Param("tenantId") UUID tenantId,
                                                             @Param("searchText") String searchText,
                                                             Pageable pageable);
}

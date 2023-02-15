package org.thingsboard.server.dao.sql.widget;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.WidgetTypeDetailsEntity;
import org.thingsboard.server.dao.model.sql.WidgetTypeEntity;
import org.thingsboard.server.dao.model.sql.WidgetTypeInfoEntity;

import java.util.List;
import java.util.UUID;

public interface WidgetTypeRepository extends JpaRepository<WidgetTypeDetailsEntity, UUID> {

    @Query("SELECT wt FROM WidgetTypeEntity wt WHERE wt.id = :widgetTypeId")
    WidgetTypeEntity findWidgetTypeById(@Param("widgetTypeId") UUID widgetTypeId);

    @Query("SELECT wt FROM WidgetTypeEntity wt WHERE wt.tenantId = :tenantId AND wt.bundleAlias = :bundleAlias")
    List<WidgetTypeEntity> findWidgetTypesByTenantIdAndBundleAlias(@Param("tenantId") UUID tenantId,
                                                                   @Param("bundleAlias") String bundleAlias);

    @Query("SELECT new org.thingsboard.server.dao.model.sql.WidgetTypeInfoEntity(wtd) FROM WidgetTypeDetailsEntity wtd " +
            "WHERE wtd.tenantId = :tenantId AND wtd.bundleAlias = :bundleAlias")
    List<WidgetTypeInfoEntity> findWidgetTypesInfosByTenantIdAndBundleAlias(@Param("tenantId") UUID tenantId,
                                                                            @Param("bundleAlias") String bundleAlias);

    List<WidgetTypeDetailsEntity> findByTenantIdAndBundleAlias(UUID tenantId, String bundleAlias);

    @Query("SELECT wt FROM WidgetTypeEntity wt " +
            "WHERE wt.tenantId = :tenantId AND wt.bundleAlias = :bundleAlias AND wt.alias = :alias")
    WidgetTypeEntity findWidgetTypeByTenantIdAndBundleAliasAndAlias(@Param("tenantId") UUID tenantId,
                                                          @Param("bundleAlias") String bundleAlias,
                                                          @Param("alias") String alias);
}

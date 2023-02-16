package org.echoiot.server.dao.sql.entityview;

import org.echoiot.server.dao.ExportableEntityRepository;
import org.echoiot.server.dao.model.sql.EntityViewEntity;
import org.echoiot.server.dao.model.sql.EntityViewInfoEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Created by Victor Basanets on 8/31/2017.
 */
public interface EntityViewRepository extends JpaRepository<EntityViewEntity, UUID>, ExportableEntityRepository<EntityViewEntity> {

    @Query("SELECT new org.echoiot.server.dao.model.sql.EntityViewInfoEntity(e, c.title, c.additionalInfo) " +
            "FROM EntityViewEntity e " +
            "LEFT JOIN CustomerEntity c on c.id = e.customerId " +
            "WHERE e.id = :entityViewId")
    EntityViewInfoEntity findEntityViewInfoById(@Param("entityViewId") UUID entityViewId);

    @Query("SELECT e FROM EntityViewEntity e WHERE e.tenantId = :tenantId " +
            "AND LOWER(e.searchText) LIKE LOWER(CONCAT('%', :textSearch, '%'))")
    Page<EntityViewEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                          @Param("textSearch") String textSearch,
                                          Pageable pageable);

    @Query("SELECT new org.echoiot.server.dao.model.sql.EntityViewInfoEntity(e, c.title, c.additionalInfo) " +
            "FROM EntityViewEntity e " +
            "LEFT JOIN CustomerEntity c on c.id = e.customerId " +
            "WHERE e.tenantId = :tenantId " +
            "AND LOWER(e.searchText) LIKE LOWER(CONCAT('%', :textSearch, '%'))")
    Page<EntityViewInfoEntity> findEntityViewInfosByTenantId(@Param("tenantId") UUID tenantId,
                                                             @Param("textSearch") String textSearch,
                                                             Pageable pageable);

    @Query("SELECT e FROM EntityViewEntity e WHERE e.tenantId = :tenantId " +
            "AND e.type = :type " +
            "AND LOWER(e.searchText) LIKE LOWER(CONCAT('%', :textSearch, '%'))")
    Page<EntityViewEntity> findByTenantIdAndType(@Param("tenantId") UUID tenantId,
                                                 @Param("type") String type,
                                                 @Param("textSearch") String textSearch,
                                                 Pageable pageable);

    @Query("SELECT new org.echoiot.server.dao.model.sql.EntityViewInfoEntity(e, c.title, c.additionalInfo) " +
            "FROM EntityViewEntity e " +
            "LEFT JOIN CustomerEntity c on c.id = e.customerId " +
            "WHERE e.tenantId = :tenantId " +
            "AND e.type = :type " +
            "AND LOWER(e.searchText) LIKE LOWER(CONCAT('%', :textSearch, '%'))")
    Page<EntityViewInfoEntity> findEntityViewInfosByTenantIdAndType(@Param("tenantId") UUID tenantId,
                                                                    @Param("type") String type,
                                                                    @Param("textSearch") String textSearch,
                                                                    Pageable pageable);

    @Query("SELECT e FROM EntityViewEntity e WHERE e.tenantId = :tenantId " +
            "AND e.customerId = :customerId " +
            "AND LOWER(e.searchText) LIKE LOWER(CONCAT('%', :searchText, '%'))")
    Page<EntityViewEntity> findByTenantIdAndCustomerId(@Param("tenantId") UUID tenantId,
                                                       @Param("customerId") UUID customerId,
                                                       @Param("searchText") String searchText,
                                                       Pageable pageable);

    @Query("SELECT new org.echoiot.server.dao.model.sql.EntityViewInfoEntity(e, c.title, c.additionalInfo) " +
            "FROM EntityViewEntity e " +
            "LEFT JOIN CustomerEntity c on c.id = e.customerId " +
            "WHERE e.tenantId = :tenantId " +
            "AND e.customerId = :customerId " +
            "AND LOWER(e.searchText) LIKE LOWER(CONCAT('%', :searchText, '%'))")
    Page<EntityViewInfoEntity> findEntityViewInfosByTenantIdAndCustomerId(@Param("tenantId") UUID tenantId,
                                                                          @Param("customerId") UUID customerId,
                                                                          @Param("searchText") String searchText,
                                                                          Pageable pageable);

    @Query("SELECT e FROM EntityViewEntity e WHERE e.tenantId = :tenantId " +
            "AND e.customerId = :customerId " +
            "AND e.type = :type " +
            "AND LOWER(e.searchText) LIKE LOWER(CONCAT('%', :searchText, '%'))")
    Page<EntityViewEntity> findByTenantIdAndCustomerIdAndType(@Param("tenantId") UUID tenantId,
                                                              @Param("customerId") UUID customerId,
                                                              @Param("type") String type,
                                                              @Param("searchText") String searchText,
                                                              Pageable pageable);

    @Query("SELECT new org.echoiot.server.dao.model.sql.EntityViewInfoEntity(e, c.title, c.additionalInfo) " +
            "FROM EntityViewEntity e " +
            "LEFT JOIN CustomerEntity c on c.id = e.customerId " +
            "WHERE e.tenantId = :tenantId " +
            "AND e.customerId = :customerId " +
            "AND e.type = :type " +
            "AND LOWER(e.searchText) LIKE LOWER(CONCAT('%', :textSearch, '%'))")
    Page<EntityViewInfoEntity> findEntityViewInfosByTenantIdAndCustomerIdAndType(@Param("tenantId") UUID tenantId,
                                                                                 @Param("customerId") UUID customerId,
                                                                                 @Param("type") String type,
                                                                                 @Param("textSearch") String textSearch,
                                                                                 Pageable pageable);

    EntityViewEntity findByTenantIdAndName(UUID tenantId, String name);

    List<EntityViewEntity> findAllByTenantIdAndEntityId(UUID tenantId, UUID entityId);

    @Query("SELECT DISTINCT ev.type FROM EntityViewEntity ev WHERE ev.tenantId = :tenantId")
    List<String> findTenantEntityViewTypes(@Param("tenantId") UUID tenantId);

    @Query("SELECT ev FROM EntityViewEntity ev, RelationEntity re WHERE ev.tenantId = :tenantId " +
            "AND ev.id = re.toId AND re.toType = 'ENTITY_VIEW' AND re.relationTypeGroup = 'EDGE' " +
            "AND re.relationType = 'Contains' AND re.fromId = :edgeId AND re.fromType = 'EDGE' " +
            "AND LOWER(ev.searchText) LIKE LOWER(CONCAT('%', :searchText, '%'))")
    Page<EntityViewEntity> findByTenantIdAndEdgeId(@Param("tenantId") UUID tenantId,
                                               @Param("edgeId") UUID edgeId,
                                               @Param("searchText") String searchText,
                                               Pageable pageable);

    @Query("SELECT ev FROM EntityViewEntity ev, RelationEntity re WHERE ev.tenantId = :tenantId " +
            "AND ev.id = re.toId AND re.toType = 'ENTITY_VIEW' AND re.relationTypeGroup = 'EDGE' " +
            "AND re.relationType = 'Contains' AND re.fromId = :edgeId AND re.fromType = 'EDGE' " +
            "AND ev.type = :type " +
            "AND LOWER(ev.searchText) LIKE LOWER(CONCAT('%', :searchText, '%'))")
    Page<EntityViewEntity> findByTenantIdAndEdgeIdAndType(@Param("tenantId") UUID tenantId,
                                                   @Param("edgeId") UUID edgeId,
                                                   @Param("type") String type,
                                                   @Param("searchText") String searchText,
                                                   Pageable pageable);
    @Query("SELECT externalId FROM EntityViewEntity WHERE id = :id")
    UUID getExternalIdById(@Param("id") UUID id);

}
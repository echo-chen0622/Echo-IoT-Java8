package org.thingsboard.server.dao.sql.edge;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.EdgeEventEntity;

import java.util.UUID;

public interface EdgeEventRepository extends JpaRepository<EdgeEventEntity, UUID>, JpaSpecificationExecutor<EdgeEventEntity> {

    @Query("SELECT e FROM EdgeEventEntity e WHERE " +
            "e.tenantId = :tenantId " +
            "AND e.edgeId = :edgeId " +
            "AND (:startTime IS NULL OR e.createdTime > :startTime) " +
            "AND (:endTime IS NULL OR e.createdTime <= :endTime) " +
            "AND LOWER(e.edgeEventType) LIKE LOWER(CONCAT('%', :textSearch, '%'))"
    )
    Page<EdgeEventEntity> findEdgeEventsByTenantIdAndEdgeId(@Param("tenantId") UUID tenantId,
                                                            @Param("edgeId") UUID edgeId,
                                                            @Param("textSearch") String textSearch,
                                                            @Param("startTime") Long startTime,
                                                            @Param("endTime") Long endTime,
                                                            Pageable pageable);

    @Query("SELECT e FROM EdgeEventEntity e WHERE " +
            "e.tenantId = :tenantId " +
            "AND e.edgeId = :edgeId " +
            "AND (:startTime IS NULL OR e.createdTime > :startTime) " +
            "AND (:endTime IS NULL OR e.createdTime <= :endTime) " +
            "AND e.edgeEventAction <> 'TIMESERIES_UPDATED' " +
            "AND LOWER(e.edgeEventType) LIKE LOWER(CONCAT('%', :textSearch, '%'))"
    )
    Page<EdgeEventEntity> findEdgeEventsByTenantIdAndEdgeIdWithoutTimeseriesUpdated(@Param("tenantId") UUID tenantId,
                                                                                    @Param("edgeId") UUID edgeId,
                                                                                    @Param("textSearch") String textSearch,
                                                                                    @Param("startTime") Long startTime,
                                                                                    @Param("endTime") Long endTime,
                                                                                    Pageable pageable);
}

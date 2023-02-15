package org.thingsboard.server.dao.sql.device;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.DeviceIdInfo;
import org.thingsboard.server.dao.ExportableEntityRepository;
import org.thingsboard.server.dao.model.sql.DeviceEntity;
import org.thingsboard.server.dao.model.sql.DeviceInfoEntity;

import java.util.List;
import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
public interface DeviceRepository extends JpaRepository<DeviceEntity, UUID>, ExportableEntityRepository<DeviceEntity> {

    @Query("SELECT new org.thingsboard.server.dao.model.sql.DeviceInfoEntity(d, c.title, c.additionalInfo, p.name) " +
            "FROM DeviceEntity d " +
            "LEFT JOIN CustomerEntity c on c.id = d.customerId " +
            "LEFT JOIN DeviceProfileEntity p on p.id = d.deviceProfileId " +
            "WHERE d.id = :deviceId")
    DeviceInfoEntity findDeviceInfoById(@Param("deviceId") UUID deviceId);

    @Query("SELECT d FROM DeviceEntity d WHERE d.tenantId = :tenantId " +
            "AND d.customerId = :customerId " +
            "AND LOWER(d.searchText) LIKE LOWER(CONCAT('%', :searchText, '%'))")
    Page<DeviceEntity> findByTenantIdAndCustomerId(@Param("tenantId") UUID tenantId,
                                                   @Param("customerId") UUID customerId,
                                                   @Param("searchText") String searchText,
                                                   Pageable pageable);

    @Query("SELECT d FROM DeviceEntity d WHERE d.tenantId = :tenantId " +
            "AND d.deviceProfileId = :profileId " +
            "AND LOWER(d.searchText) LIKE LOWER(CONCAT('%', :searchText, '%'))")
    Page<DeviceEntity> findByTenantIdAndProfileId(@Param("tenantId") UUID tenantId,
                                                  @Param("profileId") UUID profileId,
                                                  @Param("searchText") String searchText,
                                                  Pageable pageable);

    @Query("SELECT new org.thingsboard.server.dao.model.sql.DeviceInfoEntity(d, c.title, c.additionalInfo, p.name) " +
            "FROM DeviceEntity d " +
            "LEFT JOIN CustomerEntity c on c.id = d.customerId " +
            "LEFT JOIN DeviceProfileEntity p on p.id = d.deviceProfileId " +
            "WHERE d.tenantId = :tenantId " +
            "AND d.customerId = :customerId " +
            "AND LOWER(d.searchText) LIKE LOWER(CONCAT('%', :searchText, '%'))")
    Page<DeviceInfoEntity> findDeviceInfosByTenantIdAndCustomerId(@Param("tenantId") UUID tenantId,
                                                                  @Param("customerId") UUID customerId,
                                                                  @Param("searchText") String searchText,
                                                                  Pageable pageable);

    @Query("SELECT d FROM DeviceEntity d WHERE d.tenantId = :tenantId")
    Page<DeviceEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                      Pageable pageable);

    @Query("SELECT d FROM DeviceEntity d WHERE d.tenantId = :tenantId " +
            "AND LOWER(d.searchText) LIKE LOWER(CONCAT('%', :textSearch, '%'))")
    Page<DeviceEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                      @Param("textSearch") String textSearch,
                                      Pageable pageable);

    @Query("SELECT new org.thingsboard.server.dao.model.sql.DeviceInfoEntity(d, c.title, c.additionalInfo, p.name) " +
            "FROM DeviceEntity d " +
            "LEFT JOIN CustomerEntity c on c.id = d.customerId " +
            "LEFT JOIN DeviceProfileEntity p on p.id = d.deviceProfileId " +
            "WHERE d.tenantId = :tenantId " +
            "AND (LOWER(d.searchText) LIKE LOWER(CONCAT('%', :textSearch, '%')) " +
            "OR LOWER(d.label) LIKE LOWER(CONCAT('%', :textSearch, '%')) " +
            "OR LOWER(p.searchText) LIKE LOWER(CONCAT('%', :textSearch, '%')) " +
            "OR LOWER(c.searchText) LIKE LOWER(CONCAT('%', :textSearch, '%')))")
    Page<DeviceInfoEntity> findDeviceInfosByTenantId(@Param("tenantId") UUID tenantId,
                                                     @Param("textSearch") String textSearch,
                                                     Pageable pageable);

    @Query("SELECT d FROM DeviceEntity d WHERE d.tenantId = :tenantId " +
            "AND d.type = :type " +
            "AND LOWER(d.searchText) LIKE LOWER(CONCAT('%', :textSearch, '%'))")
    Page<DeviceEntity> findByTenantIdAndType(@Param("tenantId") UUID tenantId,
                                             @Param("type") String type,
                                             @Param("textSearch") String textSearch,
                                             Pageable pageable);

    @Query("SELECT d FROM DeviceEntity d WHERE d.tenantId = :tenantId " +
            "AND d.deviceProfileId = :deviceProfileId " +
            "AND d.firmwareId = null " +
            "AND LOWER(d.searchText) LIKE LOWER(CONCAT('%', :textSearch, '%'))")
    Page<DeviceEntity> findByTenantIdAndTypeAndFirmwareIdIsNull(@Param("tenantId") UUID tenantId,
                                             @Param("deviceProfileId") UUID deviceProfileId,
                                             @Param("textSearch") String textSearch,
                                             Pageable pageable);

    @Query("SELECT d FROM DeviceEntity d WHERE d.tenantId = :tenantId " +
            "AND d.deviceProfileId = :deviceProfileId " +
            "AND d.softwareId = null " +
            "AND LOWER(d.searchText) LIKE LOWER(CONCAT('%', :textSearch, '%'))")
    Page<DeviceEntity> findByTenantIdAndTypeAndSoftwareIdIsNull(@Param("tenantId") UUID tenantId,
                                                                @Param("deviceProfileId") UUID deviceProfileId,
                                                                @Param("textSearch") String textSearch,
                                                                Pageable pageable);

    @Query("SELECT count(*) FROM DeviceEntity d WHERE d.tenantId = :tenantId " +
            "AND d.deviceProfileId = :deviceProfileId " +
            "AND d.firmwareId = null")
    Long countByTenantIdAndDeviceProfileIdAndFirmwareIdIsNull(@Param("tenantId") UUID tenantId,
                                                              @Param("deviceProfileId") UUID deviceProfileId);

    @Query("SELECT count(*) FROM DeviceEntity d WHERE d.tenantId = :tenantId " +
            "AND d.deviceProfileId = :deviceProfileId " +
            "AND d.softwareId = null")
    Long countByTenantIdAndDeviceProfileIdAndSoftwareIdIsNull(@Param("tenantId") UUID tenantId,
                                                              @Param("deviceProfileId") UUID deviceProfileId);

    @Query("SELECT new org.thingsboard.server.dao.model.sql.DeviceInfoEntity(d, c.title, c.additionalInfo, p.name) " +
            "FROM DeviceEntity d " +
            "LEFT JOIN CustomerEntity c on c.id = d.customerId " +
            "LEFT JOIN DeviceProfileEntity p on p.id = d.deviceProfileId " +
            "WHERE d.tenantId = :tenantId " +
            "AND d.type = :type " +
            "AND (LOWER(d.searchText) LIKE LOWER(CONCAT('%', :textSearch, '%')) " +
            "OR LOWER(d.label) LIKE LOWER(CONCAT('%', :textSearch, '%')) " +
            "OR LOWER(c.searchText) LIKE LOWER(CONCAT('%', :textSearch, '%')))")
    Page<DeviceInfoEntity> findDeviceInfosByTenantIdAndType(@Param("tenantId") UUID tenantId,
                                                            @Param("type") String type,
                                                            @Param("textSearch") String textSearch,
                                                            Pageable pageable);

    @Query("SELECT new org.thingsboard.server.dao.model.sql.DeviceInfoEntity(d, c.title, c.additionalInfo, p.name) " +
            "FROM DeviceEntity d " +
            "LEFT JOIN CustomerEntity c on c.id = d.customerId " +
            "LEFT JOIN DeviceProfileEntity p on p.id = d.deviceProfileId " +
            "WHERE d.tenantId = :tenantId " +
            "AND d.deviceProfileId = :deviceProfileId " +
            "AND (LOWER(d.searchText) LIKE LOWER(CONCAT('%', :textSearch, '%')) " +
            "OR LOWER(d.label) LIKE LOWER(CONCAT('%', :textSearch, '%')) " +
            "OR LOWER(c.searchText) LIKE LOWER(CONCAT('%', :textSearch, '%')))")
    Page<DeviceInfoEntity> findDeviceInfosByTenantIdAndDeviceProfileId(@Param("tenantId") UUID tenantId,
                                                                       @Param("deviceProfileId") UUID deviceProfileId,
                                                                       @Param("textSearch") String textSearch,
                                                                       Pageable pageable);

    @Query("SELECT d FROM DeviceEntity d WHERE d.tenantId = :tenantId " +
            "AND d.customerId = :customerId " +
            "AND d.type = :type " +
            "AND LOWER(d.searchText) LIKE LOWER(CONCAT('%', :textSearch, '%'))")
    Page<DeviceEntity> findByTenantIdAndCustomerIdAndType(@Param("tenantId") UUID tenantId,
                                                          @Param("customerId") UUID customerId,
                                                          @Param("type") String type,
                                                          @Param("textSearch") String textSearch,
                                                          Pageable pageable);

    @Query("SELECT new org.thingsboard.server.dao.model.sql.DeviceInfoEntity(d, c.title, c.additionalInfo, p.name) " +
            "FROM DeviceEntity d " +
            "LEFT JOIN CustomerEntity c on c.id = d.customerId " +
            "LEFT JOIN DeviceProfileEntity p on p.id = d.deviceProfileId " +
            "WHERE d.tenantId = :tenantId " +
            "AND d.customerId = :customerId " +
            "AND d.type = :type " +
            "AND LOWER(d.searchText) LIKE LOWER(CONCAT('%', :textSearch, '%'))")
    Page<DeviceInfoEntity> findDeviceInfosByTenantIdAndCustomerIdAndType(@Param("tenantId") UUID tenantId,
                                                                         @Param("customerId") UUID customerId,
                                                                         @Param("type") String type,
                                                                         @Param("textSearch") String textSearch,
                                                                         Pageable pageable);

    @Query("SELECT new org.thingsboard.server.dao.model.sql.DeviceInfoEntity(d, c.title, c.additionalInfo, p.name) " +
            "FROM DeviceEntity d " +
            "LEFT JOIN CustomerEntity c on c.id = d.customerId " +
            "LEFT JOIN DeviceProfileEntity p on p.id = d.deviceProfileId " +
            "WHERE d.tenantId = :tenantId " +
            "AND d.customerId = :customerId " +
            "AND d.deviceProfileId = :deviceProfileId " +
            "AND LOWER(d.searchText) LIKE LOWER(CONCAT('%', :textSearch, '%'))")
    Page<DeviceInfoEntity> findDeviceInfosByTenantIdAndCustomerIdAndDeviceProfileId(@Param("tenantId") UUID tenantId,
                                                                                    @Param("customerId") UUID customerId,
                                                                                    @Param("deviceProfileId") UUID deviceProfileId,
                                                                                    @Param("textSearch") String textSearch,
                                                                                    Pageable pageable);

    @Query("SELECT DISTINCT d.type FROM DeviceEntity d WHERE d.tenantId = :tenantId")
    List<String> findTenantDeviceTypes(@Param("tenantId") UUID tenantId);

    DeviceEntity findByTenantIdAndName(UUID tenantId, String name);

    List<DeviceEntity> findDevicesByTenantIdAndCustomerIdAndIdIn(UUID tenantId, UUID customerId, List<UUID> deviceIds);

    List<DeviceEntity> findDevicesByTenantIdAndIdIn(UUID tenantId, List<UUID> deviceIds);

    List<DeviceEntity> findDevicesByIdIn(List<UUID> deviceIds);

    DeviceEntity findByTenantIdAndId(UUID tenantId, UUID id);

    Long countByDeviceProfileId(UUID deviceProfileId);

    @Query("SELECT d FROM DeviceEntity d, RelationEntity re WHERE d.tenantId = :tenantId " +
            "AND d.id = re.toId AND re.toType = 'DEVICE' AND re.relationTypeGroup = 'EDGE' " +
            "AND re.relationType = 'Contains' AND re.fromId = :edgeId AND re.fromType = 'EDGE' " +
            "AND LOWER(d.searchText) LIKE LOWER(CONCAT('%', :searchText, '%'))")
    Page<DeviceEntity> findByTenantIdAndEdgeId(@Param("tenantId") UUID tenantId,
                                               @Param("edgeId") UUID edgeId,
                                               @Param("searchText") String searchText,
                                               Pageable pageable);

    @Query("SELECT d FROM DeviceEntity d, RelationEntity re WHERE d.tenantId = :tenantId " +
            "AND d.id = re.toId AND re.toType = 'DEVICE' AND re.relationTypeGroup = 'EDGE' " +
            "AND re.relationType = 'Contains' AND re.fromId = :edgeId AND re.fromType = 'EDGE' " +
            "AND d.type = :type " +
            "AND LOWER(d.searchText) LIKE LOWER(CONCAT('%', :searchText, '%'))")
    Page<DeviceEntity> findByTenantIdAndEdgeIdAndType(@Param("tenantId") UUID tenantId,
                                                      @Param("edgeId") UUID edgeId,
                                                      @Param("type") String type,
                                                      @Param("searchText") String searchText,
                                                      Pageable pageable);

    /**
     * Count devices by tenantId.
     * Custom query applied because default QueryDSL produces slow count(id).
     * <p>
     * There is two way to count devices.
     * OPTIMAL: count(*)
     *   - returns _row_count_ and use index-only scan (super fast).
     * SLOW: count(id)
     *   - returns _NON_NULL_id_count and performs table scan to verify isNull for each id in filtered rows.
     * */
    @Query("SELECT count(*) FROM DeviceEntity d WHERE d.tenantId = :tenantId")
    Long countByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT d.id FROM DeviceEntity d " +
            "INNER JOIN DeviceProfileEntity p ON d.deviceProfileId = p.id " +
            "WHERE p.transportType = :transportType")
    Page<UUID> findIdsByDeviceProfileTransportType(@Param("transportType") DeviceTransportType transportType, Pageable pageable);

    @Query("SELECT externalId FROM DeviceEntity WHERE id = :id")
    UUID getExternalIdById(@Param("id") UUID id);

}

/**
 * Copyright © 2016-2023 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.sql.device;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.DeviceProfileInfo;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.dao.ExportableEntityRepository;
import org.thingsboard.server.dao.model.sql.DeviceProfileEntity;

import java.util.UUID;

public interface DeviceProfileRepository extends JpaRepository<DeviceProfileEntity, UUID>, ExportableEntityRepository<DeviceProfileEntity> {

    @Query("SELECT new org.thingsboard.server.common.data.DeviceProfileInfo(d.id, d.name, d.image, d.defaultDashboardId, d.type, d.transportType) " +
            "FROM DeviceProfileEntity d " +
            "WHERE d.id = :deviceProfileId")
    DeviceProfileInfo findDeviceProfileInfoById(@Param("deviceProfileId") UUID deviceProfileId);

    @Query("SELECT d FROM DeviceProfileEntity d WHERE " +
            "d.tenantId = :tenantId AND LOWER(d.searchText) LIKE LOWER(CONCAT('%', :textSearch, '%'))")
    Page<DeviceProfileEntity> findDeviceProfiles(@Param("tenantId") UUID tenantId,
                                                 @Param("textSearch") String textSearch,
                                                 Pageable pageable);

    @Query("SELECT new org.thingsboard.server.common.data.DeviceProfileInfo(d.id, d.name, d.image, d.defaultDashboardId, d.type, d.transportType) " +
            "FROM DeviceProfileEntity d WHERE " +
            "d.tenantId = :tenantId AND LOWER(d.searchText) LIKE LOWER(CONCAT('%', :textSearch, '%'))")
    Page<DeviceProfileInfo> findDeviceProfileInfos(@Param("tenantId") UUID tenantId,
                                                   @Param("textSearch") String textSearch,
                                                   Pageable pageable);

    @Query("SELECT new org.thingsboard.server.common.data.DeviceProfileInfo(d.id, d.name, d.image, d.defaultDashboardId, d.type, d.transportType) " +
            "FROM DeviceProfileEntity d WHERE " +
            "d.tenantId = :tenantId AND d.transportType = :transportType AND LOWER(d.searchText) LIKE LOWER(CONCAT('%', :textSearch, '%'))")
    Page<DeviceProfileInfo> findDeviceProfileInfos(@Param("tenantId") UUID tenantId,
                                                   @Param("textSearch") String textSearch,
                                                   @Param("transportType") DeviceTransportType transportType,
                                                   Pageable pageable);

    @Query("SELECT d FROM DeviceProfileEntity d " +
            "WHERE d.tenantId = :tenantId AND d.isDefault = true")
    DeviceProfileEntity findByDefaultTrueAndTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT new org.thingsboard.server.common.data.DeviceProfileInfo(d.id, d.name, d.image, d.defaultDashboardId, d.type, d.transportType) " +
            "FROM DeviceProfileEntity d " +
            "WHERE d.tenantId = :tenantId AND d.isDefault = true")
    DeviceProfileInfo findDefaultDeviceProfileInfo(@Param("tenantId") UUID tenantId);

    DeviceProfileEntity findByTenantIdAndName(UUID id, String profileName);

    DeviceProfileEntity findByProvisionDeviceKey(@Param("provisionDeviceKey") String provisionDeviceKey);

    @Query("SELECT externalId FROM DeviceProfileEntity WHERE id = :id")
    UUID getExternalIdById(@Param("id") UUID id);

}

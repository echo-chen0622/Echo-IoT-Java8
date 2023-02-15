package org.thingsboard.server.dao.ota;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.ota.ChecksumAlgorithm;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;

import java.nio.ByteBuffer;

public interface OtaPackageService {

    OtaPackageInfo saveOtaPackageInfo(OtaPackageInfo otaPackageInfo, boolean isUrl);

    OtaPackage saveOtaPackage(OtaPackage otaPackage);

    String generateChecksum(ChecksumAlgorithm checksumAlgorithm, ByteBuffer data);

    OtaPackage findOtaPackageById(TenantId tenantId, OtaPackageId otaPackageId);

    OtaPackageInfo findOtaPackageInfoById(TenantId tenantId, OtaPackageId otaPackageId);

    ListenableFuture<OtaPackageInfo> findOtaPackageInfoByIdAsync(TenantId tenantId, OtaPackageId otaPackageId);

    PageData<OtaPackageInfo> findTenantOtaPackagesByTenantId(TenantId tenantId, PageLink pageLink);

    PageData<OtaPackageInfo> findTenantOtaPackagesByTenantIdAndDeviceProfileIdAndTypeAndHasData(TenantId tenantId, DeviceProfileId deviceProfileId, OtaPackageType otaPackageType, PageLink pageLink);

    void deleteOtaPackage(TenantId tenantId, OtaPackageId otaPackageId);

    void deleteOtaPackagesByTenantId(TenantId tenantId);

    long sumDataSizeByTenantId(TenantId tenantId);
}

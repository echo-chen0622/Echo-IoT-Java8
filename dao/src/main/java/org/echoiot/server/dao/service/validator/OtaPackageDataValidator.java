package org.echoiot.server.dao.service.validator;

import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.OtaPackage;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.echoiot.server.dao.exception.DataValidationException;
import org.echoiot.server.dao.ota.OtaPackageDao;
import org.echoiot.server.dao.ota.OtaPackageService;
import org.echoiot.server.dao.tenant.TbTenantProfileCache;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class OtaPackageDataValidator extends BaseOtaPackageDataValidator<OtaPackage> {

    @Resource
    private OtaPackageDao otaPackageDao;

    @Resource
    @Lazy
    private OtaPackageService otaPackageService;

    @Resource
    @Lazy
    private TbTenantProfileCache tenantProfileCache;

    @Override
    protected void validateCreate(TenantId tenantId, @NotNull OtaPackage otaPackage) {
        DefaultTenantProfileConfiguration profileConfiguration =
                (DefaultTenantProfileConfiguration) tenantProfileCache.get(tenantId).getProfileData().getConfiguration();
        long maxOtaPackagesInBytes = profileConfiguration.getMaxOtaPackagesInBytes();
        validateMaxSumDataSizePerTenant(tenantId, otaPackageDao, maxOtaPackagesInBytes, otaPackage.getDataSize(), EntityType.OTA_PACKAGE);
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, @NotNull OtaPackage otaPackage) {
        validateImpl(otaPackage);

        if (!otaPackage.hasUrl()) {
            if (StringUtils.isEmpty(otaPackage.getFileName())) {
                throw new DataValidationException("OtaPackage file name should be specified!");
            }

            if (StringUtils.isEmpty(otaPackage.getContentType())) {
                throw new DataValidationException("OtaPackage content type should be specified!");
            }

            if (otaPackage.getChecksumAlgorithm() == null) {
                throw new DataValidationException("OtaPackage checksum algorithm should be specified!");
            }
            if (StringUtils.isEmpty(otaPackage.getChecksum())) {
                throw new DataValidationException("OtaPackage checksum should be specified!");
            }

            String currentChecksum;

            currentChecksum = otaPackageService.generateChecksum(otaPackage.getChecksumAlgorithm(), otaPackage.getData());

            if (!currentChecksum.equals(otaPackage.getChecksum())) {
                throw new DataValidationException("Wrong otaPackage file!");
            }
        } else {
            if (otaPackage.getData() != null) {
                throw new DataValidationException("File can't be saved if URL present!");
            }
        }
    }

    @NotNull
    @Override
    protected OtaPackage validateUpdate(TenantId tenantId, @NotNull OtaPackage otaPackage) {
        OtaPackage otaPackageOld = otaPackageDao.findById(tenantId, otaPackage.getUuidId());

        validateUpdate(otaPackage, otaPackageOld);

        if (otaPackageOld.getData() != null && !otaPackageOld.getData().equals(otaPackage.getData())) {
            throw new DataValidationException("Updating otaPackage data is prohibited!");
        }

        if (otaPackageOld.getData() == null && otaPackage.getData() != null) {
            DefaultTenantProfileConfiguration profileConfiguration =
                    (DefaultTenantProfileConfiguration) tenantProfileCache.get(tenantId).getProfileData().getConfiguration();
            long maxOtaPackagesInBytes = profileConfiguration.getMaxOtaPackagesInBytes();
            validateMaxSumDataSizePerTenant(tenantId, otaPackageDao, maxOtaPackagesInBytes, otaPackage.getDataSize(), EntityType.OTA_PACKAGE);
        }
        return otaPackageOld;
    }
}

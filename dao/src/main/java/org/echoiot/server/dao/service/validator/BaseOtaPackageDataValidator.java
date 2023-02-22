package org.echoiot.server.dao.service.validator;

import org.echoiot.server.common.data.BaseData;
import org.echoiot.server.common.data.DeviceProfile;
import org.echoiot.server.common.data.OtaPackageInfo;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.dao.device.DeviceProfileDao;
import org.echoiot.server.dao.exception.DataValidationException;
import org.echoiot.server.dao.service.DataValidator;
import org.echoiot.server.dao.tenant.TenantService;
import org.springframework.context.annotation.Lazy;

import javax.annotation.Resource;
import java.util.Objects;

public abstract class BaseOtaPackageDataValidator<D extends BaseData<?>> extends DataValidator<D> {

    @Resource
    @Lazy
    private TenantService tenantService;

    @Resource
    private DeviceProfileDao deviceProfileDao;

    protected void validateImpl(OtaPackageInfo otaPackageInfo) {
        if (otaPackageInfo.getTenantId() == null) {
            throw new DataValidationException("OtaPackage should be assigned to tenant!");
        } else {
            if (!tenantService.tenantExists(otaPackageInfo.getTenantId())) {
                throw new DataValidationException("OtaPackage is referencing to non-existent tenant!");
            }
        }

        if (otaPackageInfo.getDeviceProfileId() != null) {
            DeviceProfile deviceProfile = deviceProfileDao.findById(otaPackageInfo.getTenantId(), otaPackageInfo.getDeviceProfileId().getId());
            if (deviceProfile == null) {
                throw new DataValidationException("OtaPackage is referencing to non-existent device profile!");
            }
        }

        if (otaPackageInfo.getType() == null) {
            throw new DataValidationException("Type should be specified!");
        }

        if (StringUtils.isEmpty(otaPackageInfo.getTitle())) {
            throw new DataValidationException("OtaPackage title should be specified!");
        }

        if (StringUtils.isEmpty(otaPackageInfo.getVersion())) {
            throw new DataValidationException("OtaPackage version should be specified!");
        }

        if (otaPackageInfo.getTitle().length() > 255) {
            throw new DataValidationException("The length of title should be equal or shorter than 255");
        }

        if (otaPackageInfo.getVersion().length() > 255) {
            throw new DataValidationException("The length of version should be equal or shorter than 255");
        }
    }

    protected void validateUpdate(OtaPackageInfo otaPackage, OtaPackageInfo otaPackageOld) {
        if (!otaPackageOld.getType().equals(otaPackage.getType())) {
            throw new DataValidationException("Updating type is prohibited!");
        }

        if (!otaPackageOld.getTitle().equals(otaPackage.getTitle())) {
            throw new DataValidationException("Updating otaPackage title is prohibited!");
        }

        if (!otaPackageOld.getVersion().equals(otaPackage.getVersion())) {
            throw new DataValidationException("Updating otaPackage version is prohibited!");
        }

        if (!Objects.equals(otaPackage.getTag(), otaPackageOld.getTag())) {
            throw new DataValidationException("Updating otaPackage tag is prohibited!");
        }

        if (!otaPackageOld.getDeviceProfileId().equals(otaPackage.getDeviceProfileId())) {
            throw new DataValidationException("Updating otaPackage deviceProfile is prohibited!");
        }

        if (otaPackageOld.getFileName() != null && !otaPackageOld.getFileName().equals(otaPackage.getFileName())) {
            throw new DataValidationException("Updating otaPackage file name is prohibited!");
        }

        if (otaPackageOld.getContentType() != null && !otaPackageOld.getContentType().equals(otaPackage.getContentType())) {
            throw new DataValidationException("Updating otaPackage content type is prohibited!");
        }

        if (otaPackageOld.getChecksumAlgorithm() != null && !otaPackageOld.getChecksumAlgorithm().equals(otaPackage.getChecksumAlgorithm())) {
            throw new DataValidationException("Updating otaPackage content type is prohibited!");
        }

        if (otaPackageOld.getChecksum() != null && !otaPackageOld.getChecksum().equals(otaPackage.getChecksum())) {
            throw new DataValidationException("Updating otaPackage content type is prohibited!");
        }

        if (otaPackageOld.getDataSize() != null && !otaPackageOld.getDataSize().equals(otaPackage.getDataSize())) {
            throw new DataValidationException("Updating otaPackage data size is prohibited!");
        }

        if (otaPackageOld.getUrl() != null && !otaPackageOld.getUrl().equals(otaPackage.getUrl())) {
            throw new DataValidationException("Updating otaPackage URL is prohibited!");
        }
    }

}

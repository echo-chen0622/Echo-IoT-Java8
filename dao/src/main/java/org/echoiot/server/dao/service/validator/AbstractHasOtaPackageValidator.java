package org.echoiot.server.dao.service.validator;

import org.echoiot.server.common.data.BaseData;
import org.echoiot.server.common.data.HasOtaPackage;
import org.echoiot.server.common.data.OtaPackage;
import org.echoiot.server.common.data.id.DeviceProfileId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.ota.OtaPackageType;
import org.echoiot.server.dao.exception.DataValidationException;
import org.echoiot.server.dao.ota.OtaPackageService;
import org.echoiot.server.dao.service.DataValidator;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

public abstract class AbstractHasOtaPackageValidator<D extends BaseData<?>> extends DataValidator<D> {

    @Resource
    @Lazy
    private OtaPackageService otaPackageService;

    protected <T extends HasOtaPackage> void validateOtaPackage(TenantId tenantId, @NotNull T entity, DeviceProfileId deviceProfileId) {
        if (entity.getFirmwareId() != null) {
            OtaPackage firmware = otaPackageService.findOtaPackageById(tenantId, entity.getFirmwareId());
            validateOtaPackage(tenantId, OtaPackageType.FIRMWARE, deviceProfileId, firmware);
        }
        if (entity.getSoftwareId() != null) {
            OtaPackage software = otaPackageService.findOtaPackageById(tenantId, entity.getSoftwareId());
            validateOtaPackage(tenantId, OtaPackageType.SOFTWARE, deviceProfileId, software);
        }
    }

    private void validateOtaPackage(TenantId tenantId, @NotNull OtaPackageType type, DeviceProfileId deviceProfileId, @NotNull OtaPackage otaPackage) {
        if (otaPackage == null) {
            throw new DataValidationException(prepareMsg("Can't assign non-existent %s!", type));
        }
        if (!otaPackage.getTenantId().equals(tenantId)) {
            throw new DataValidationException(prepareMsg("Can't assign %s from different tenant!", type));
        }
        if (!otaPackage.getType().equals(type)) {
            throw new DataValidationException(prepareMsg("Can't assign %s with type: " + otaPackage.getType(), type));
        }
        if (otaPackage.getData() == null && !otaPackage.hasUrl()) {
            throw new DataValidationException(prepareMsg("Can't assign %s with empty data!", type));
        }
        if (!otaPackage.getDeviceProfileId().equals(deviceProfileId)) {
            throw new DataValidationException(prepareMsg("Can't assign %s with different deviceProfile!", type));
        }
    }

    private String prepareMsg(@NotNull String msg, @NotNull OtaPackageType type) {
        return String.format(msg, type.name().toLowerCase());
    }
}

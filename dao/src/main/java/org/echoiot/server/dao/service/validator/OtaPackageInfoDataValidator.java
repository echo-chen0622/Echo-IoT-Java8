package org.echoiot.server.dao.service.validator;

import org.echoiot.server.common.data.OtaPackageInfo;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.dao.ota.OtaPackageInfoDao;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class OtaPackageInfoDataValidator extends BaseOtaPackageDataValidator<OtaPackageInfo> {

    @Resource
    private OtaPackageInfoDao otaPackageInfoDao;

    @Override
    protected void validateDataImpl(TenantId tenantId, @NotNull OtaPackageInfo otaPackageInfo) {
        validateImpl(otaPackageInfo);
    }

    @NotNull
    @Override
    protected OtaPackageInfo validateUpdate(TenantId tenantId, @NotNull OtaPackageInfo otaPackage) {
        OtaPackageInfo otaPackageOld = otaPackageInfoDao.findById(tenantId, otaPackage.getUuidId());
        validateUpdate(otaPackage, otaPackageOld);
        return otaPackageOld;
    }
}

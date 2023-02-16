package org.echoiot.server.dao.service.validator;

import org.echoiot.server.common.data.OtaPackageInfo;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.dao.ota.OtaPackageInfoDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OtaPackageInfoDataValidator extends BaseOtaPackageDataValidator<OtaPackageInfo> {

    @Autowired
    private OtaPackageInfoDao otaPackageInfoDao;

    @Override
    protected void validateDataImpl(TenantId tenantId, OtaPackageInfo otaPackageInfo) {
        validateImpl(otaPackageInfo);
    }

    @Override
    protected OtaPackageInfo validateUpdate(TenantId tenantId, OtaPackageInfo otaPackage) {
        OtaPackageInfo otaPackageOld = otaPackageInfoDao.findById(tenantId, otaPackage.getUuidId());
        validateUpdate(otaPackage, otaPackageOld);
        return otaPackageOld;
    }
}

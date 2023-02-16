package org.echoiot.server.dao.ota;

import org.echoiot.server.common.data.OtaPackage;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.dao.Dao;
import org.echoiot.server.dao.TenantEntityWithDataDao;

public interface OtaPackageDao extends Dao<OtaPackage>, TenantEntityWithDataDao {
    Long sumDataSizeByTenantId(TenantId tenantId);
}

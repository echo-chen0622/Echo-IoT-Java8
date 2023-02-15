package org.thingsboard.server.dao.ota;

import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.TenantEntityWithDataDao;

public interface OtaPackageDao extends Dao<OtaPackage>, TenantEntityWithDataDao {
    Long sumDataSizeByTenantId(TenantId tenantId);
}

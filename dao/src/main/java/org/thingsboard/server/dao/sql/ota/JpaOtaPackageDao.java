package org.thingsboard.server.dao.sql.ota;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.sql.OtaPackageEntity;
import org.thingsboard.server.dao.ota.OtaPackageDao;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTextDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.UUID;

@Slf4j
@Component
@SqlDao
public class JpaOtaPackageDao extends JpaAbstractSearchTextDao<OtaPackageEntity, OtaPackage> implements OtaPackageDao {

    @Autowired
    private OtaPackageRepository otaPackageRepository;

    @Override
    protected Class<OtaPackageEntity> getEntityClass() {
        return OtaPackageEntity.class;
    }

    @Override
    protected JpaRepository<OtaPackageEntity, UUID> getRepository() {
        return otaPackageRepository;
    }

    @Override
    public Long sumDataSizeByTenantId(TenantId tenantId) {
        return otaPackageRepository.sumDataSizeByTenantId(tenantId.getId());
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.OTA_PACKAGE;
    }

}

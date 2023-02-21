package org.echoiot.server.dao.sql.ota;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.OtaPackage;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.dao.model.sql.OtaPackageEntity;
import org.echoiot.server.dao.ota.OtaPackageDao;
import org.echoiot.server.dao.sql.JpaAbstractSearchTextDao;
import org.echoiot.server.dao.util.SqlDao;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.UUID;

@Slf4j
@Component
@SqlDao
public class JpaOtaPackageDao extends JpaAbstractSearchTextDao<OtaPackageEntity, OtaPackage> implements OtaPackageDao {

    @Resource
    private OtaPackageRepository otaPackageRepository;

    @NotNull
    @Override
    protected Class<OtaPackageEntity> getEntityClass() {
        return OtaPackageEntity.class;
    }

    @Override
    protected JpaRepository<OtaPackageEntity, UUID> getRepository() {
        return otaPackageRepository;
    }

    @Override
    public Long sumDataSizeByTenantId(@NotNull TenantId tenantId) {
        return otaPackageRepository.sumDataSizeByTenantId(tenantId.getId());
    }

    @NotNull
    @Override
    public EntityType getEntityType() {
        return EntityType.OTA_PACKAGE;
    }

}

package org.echoiot.server.dao.sql.ota;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.dao.model.sql.OtaPackageInfoEntity;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.echoiot.server.common.data.OtaPackageInfo;
import org.echoiot.server.common.data.id.DeviceProfileId;
import org.echoiot.server.common.data.id.OtaPackageId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.ota.OtaPackageType;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.dao.DaoUtil;
import org.echoiot.server.dao.ota.OtaPackageInfoDao;
import org.echoiot.server.dao.sql.JpaAbstractSearchTextDao;
import org.echoiot.server.dao.util.SqlDao;

import java.util.Objects;
import java.util.UUID;

@Slf4j
@Component
@SqlDao
public class JpaOtaPackageInfoDao extends JpaAbstractSearchTextDao<OtaPackageInfoEntity, OtaPackageInfo> implements OtaPackageInfoDao {

    @Resource
    private OtaPackageInfoRepository otaPackageInfoRepository;

    @NotNull
    @Override
    protected Class<OtaPackageInfoEntity> getEntityClass() {
        return OtaPackageInfoEntity.class;
    }

    @Override
    protected JpaRepository<OtaPackageInfoEntity, UUID> getRepository() {
        return otaPackageInfoRepository;
    }

    @Override
    public OtaPackageInfo findById(TenantId tenantId, UUID id) {
        return DaoUtil.getData(otaPackageInfoRepository.findOtaPackageInfoById(id));
    }

    @Override
    public OtaPackageInfo save(TenantId tenantId, @NotNull OtaPackageInfo otaPackageInfo) {
        OtaPackageInfo savedOtaPackage = super.save(tenantId, otaPackageInfo);
        if (otaPackageInfo.getId() == null) {
            return savedOtaPackage;
        } else {
            return findById(tenantId, savedOtaPackage.getId().getId());
        }
    }

    @NotNull
    @Override
    public PageData<OtaPackageInfo> findOtaPackageInfoByTenantId(@NotNull TenantId tenantId, @NotNull PageLink pageLink) {
        return DaoUtil.toPageData(otaPackageInfoRepository
                .findAllByTenantId(
                        tenantId.getId(),
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @NotNull
    @Override
    public PageData<OtaPackageInfo> findOtaPackageInfoByTenantIdAndDeviceProfileIdAndTypeAndHasData(@NotNull TenantId tenantId, @NotNull DeviceProfileId deviceProfileId, OtaPackageType otaPackageType, @NotNull PageLink pageLink) {
        return DaoUtil.toPageData(otaPackageInfoRepository
                .findAllByTenantIdAndTypeAndDeviceProfileIdAndHasData(
                        tenantId.getId(),
                        deviceProfileId.getId(),
                        otaPackageType,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public boolean isOtaPackageUsed(@NotNull OtaPackageId otaPackageId, @NotNull OtaPackageType otaPackageType, @NotNull DeviceProfileId deviceProfileId) {
        return otaPackageInfoRepository.isOtaPackageUsed(otaPackageId.getId(), deviceProfileId.getId(), otaPackageType.name());
    }
}

package org.echoiot.server.dao.sql.resource;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.TbResourceInfo;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.dao.DaoUtil;
import org.echoiot.server.dao.model.sql.TbResourceInfoEntity;
import org.echoiot.server.dao.resource.TbResourceInfoDao;
import org.echoiot.server.dao.sql.JpaAbstractSearchTextDao;
import org.echoiot.server.dao.util.SqlDao;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Component
@SqlDao
public class JpaTbResourceInfoDao extends JpaAbstractSearchTextDao<TbResourceInfoEntity, TbResourceInfo> implements TbResourceInfoDao {

    @Resource
    private TbResourceInfoRepository resourceInfoRepository;

    @NotNull
    @Override
    protected Class<TbResourceInfoEntity> getEntityClass() {
        return TbResourceInfoEntity.class;
    }

    @Override
    protected JpaRepository<TbResourceInfoEntity, UUID> getRepository() {
        return resourceInfoRepository;
    }

    @NotNull
    @Override
    public PageData<TbResourceInfo> findAllTenantResourcesByTenantId(UUID tenantId, @NotNull PageLink pageLink) {
        return DaoUtil.toPageData(resourceInfoRepository
                .findAllTenantResourcesByTenantId(
                        tenantId,
                        TenantId.NULL_UUID,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @NotNull
    @Override
    public PageData<TbResourceInfo> findTenantResourcesByTenantId(UUID tenantId, @NotNull PageLink pageLink) {
        return DaoUtil.toPageData(resourceInfoRepository
                .findTenantResourcesByTenantId(
                        tenantId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }
}

package org.thingsboard.server.dao.sql.resource;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.TbResourceInfoEntity;
import org.thingsboard.server.dao.resource.TbResourceInfoDao;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTextDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.Objects;
import java.util.UUID;

@Slf4j
@Component
@SqlDao
public class JpaTbResourceInfoDao extends JpaAbstractSearchTextDao<TbResourceInfoEntity, TbResourceInfo> implements TbResourceInfoDao {

    @Autowired
    private TbResourceInfoRepository resourceInfoRepository;

    @Override
    protected Class<TbResourceInfoEntity> getEntityClass() {
        return TbResourceInfoEntity.class;
    }

    @Override
    protected JpaRepository<TbResourceInfoEntity, UUID> getRepository() {
        return resourceInfoRepository;
    }

    @Override
    public PageData<TbResourceInfo> findAllTenantResourcesByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(resourceInfoRepository
                .findAllTenantResourcesByTenantId(
                        tenantId,
                        TenantId.NULL_UUID,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<TbResourceInfo> findTenantResourcesByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(resourceInfoRepository
                .findTenantResourcesByTenantId(
                        tenantId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }
}

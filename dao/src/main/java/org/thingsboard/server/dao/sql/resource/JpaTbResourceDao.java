package org.thingsboard.server.dao.sql.resource;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.TbResourceEntity;
import org.thingsboard.server.dao.resource.TbResourceDao;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTextDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Component
@SqlDao
public class JpaTbResourceDao extends JpaAbstractSearchTextDao<TbResourceEntity, TbResource> implements TbResourceDao {

    private final TbResourceRepository resourceRepository;

    public JpaTbResourceDao(TbResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    @Override
    protected Class<TbResourceEntity> getEntityClass() {
        return TbResourceEntity.class;
    }

    @Override
    protected JpaRepository<TbResourceEntity, UUID> getRepository() {
        return resourceRepository;
    }

    @Override
    public TbResource getResource(TenantId tenantId, ResourceType resourceType, String resourceKey) {

        return DaoUtil.getData(resourceRepository.findByTenantIdAndResourceTypeAndResourceKey(tenantId.getId(), resourceType.name(), resourceKey));
    }

    @Override
    public PageData<TbResource> findAllByTenantId(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(resourceRepository.findAllByTenantId(tenantId.getId(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<TbResource> findResourcesByTenantIdAndResourceType(TenantId tenantId,
                                                                       ResourceType resourceType,
                                                                       PageLink pageLink) {
        return DaoUtil.toPageData(resourceRepository.findResourcesPage(
                tenantId.getId(),
                TenantId.SYS_TENANT_ID.getId(),
                resourceType.name(),
                Objects.toString(pageLink.getTextSearch(), ""),
                DaoUtil.toPageable(pageLink)
        ));
    }

    @Override
    public List<TbResource> findResourcesByTenantIdAndResourceType(TenantId tenantId, ResourceType resourceType,
                                                                   String[] objectIds,
                                                                   String searchText) {
        return objectIds == null ?
                DaoUtil.convertDataList(resourceRepository.findResources(
                        tenantId.getId(),
                        TenantId.SYS_TENANT_ID.getId(),
                        resourceType.name(),
                        Objects.toString(searchText, ""))) :
                DaoUtil.convertDataList(resourceRepository.findResourcesByIds(
                        tenantId.getId(),
                        TenantId.SYS_TENANT_ID.getId(),
                        resourceType.name(), objectIds));
    }

    @Override
    public Long sumDataSizeByTenantId(TenantId tenantId) {
        return resourceRepository.sumDataSizeByTenantId(tenantId.getId());
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.TB_RESOURCE;
    }

}

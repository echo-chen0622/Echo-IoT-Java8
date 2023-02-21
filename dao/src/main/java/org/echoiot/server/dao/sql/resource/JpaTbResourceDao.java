package org.echoiot.server.dao.sql.resource;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.ResourceType;
import org.echoiot.server.common.data.TbResource;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.dao.DaoUtil;
import org.echoiot.server.dao.model.sql.TbResourceEntity;
import org.echoiot.server.dao.resource.TbResourceDao;
import org.echoiot.server.dao.sql.JpaAbstractSearchTextDao;
import org.echoiot.server.dao.util.SqlDao;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

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

    @NotNull
    @Override
    protected Class<TbResourceEntity> getEntityClass() {
        return TbResourceEntity.class;
    }

    @Override
    protected JpaRepository<TbResourceEntity, UUID> getRepository() {
        return resourceRepository;
    }

    @Override
    public TbResource getResource(@NotNull TenantId tenantId, @NotNull ResourceType resourceType, String resourceKey) {

        return DaoUtil.getData(resourceRepository.findByTenantIdAndResourceTypeAndResourceKey(tenantId.getId(), resourceType.name(), resourceKey));
    }

    @NotNull
    @Override
    public PageData<TbResource> findAllByTenantId(@NotNull TenantId tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(resourceRepository.findAllByTenantId(tenantId.getId(), DaoUtil.toPageable(pageLink)));
    }

    @NotNull
    @Override
    public PageData<TbResource> findResourcesByTenantIdAndResourceType(@NotNull TenantId tenantId,
                                                                       @NotNull ResourceType resourceType,
                                                                       @NotNull PageLink pageLink) {
        return DaoUtil.toPageData(resourceRepository.findResourcesPage(
                tenantId.getId(),
                TenantId.SYS_TENANT_ID.getId(),
                resourceType.name(),
                Objects.toString(pageLink.getTextSearch(), ""),
                DaoUtil.toPageable(pageLink)
        ));
    }

    @Override
    public List<TbResource> findResourcesByTenantIdAndResourceType(@NotNull TenantId tenantId, @NotNull ResourceType resourceType,
                                                                   @Nullable String[] objectIds,
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
    public Long sumDataSizeByTenantId(@NotNull TenantId tenantId) {
        return resourceRepository.sumDataSizeByTenantId(tenantId.getId());
    }

    @NotNull
    @Override
    public EntityType getEntityType() {
        return EntityType.TB_RESOURCE;
    }

}

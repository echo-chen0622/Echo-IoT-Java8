package org.echoiot.server.dao.sql.entityview;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.EntitySubtype;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.EntityView;
import org.echoiot.server.common.data.EntityViewInfo;
import org.echoiot.server.common.data.id.EntityViewId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.dao.DaoUtil;
import org.echoiot.server.dao.entityview.EntityViewDao;
import org.echoiot.server.dao.model.sql.EntityViewEntity;
import org.echoiot.server.dao.model.sql.EntityViewInfoEntity;
import org.echoiot.server.dao.sql.JpaAbstractSearchTextDao;
import org.echoiot.server.dao.util.SqlDao;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

/**
 * Created by Victor Basanets on 8/31/2017.
 */
@Component
@Slf4j
@SqlDao
public class JpaEntityViewDao extends JpaAbstractSearchTextDao<EntityViewEntity, EntityView>
        implements EntityViewDao {

    @Resource
    private EntityViewRepository entityViewRepository;

    @NotNull
    @Override
    protected Class<EntityViewEntity> getEntityClass() {
        return EntityViewEntity.class;
    }

    @Override
    protected JpaRepository<EntityViewEntity, UUID> getRepository() {
        return entityViewRepository;
    }

    @Override
    public EntityViewInfo findEntityViewInfoById(TenantId tenantId, UUID entityViewId) {
        return DaoUtil.getData(entityViewRepository.findEntityViewInfoById(entityViewId));
    }

    @NotNull
    @Override
    public PageData<EntityView> findEntityViewsByTenantId(UUID tenantId, @NotNull PageLink pageLink) {
        return DaoUtil.toPageData(
                entityViewRepository.findByTenantId(
                        tenantId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @NotNull
    @Override
    public PageData<EntityViewInfo> findEntityViewInfosByTenantId(UUID tenantId, @NotNull PageLink pageLink) {
        return DaoUtil.toPageData(
                entityViewRepository.findEntityViewInfosByTenantId(
                        tenantId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink, EntityViewInfoEntity.entityViewInfoColumnMap)));
    }

    @NotNull
    @Override
    public PageData<EntityView> findEntityViewsByTenantIdAndType(UUID tenantId, String type, @NotNull PageLink pageLink) {
        return DaoUtil.toPageData(
                entityViewRepository.findByTenantIdAndType(
                        tenantId,
                        type,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @NotNull
    @Override
    public PageData<EntityViewInfo> findEntityViewInfosByTenantIdAndType(UUID tenantId, String type, @NotNull PageLink pageLink) {
        return DaoUtil.toPageData(
                entityViewRepository.findEntityViewInfosByTenantIdAndType(
                        tenantId,
                        type,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink, EntityViewInfoEntity.entityViewInfoColumnMap)));
    }

    @NotNull
    @Override
    public Optional<EntityView> findEntityViewByTenantIdAndName(UUID tenantId, String name) {
        return Optional.ofNullable(
                DaoUtil.getData(entityViewRepository.findByTenantIdAndName(tenantId, name)));
    }

    @NotNull
    @Override
    public PageData<EntityView> findEntityViewsByTenantIdAndCustomerId(UUID tenantId,
                                                                       UUID customerId,
                                                                       @NotNull PageLink pageLink) {
        return DaoUtil.toPageData(
                entityViewRepository.findByTenantIdAndCustomerId(
                        tenantId,
                        customerId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)
                ));
    }

    @NotNull
    @Override
    public PageData<EntityViewInfo> findEntityViewInfosByTenantIdAndCustomerId(UUID tenantId, UUID customerId, @NotNull PageLink pageLink) {
        return DaoUtil.toPageData(
                entityViewRepository.findEntityViewInfosByTenantIdAndCustomerId(
                        tenantId,
                        customerId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink, EntityViewInfoEntity.entityViewInfoColumnMap)));
    }

    @NotNull
    @Override
    public PageData<EntityView> findEntityViewsByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type, @NotNull PageLink pageLink) {
        return DaoUtil.toPageData(
                entityViewRepository.findByTenantIdAndCustomerIdAndType(
                        tenantId,
                        customerId,
                        type,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)
                ));
    }

    @NotNull
    @Override
    public PageData<EntityViewInfo> findEntityViewInfosByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type, @NotNull PageLink pageLink) {
        return DaoUtil.toPageData(
                entityViewRepository.findEntityViewInfosByTenantIdAndCustomerIdAndType(
                        tenantId,
                        customerId,
                        type,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink, EntityViewInfoEntity.entityViewInfoColumnMap)));
    }

    @Override
    public List<EntityView> findEntityViewsByTenantIdAndEntityId(UUID tenantId, UUID entityId) {
        return DaoUtil.convertDataList(
                entityViewRepository.findAllByTenantIdAndEntityId(tenantId, entityId));
    }

    @Override
    public ListenableFuture<List<EntitySubtype>> findTenantEntityViewTypesAsync(UUID tenantId) {
        return service.submit(() -> convertTenantEntityViewTypesToDto(tenantId, entityViewRepository.findTenantEntityViewTypes(tenantId)));
    }

    @NotNull
    private List<EntitySubtype> convertTenantEntityViewTypesToDto(UUID tenantId, @Nullable List<String> types) {
        @NotNull List<EntitySubtype> list = Collections.emptyList();
        if (types != null && !types.isEmpty()) {
            list = new ArrayList<>();
            for (String type : types) {
                list.add(new EntitySubtype(TenantId.fromUUID(tenantId), EntityType.ENTITY_VIEW, type));
            }
        }
        return list;
    }

    @NotNull
    @Override
    public PageData<EntityView> findEntityViewsByTenantIdAndEdgeId(UUID tenantId, UUID edgeId, @NotNull PageLink pageLink) {
        log.debug("Try to find entity views by tenantId [{}], edgeId [{}] and pageLink [{}]", tenantId, edgeId, pageLink);
        return DaoUtil.toPageData(entityViewRepository
                .findByTenantIdAndEdgeId(
                        tenantId,
                        edgeId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @NotNull
    @Override
    public PageData<EntityView> findEntityViewsByTenantIdAndEdgeIdAndType(UUID tenantId, UUID edgeId, String type, @NotNull PageLink pageLink) {
        log.debug("Try to find entity views by tenantId [{}], edgeId [{}], type [{}] and pageLink [{}]", tenantId, edgeId, type, pageLink);
        return DaoUtil.toPageData(entityViewRepository
                .findByTenantIdAndEdgeIdAndType(
                        tenantId,
                        edgeId,
                        type,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public EntityView findByTenantIdAndExternalId(UUID tenantId, UUID externalId) {
        return DaoUtil.getData(entityViewRepository.findByTenantIdAndExternalId(tenantId, externalId));
    }

    @Override
    public PageData<EntityView> findByTenantId(UUID tenantId, @NotNull PageLink pageLink) {
        return findEntityViewsByTenantId(tenantId, pageLink);
    }

    @Nullable
    @Override
    public EntityViewId getExternalIdByInternal(@NotNull EntityViewId internalId) {
        return Optional.ofNullable(entityViewRepository.getExternalIdById(internalId.getId()))
                .map(EntityViewId::new).orElse(null);
    }

    @Nullable
    @Override
    public EntityView findByTenantIdAndName(UUID tenantId, String name) {
        return findEntityViewByTenantIdAndName(tenantId, name).orElse(null);
    }

    @NotNull
    @Override
    public EntityType getEntityType() {
        return EntityType.ENTITY_VIEW;
    }
}

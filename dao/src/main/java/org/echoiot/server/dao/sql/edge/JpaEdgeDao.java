package org.echoiot.server.dao.sql.edge;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.EntitySubtype;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.edge.Edge;
import org.echoiot.server.common.data.edge.EdgeInfo;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.dao.DaoUtil;
import org.echoiot.server.dao.edge.EdgeDao;
import org.echoiot.server.dao.model.sql.EdgeEntity;
import org.echoiot.server.dao.model.sql.EdgeInfoEntity;
import org.echoiot.server.dao.sql.JpaAbstractSearchTextDao;
import org.echoiot.server.dao.util.SqlDao;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

@Component
@Slf4j
@SqlDao
public class JpaEdgeDao extends JpaAbstractSearchTextDao<EdgeEntity, Edge> implements EdgeDao {

    @Resource
    private EdgeRepository edgeRepository;

    @NotNull
    @Override
    protected Class<EdgeEntity> getEntityClass() {
        return EdgeEntity.class;
    }

    @Override
    protected JpaRepository<EdgeEntity, UUID> getRepository() {
        return edgeRepository;
    }

    @Override
    public EdgeInfo findEdgeInfoById(TenantId tenantId, UUID edgeId) {
        return DaoUtil.getData(edgeRepository.findEdgeInfoById(edgeId));
    }

    @NotNull
    @Override
    public PageData<Edge> findEdgesByTenantId(UUID tenantId, @NotNull PageLink pageLink) {
        return DaoUtil.toPageData(
                edgeRepository.findByTenantId(
                        tenantId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public ListenableFuture<List<Edge>> findEdgesByTenantIdAndIdsAsync(UUID tenantId, List<UUID> edgeIds) {
        return service.submit(() -> DaoUtil.convertDataList(edgeRepository.findEdgesByTenantIdAndIdIn(tenantId, edgeIds)));
    }

    @NotNull
    @Override
    public PageData<Edge> findEdgesByTenantIdAndCustomerId(UUID tenantId, UUID customerId, @NotNull PageLink pageLink) {
        return DaoUtil.toPageData(
                edgeRepository.findByTenantIdAndCustomerId(
                        tenantId,
                        customerId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public ListenableFuture<List<Edge>> findEdgesByTenantIdCustomerIdAndIdsAsync(UUID tenantId, UUID customerId, List<UUID> edgeIds) {
        return service.submit(() -> DaoUtil.convertDataList(
                edgeRepository.findEdgesByTenantIdAndCustomerIdAndIdIn(tenantId, customerId, edgeIds)));
    }

    @NotNull
    @Override
    public Optional<Edge> findEdgeByTenantIdAndName(UUID tenantId, String name) {
        Edge edge = DaoUtil.getData(edgeRepository.findByTenantIdAndName(tenantId, name));
        return Optional.ofNullable(edge);
    }

    @NotNull
    @Override
    public PageData<Edge> findEdgesByTenantIdAndType(UUID tenantId, String type, @NotNull PageLink pageLink) {
        return DaoUtil.toPageData(
                edgeRepository.findByTenantIdAndType(
                        tenantId,
                        type,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @NotNull
    @Override
    public PageData<Edge> findEdgesByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type, @NotNull PageLink pageLink) {
        return DaoUtil.toPageData(
                edgeRepository.findByTenantIdAndCustomerIdAndType(
                        tenantId,
                        customerId,
                        type,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @NotNull
    @Override
    public PageData<EdgeInfo> findEdgeInfosByTenantIdAndCustomerId(UUID tenantId, UUID customerId, @NotNull PageLink pageLink) {
        return DaoUtil.toPageData(
                edgeRepository.findEdgeInfosByTenantIdAndCustomerId(
                        tenantId,
                        customerId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink, EdgeInfoEntity.edgeInfoColumnMap)));
    }

    @NotNull
    @Override
    public PageData<EdgeInfo> findEdgeInfosByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type, @NotNull PageLink pageLink) {
        return DaoUtil.toPageData(
                edgeRepository.findEdgeInfosByTenantIdAndCustomerIdAndType(
                        tenantId,
                        customerId,
                        type,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink, EdgeInfoEntity.edgeInfoColumnMap)));
    }

    @Override
    public ListenableFuture<List<EntitySubtype>> findTenantEdgeTypesAsync(UUID tenantId) {
        return service.submit(() -> convertTenantEdgeTypesToDto(tenantId, edgeRepository.findTenantEdgeTypes(tenantId)));
    }

    @NotNull
    @Override
    public PageData<EdgeInfo> findEdgeInfosByTenantIdAndType(UUID tenantId, String type, @NotNull PageLink pageLink) {
        return DaoUtil.toPageData(
                edgeRepository.findEdgeInfosByTenantIdAndType(
                        tenantId,
                        type,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink, EdgeInfoEntity.edgeInfoColumnMap)));
    }

    @NotNull
    @Override
    public PageData<EdgeInfo> findEdgeInfosByTenantId(UUID tenantId, @NotNull PageLink pageLink) {
        return DaoUtil.toPageData(
                edgeRepository.findEdgeInfosByTenantId(
                        tenantId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink, EdgeInfoEntity.edgeInfoColumnMap)));
    }

    @NotNull
    @Override
    public Optional<Edge> findByRoutingKey(UUID tenantId, String routingKey) {
        Edge edge = DaoUtil.getData(edgeRepository.findByRoutingKey(routingKey));
        return Optional.ofNullable(edge);
    }

    @NotNull
    @Override
    public PageData<Edge> findEdgesByTenantIdAndEntityId(UUID tenantId, UUID entityId, @NotNull EntityType entityType, @NotNull PageLink pageLink) {
        log.debug("Try to find edges by tenantId [{}], entityId [{}], entityType [{}], pageLink [{}]", tenantId, entityId, entityType, pageLink);
        return DaoUtil.toPageData(
                edgeRepository.findByTenantIdAndEntityId(
                        tenantId,
                        entityId,
                        entityType.name(),
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @NotNull
    private List<EntitySubtype> convertTenantEdgeTypesToDto(UUID tenantId, @Nullable List<String> types) {
        @NotNull List<EntitySubtype> list = Collections.emptyList();
        if (types != null && !types.isEmpty()) {
            list = new ArrayList<>();
            for (String type : types) {
                list.add(new EntitySubtype(TenantId.fromUUID(tenantId), EntityType.EDGE, type));
            }
        }
        return list;
    }

    @NotNull
    @Override
    public EntityType getEntityType() {
        return EntityType.EDGE;
    }

}

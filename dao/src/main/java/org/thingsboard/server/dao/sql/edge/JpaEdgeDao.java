package org.thingsboard.server.dao.sql.edge;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeInfo;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.edge.EdgeDao;
import org.thingsboard.server.dao.model.sql.EdgeEntity;
import org.thingsboard.server.dao.model.sql.EdgeInfoEntity;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTextDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
@SqlDao
public class JpaEdgeDao extends JpaAbstractSearchTextDao<EdgeEntity, Edge> implements EdgeDao {

    @Autowired
    private EdgeRepository edgeRepository;

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

    @Override
    public PageData<Edge> findEdgesByTenantId(UUID tenantId, PageLink pageLink) {
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

    @Override
    public PageData<Edge> findEdgesByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink) {
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

    @Override
    public Optional<Edge> findEdgeByTenantIdAndName(UUID tenantId, String name) {
        Edge edge = DaoUtil.getData(edgeRepository.findByTenantIdAndName(tenantId, name));
        return Optional.ofNullable(edge);
    }

    @Override
    public PageData<Edge> findEdgesByTenantIdAndType(UUID tenantId, String type, PageLink pageLink) {
        return DaoUtil.toPageData(
                edgeRepository.findByTenantIdAndType(
                        tenantId,
                        type,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<Edge> findEdgesByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type, PageLink pageLink) {
        return DaoUtil.toPageData(
                edgeRepository.findByTenantIdAndCustomerIdAndType(
                        tenantId,
                        customerId,
                        type,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<EdgeInfo> findEdgeInfosByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink) {
        return DaoUtil.toPageData(
                edgeRepository.findEdgeInfosByTenantIdAndCustomerId(
                        tenantId,
                        customerId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink, EdgeInfoEntity.edgeInfoColumnMap)));
    }

    @Override
    public PageData<EdgeInfo> findEdgeInfosByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type, PageLink pageLink) {
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

    @Override
    public PageData<EdgeInfo> findEdgeInfosByTenantIdAndType(UUID tenantId, String type, PageLink pageLink) {
        return DaoUtil.toPageData(
                edgeRepository.findEdgeInfosByTenantIdAndType(
                        tenantId,
                        type,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink, EdgeInfoEntity.edgeInfoColumnMap)));
    }

    @Override
    public PageData<EdgeInfo> findEdgeInfosByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(
                edgeRepository.findEdgeInfosByTenantId(
                        tenantId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink, EdgeInfoEntity.edgeInfoColumnMap)));
    }

    @Override
    public Optional<Edge> findByRoutingKey(UUID tenantId, String routingKey) {
        Edge edge = DaoUtil.getData(edgeRepository.findByRoutingKey(routingKey));
        return Optional.ofNullable(edge);
    }

    @Override
    public PageData<Edge> findEdgesByTenantIdAndEntityId(UUID tenantId, UUID entityId, EntityType entityType, PageLink pageLink) {
        log.debug("Try to find edges by tenantId [{}], entityId [{}], entityType [{}], pageLink [{}]", tenantId, entityId, entityType, pageLink);
        return DaoUtil.toPageData(
                edgeRepository.findByTenantIdAndEntityId(
                        tenantId,
                        entityId,
                        entityType.name(),
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    private List<EntitySubtype> convertTenantEdgeTypesToDto(UUID tenantId, List<String> types) {
        List<EntitySubtype> list = Collections.emptyList();
        if (types != null && !types.isEmpty()) {
            list = new ArrayList<>();
            for (String type : types) {
                list.add(new EntitySubtype(TenantId.fromUUID(tenantId), EntityType.EDGE, type));
            }
        }
        return list;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.EDGE;
    }

}

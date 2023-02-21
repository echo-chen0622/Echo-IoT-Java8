package org.echoiot.server.dao.sql.widget;

import org.echoiot.server.dao.model.sql.WidgetsBundleEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.id.WidgetsBundleId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.data.widget.WidgetsBundle;
import org.echoiot.server.dao.DaoUtil;
import org.echoiot.server.dao.sql.JpaAbstractSearchTextDao;
import org.echoiot.server.dao.util.SqlDao;
import org.echoiot.server.dao.widget.WidgetsBundleDao;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.echoiot.server.dao.model.ModelConstants.NULL_UUID;

/**
 * Created by Valerii Sosliuk on 4/23/2017.
 */
@Component
@SqlDao
public class JpaWidgetsBundleDao extends JpaAbstractSearchTextDao<WidgetsBundleEntity, WidgetsBundle> implements WidgetsBundleDao {

    @Resource
    private WidgetsBundleRepository widgetsBundleRepository;

    @NotNull
    @Override
    protected Class<WidgetsBundleEntity> getEntityClass() {
        return WidgetsBundleEntity.class;
    }

    @Override
    protected JpaRepository<WidgetsBundleEntity, UUID> getRepository() {
        return widgetsBundleRepository;
    }

    @Override
    public WidgetsBundle findWidgetsBundleByTenantIdAndAlias(UUID tenantId, String alias) {
        return DaoUtil.getData(widgetsBundleRepository.findWidgetsBundleByTenantIdAndAlias(tenantId, alias));
    }

    @NotNull
    @Override
    public PageData<WidgetsBundle> findSystemWidgetsBundles(TenantId tenantId, @NotNull PageLink pageLink) {
        return DaoUtil.toPageData(
                widgetsBundleRepository
                        .findSystemWidgetsBundles(
                                NULL_UUID,
                                Objects.toString(pageLink.getTextSearch(), ""),
                                DaoUtil.toPageable(pageLink)));
    }

    @NotNull
    @Override
    public PageData<WidgetsBundle> findTenantWidgetsBundlesByTenantId(UUID tenantId, @NotNull PageLink pageLink) {
        return DaoUtil.toPageData(
                widgetsBundleRepository
                        .findTenantWidgetsBundlesByTenantId(
                                tenantId,
                                Objects.toString(pageLink.getTextSearch(), ""),
                                DaoUtil.toPageable(pageLink)));
    }

    @NotNull
    @Override
    public PageData<WidgetsBundle> findAllTenantWidgetsBundlesByTenantId(UUID tenantId, @NotNull PageLink pageLink) {
        return DaoUtil.toPageData(
                widgetsBundleRepository
                        .findAllTenantWidgetsBundlesByTenantId(
                                tenantId,
                                NULL_UUID,
                                Objects.toString(pageLink.getTextSearch(), ""),
                                DaoUtil.toPageable(pageLink)));
    }

    @Override
    public WidgetsBundle findByTenantIdAndExternalId(UUID tenantId, UUID externalId) {
        return DaoUtil.getData(widgetsBundleRepository.findByTenantIdAndExternalId(tenantId, externalId));
    }

    @Override
    public WidgetsBundle findByTenantIdAndName(UUID tenantId, String name) {
        return DaoUtil.getData(widgetsBundleRepository.findFirstByTenantIdAndTitle(tenantId, name));
    }

    @Override
    public PageData<WidgetsBundle> findByTenantId(UUID tenantId, @NotNull PageLink pageLink) {
        return findTenantWidgetsBundlesByTenantId(tenantId, pageLink);
    }

    @Nullable
    @Override
    public WidgetsBundleId getExternalIdByInternal(@NotNull WidgetsBundleId internalId) {
        return Optional.ofNullable(widgetsBundleRepository.getExternalIdById(internalId.getId()))
                .map(WidgetsBundleId::new).orElse(null);
    }

    @NotNull
    @Override
    public EntityType getEntityType() {
        return EntityType.WIDGETS_BUNDLE;
    }

}

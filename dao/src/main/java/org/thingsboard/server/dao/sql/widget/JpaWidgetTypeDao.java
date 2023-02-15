package org.thingsboard.server.dao.sql.widget;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.common.data.widget.WidgetTypeInfo;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.WidgetTypeDetailsEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;
import org.thingsboard.server.dao.widget.WidgetTypeDao;

import java.util.List;
import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 4/29/2017.
 */
@Component
@SqlDao
public class JpaWidgetTypeDao extends JpaAbstractDao<WidgetTypeDetailsEntity, WidgetTypeDetails> implements WidgetTypeDao {

    @Autowired
    private WidgetTypeRepository widgetTypeRepository;

    @Override
    protected Class<WidgetTypeDetailsEntity> getEntityClass() {
        return WidgetTypeDetailsEntity.class;
    }

    @Override
    protected JpaRepository<WidgetTypeDetailsEntity, UUID> getRepository() {
        return widgetTypeRepository;
    }

    @Override
    public WidgetType findWidgetTypeById(TenantId tenantId, UUID widgetTypeId) {
        return DaoUtil.getData(widgetTypeRepository.findWidgetTypeById(widgetTypeId));
    }

    @Override
    public List<WidgetType> findWidgetTypesByTenantIdAndBundleAlias(UUID tenantId, String bundleAlias) {
        return DaoUtil.convertDataList(widgetTypeRepository.findWidgetTypesByTenantIdAndBundleAlias(tenantId, bundleAlias));
    }

    @Override
    public List<WidgetTypeDetails> findWidgetTypesDetailsByTenantIdAndBundleAlias(UUID tenantId, String bundleAlias) {
        return DaoUtil.convertDataList(widgetTypeRepository.findByTenantIdAndBundleAlias(tenantId, bundleAlias));
    }

    @Override
    public List<WidgetTypeInfo> findWidgetTypesInfosByTenantIdAndBundleAlias(UUID tenantId, String bundleAlias) {
        return DaoUtil.convertDataList(widgetTypeRepository.findWidgetTypesInfosByTenantIdAndBundleAlias(tenantId, bundleAlias));
    }

    @Override
    public WidgetType findByTenantIdBundleAliasAndAlias(UUID tenantId, String bundleAlias, String alias) {
        return DaoUtil.getData(widgetTypeRepository.findWidgetTypeByTenantIdAndBundleAliasAndAlias(tenantId, bundleAlias, alias));
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.WIDGET_TYPE;
    }

}

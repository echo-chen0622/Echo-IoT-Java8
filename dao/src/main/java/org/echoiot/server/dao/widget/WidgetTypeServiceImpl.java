package org.echoiot.server.dao.widget;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.id.WidgetTypeId;
import org.echoiot.server.common.data.widget.WidgetType;
import org.echoiot.server.common.data.widget.WidgetTypeDetails;
import org.echoiot.server.common.data.widget.WidgetTypeInfo;
import org.echoiot.server.dao.service.DataValidator;
import org.echoiot.server.dao.service.Validator;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@Slf4j
public class WidgetTypeServiceImpl implements WidgetTypeService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_BUNDLE_ALIAS = "Incorrect bundleAlias ";
    @Resource
    private WidgetTypeDao widgetTypeDao;

    @Resource
    private DataValidator<WidgetTypeDetails> widgetTypeValidator;

    @Override
    public WidgetType findWidgetTypeById(TenantId tenantId, @NotNull WidgetTypeId widgetTypeId) {
        log.trace("Executing findWidgetTypeById [{}]", widgetTypeId);
        Validator.validateId(widgetTypeId, "Incorrect widgetTypeId " + widgetTypeId);
        return widgetTypeDao.findWidgetTypeById(tenantId, widgetTypeId.getId());
    }

    @Override
    public WidgetTypeDetails findWidgetTypeDetailsById(TenantId tenantId, @NotNull WidgetTypeId widgetTypeId) {
        log.trace("Executing findWidgetTypeDetailsById [{}]", widgetTypeId);
        Validator.validateId(widgetTypeId, "Incorrect widgetTypeId " + widgetTypeId);
        return widgetTypeDao.findById(tenantId, widgetTypeId.getId());
    }

    @Override
    public WidgetTypeDetails saveWidgetType(@NotNull WidgetTypeDetails widgetTypeDetails) {
        log.trace("Executing saveWidgetType [{}]", widgetTypeDetails);
        widgetTypeValidator.validate(widgetTypeDetails, WidgetType::getTenantId);
        return widgetTypeDao.save(widgetTypeDetails.getTenantId(), widgetTypeDetails);
    }

    @Override
    public void deleteWidgetType(TenantId tenantId, @NotNull WidgetTypeId widgetTypeId) {
        log.trace("Executing deleteWidgetType [{}]", widgetTypeId);
        Validator.validateId(widgetTypeId, "Incorrect widgetTypeId " + widgetTypeId);
        widgetTypeDao.removeById(tenantId, widgetTypeId.getId());
    }

    @Override
    public List<WidgetType> findWidgetTypesByTenantIdAndBundleAlias(@NotNull TenantId tenantId, String bundleAlias) {
        log.trace("Executing findWidgetTypesByTenantIdAndBundleAlias, tenantId [{}], bundleAlias [{}]", tenantId, bundleAlias);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validateString(bundleAlias, INCORRECT_BUNDLE_ALIAS + bundleAlias);
        return widgetTypeDao.findWidgetTypesByTenantIdAndBundleAlias(tenantId.getId(), bundleAlias);
    }

    @Override
    public List<WidgetTypeDetails> findWidgetTypesDetailsByTenantIdAndBundleAlias(@NotNull TenantId tenantId, String bundleAlias) {
        log.trace("Executing findWidgetTypesDetailsByTenantIdAndBundleAlias, tenantId [{}], bundleAlias [{}]", tenantId, bundleAlias);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validateString(bundleAlias, INCORRECT_BUNDLE_ALIAS + bundleAlias);
        return widgetTypeDao.findWidgetTypesDetailsByTenantIdAndBundleAlias(tenantId.getId(), bundleAlias);
    }

    @Override
    public List<WidgetTypeInfo> findWidgetTypesInfosByTenantIdAndBundleAlias(@NotNull TenantId tenantId, String bundleAlias) {
        log.trace("Executing findWidgetTypesInfosByTenantIdAndBundleAlias, tenantId [{}], bundleAlias [{}]", tenantId, bundleAlias);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validateString(bundleAlias, INCORRECT_BUNDLE_ALIAS + bundleAlias);
        return widgetTypeDao.findWidgetTypesInfosByTenantIdAndBundleAlias(tenantId.getId(), bundleAlias);
    }

    @Override
    public WidgetType findWidgetTypeByTenantIdBundleAliasAndAlias(@NotNull TenantId tenantId, String bundleAlias, String alias) {
        log.trace("Executing findWidgetTypeByTenantIdBundleAliasAndAlias, tenantId [{}], bundleAlias [{}], alias [{}]", tenantId, bundleAlias, alias);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validateString(bundleAlias, INCORRECT_BUNDLE_ALIAS + bundleAlias);
        Validator.validateString(alias, "Incorrect alias " + alias);
        return widgetTypeDao.findByTenantIdBundleAliasAndAlias(tenantId.getId(), bundleAlias, alias);
    }

    @Override
    public void deleteWidgetTypesByTenantIdAndBundleAlias(@NotNull TenantId tenantId, String bundleAlias) {
        log.trace("Executing deleteWidgetTypesByTenantIdAndBundleAlias, tenantId [{}], bundleAlias [{}]", tenantId, bundleAlias);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validateString(bundleAlias, INCORRECT_BUNDLE_ALIAS + bundleAlias);
        List<WidgetType> widgetTypes = widgetTypeDao.findWidgetTypesByTenantIdAndBundleAlias(tenantId.getId(), bundleAlias);
        for (@NotNull WidgetType widgetType : widgetTypes) {
            deleteWidgetType(tenantId, new WidgetTypeId(widgetType.getUuidId()));
        }
    }

}

package org.echoiot.server.dao.widget;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.dao.exception.IncorrectParameterException;
import org.echoiot.server.dao.service.DataValidator;
import org.echoiot.server.dao.service.Validator;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.id.WidgetsBundleId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.data.widget.WidgetsBundle;
import org.echoiot.server.dao.entity.AbstractCachedEntityService;
import org.echoiot.server.dao.service.PaginatedRemover;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class WidgetsBundleServiceImpl implements WidgetsBundleService {

    private static final int DEFAULT_WIDGETS_BUNDLE_LIMIT = 300;
    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_PAGE_LINK = "Incorrect page link ";

    @Resource
    private WidgetsBundleDao widgetsBundleDao;

    @Resource
    private WidgetTypeService widgetTypeService;

    @Resource
    private DataValidator<WidgetsBundle> widgetsBundleValidator;

    @Override
    public WidgetsBundle findWidgetsBundleById(TenantId tenantId, @NotNull WidgetsBundleId widgetsBundleId) {
        log.trace("Executing findWidgetsBundleById [{}]", widgetsBundleId);
        Validator.validateId(widgetsBundleId, "Incorrect widgetsBundleId " + widgetsBundleId);
        return widgetsBundleDao.findById(tenantId, widgetsBundleId.getId());
    }

    @Override
    public WidgetsBundle saveWidgetsBundle(@NotNull WidgetsBundle widgetsBundle) {
        log.trace("Executing saveWidgetsBundle [{}]", widgetsBundle);
        widgetsBundleValidator.validate(widgetsBundle, WidgetsBundle::getTenantId);
        try {
            return widgetsBundleDao.save(widgetsBundle.getTenantId(), widgetsBundle);
        } catch (Exception e) {
            AbstractCachedEntityService.checkConstraintViolation(e, "widgets_bundle_external_id_unq_key", "Widget Bundle with such external id already exists!");
            throw e;
        }
    }

    @Override
    public void deleteWidgetsBundle(TenantId tenantId, @NotNull WidgetsBundleId widgetsBundleId) {
        log.trace("Executing deleteWidgetsBundle [{}]", widgetsBundleId);
        Validator.validateId(widgetsBundleId, "Incorrect widgetsBundleId " + widgetsBundleId);
        WidgetsBundle widgetsBundle = findWidgetsBundleById(tenantId, widgetsBundleId);
        if (widgetsBundle == null) {
            throw new IncorrectParameterException("Unable to delete non-existent widgets bundle.");
        }
        widgetTypeService.deleteWidgetTypesByTenantIdAndBundleAlias(widgetsBundle.getTenantId(), widgetsBundle.getAlias());
        widgetsBundleDao.removeById(tenantId, widgetsBundleId.getId());
    }

    @Override
    public WidgetsBundle findWidgetsBundleByTenantIdAndAlias(@NotNull TenantId tenantId, String alias) {
        log.trace("Executing findWidgetsBundleByTenantIdAndAlias, tenantId [{}], alias [{}]", tenantId, alias);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validateString(alias, "Incorrect alias " + alias);
        return widgetsBundleDao.findWidgetsBundleByTenantIdAndAlias(tenantId.getId(), alias);
    }

    @Override
    public PageData<WidgetsBundle> findSystemWidgetsBundlesByPageLink(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findSystemWidgetsBundles, pageLink [{}]", pageLink);
        Validator.validatePageLink(pageLink);
        return widgetsBundleDao.findSystemWidgetsBundles(tenantId, pageLink);
    }

    @NotNull
    @Override
    public List<WidgetsBundle> findSystemWidgetsBundles(TenantId tenantId) {
        log.trace("Executing findSystemWidgetsBundles");
        @NotNull List<WidgetsBundle> widgetsBundles = new ArrayList<>();
        PageLink pageLink = new PageLink(DEFAULT_WIDGETS_BUNDLE_LIMIT);
        PageData<WidgetsBundle> pageData;
        do {
            pageData = findSystemWidgetsBundlesByPageLink(tenantId, pageLink);
            widgetsBundles.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());
        return widgetsBundles;
    }

    @Override
    public PageData<WidgetsBundle> findTenantWidgetsBundlesByTenantId(@NotNull TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findTenantWidgetsBundlesByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validatePageLink(pageLink);
        return widgetsBundleDao.findTenantWidgetsBundlesByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public PageData<WidgetsBundle> findAllTenantWidgetsBundlesByTenantIdAndPageLink(@NotNull TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findAllTenantWidgetsBundlesByTenantIdAndPageLink, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validatePageLink(pageLink);
        return widgetsBundleDao.findAllTenantWidgetsBundlesByTenantId(tenantId.getId(), pageLink);
    }

    @NotNull
    @Override
    public List<WidgetsBundle> findAllTenantWidgetsBundlesByTenantId(@NotNull TenantId tenantId) {
        log.trace("Executing findAllTenantWidgetsBundlesByTenantId, tenantId [{}]", tenantId);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        @NotNull List<WidgetsBundle> widgetsBundles = new ArrayList<>();
        PageLink pageLink = new PageLink(DEFAULT_WIDGETS_BUNDLE_LIMIT);
        PageData<WidgetsBundle> pageData;
        do {
            pageData = findAllTenantWidgetsBundlesByTenantIdAndPageLink(tenantId, pageLink);
            widgetsBundles.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());
        return widgetsBundles;
    }

    @Override
    public void deleteWidgetsBundlesByTenantId(TenantId tenantId) {
        log.trace("Executing deleteWidgetsBundlesByTenantId, tenantId [{}]", tenantId);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        tenantWidgetsBundleRemover.removeEntities(tenantId, tenantId);
    }

    private final PaginatedRemover<TenantId, WidgetsBundle> tenantWidgetsBundleRemover =
            new PaginatedRemover<TenantId, WidgetsBundle>() {

                @Override
                protected PageData<WidgetsBundle> findEntities(TenantId tenantId, @NotNull TenantId id, PageLink pageLink) {
                    return widgetsBundleDao.findTenantWidgetsBundlesByTenantId(id.getId(), pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, @NotNull WidgetsBundle entity) {
                    deleteWidgetsBundle(tenantId, new WidgetsBundleId(entity.getUuidId()));
                }
            };

}

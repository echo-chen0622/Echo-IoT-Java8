package org.echoiot.server.dao.entity;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.HasCustomerId;
import org.echoiot.server.common.data.HasName;
import org.echoiot.server.common.data.id.*;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.query.*;
import org.echoiot.server.dao.alarm.AlarmService;
import org.echoiot.server.dao.asset.AssetService;
import org.echoiot.server.dao.customer.CustomerService;
import org.echoiot.server.dao.dashboard.DashboardService;
import org.echoiot.server.dao.device.DeviceService;
import org.echoiot.server.dao.entityview.EntityViewService;
import org.echoiot.server.dao.exception.IncorrectParameterException;
import org.echoiot.server.dao.ota.OtaPackageService;
import org.echoiot.server.dao.resource.ResourceService;
import org.echoiot.server.dao.rule.RuleChainService;
import org.echoiot.server.dao.service.Validator;
import org.echoiot.server.dao.tenant.TenantService;
import org.echoiot.server.dao.user.UserService;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;

import static org.echoiot.server.dao.model.ModelConstants.NULL_UUID;
import static org.echoiot.server.dao.service.Validator.validateId;

/**
 * Created by Echo on 04.05.17.
 */
@Service
@Slf4j
public class BaseEntityService extends AbstractEntityService implements EntityService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_CUSTOMER_ID = "Incorrect customerId ";

    @Resource
    private AssetService assetService;

    @Resource
    private DeviceService deviceService;

    @Resource
    private EntityViewService entityViewService;

    @Resource
    private TenantService tenantService;

    @Resource
    private CustomerService customerService;

    @Resource
    private UserService userService;

    @Resource
    private DashboardService dashboardService;

    @Resource
    private AlarmService alarmService;

    @Resource
    private RuleChainService ruleChainService;

    @Resource
    private EntityQueryDao entityQueryDao;

    @Resource
    private ResourceService resourceService;

    @Resource
    private OtaPackageService otaPackageService;

    @Override
    public long countEntitiesByQuery(TenantId tenantId, CustomerId customerId, EntityCountQuery query) {
        log.trace("Executing countEntitiesByQuery, tenantId [{}], customerId [{}], query [{}]", tenantId, customerId, query);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validateEntityCountQuery(query);
        return this.entityQueryDao.countEntitiesByQuery(tenantId, customerId, query);
    }

    @Override
    public PageData<EntityData> findEntityDataByQuery(TenantId tenantId, CustomerId customerId, EntityDataQuery query) {
        log.trace("Executing findEntityDataByQuery, tenantId [{}], customerId [{}], query [{}]", tenantId, customerId, query);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validateEntityDataQuery(query);
        return this.entityQueryDao.findEntityDataByQuery(tenantId, customerId, query);
    }

    //TODO: 3.1 Remove this from project.
    @Override
    public ListenableFuture<String> fetchEntityNameAsync(TenantId tenantId, EntityId entityId) {
        log.trace("Executing fetchEntityNameAsync [{}]", entityId);
        ListenableFuture<String> entityName;
        ListenableFuture<? extends HasName> hasName;
        switch (entityId.getEntityType()) {
            case ASSET:
                hasName = assetService.findAssetByIdAsync(tenantId, new AssetId(entityId.getId()));
                break;
            case DEVICE:
                hasName = deviceService.findDeviceByIdAsync(tenantId, new DeviceId(entityId.getId()));
                break;
            case ENTITY_VIEW:
                hasName = entityViewService.findEntityViewByIdAsync(tenantId, new EntityViewId(entityId.getId()));
                break;
            case TENANT:
                hasName = tenantService.findTenantByIdAsync(tenantId, TenantId.fromUUID(entityId.getId()));
                break;
            case CUSTOMER:
                hasName = customerService.findCustomerByIdAsync(tenantId, new CustomerId(entityId.getId()));
                break;
            case USER:
                hasName = userService.findUserByIdAsync(tenantId, new UserId(entityId.getId()));
                break;
            case DASHBOARD:
                hasName = dashboardService.findDashboardInfoByIdAsync(tenantId, new DashboardId(entityId.getId()));
                break;
            case ALARM:
                hasName = alarmService.findAlarmByIdAsync(tenantId, new AlarmId(entityId.getId()));
                break;
            case RULE_CHAIN:
                hasName = ruleChainService.findRuleChainByIdAsync(tenantId, new RuleChainId(entityId.getId()));
                break;
            case EDGE:
                hasName = edgeService.findEdgeByIdAsync(tenantId, new EdgeId(entityId.getId()));
                break;
            case TB_RESOURCE:
                hasName = resourceService.findResourceInfoByIdAsync(tenantId, new TbResourceId(entityId.getId()));
                break;
            case OTA_PACKAGE:
                hasName = otaPackageService.findOtaPackageInfoByIdAsync(tenantId, new OtaPackageId(entityId.getId()));
                break;
            default:
                throw new IllegalStateException("Not Implemented!");
        }
        entityName = Futures.transform(hasName, (Function<HasName, String>) hasName1 -> hasName1 != null ? hasName1.getName() : null, MoreExecutors.directExecutor());
        return entityName;
    }

    @Override
    public CustomerId fetchEntityCustomerId(TenantId tenantId, EntityId entityId) {
        log.trace("Executing fetchEntityCustomerId [{}]", entityId);
        @Nullable HasCustomerId hasCustomerId = null;
        switch (entityId.getEntityType()) {
            case TENANT:
            case RULE_CHAIN:
            case RULE_NODE:
            case DASHBOARD:
            case WIDGETS_BUNDLE:
            case WIDGET_TYPE:
            case TENANT_PROFILE:
            case DEVICE_PROFILE:
            case ASSET_PROFILE:
            case API_USAGE_STATE:
            case TB_RESOURCE:
            case OTA_PACKAGE:
                break;
            case CUSTOMER:
                hasCustomerId = () -> new CustomerId(entityId.getId());
                break;
            case USER:
                hasCustomerId = userService.findUserById(tenantId, new UserId(entityId.getId()));
                break;
            case ASSET:
                hasCustomerId = assetService.findAssetById(tenantId, new AssetId(entityId.getId()));
                break;
            case DEVICE:
                hasCustomerId = deviceService.findDeviceById(tenantId, new DeviceId(entityId.getId()));
                break;
            case ALARM:
                try {
                    hasCustomerId = alarmService.findAlarmByIdAsync(tenantId, new AlarmId(entityId.getId())).get();
                } catch (Exception e) {
                }
                break;
            case ENTITY_VIEW:
                hasCustomerId = entityViewService.findEntityViewById(tenantId, new EntityViewId(entityId.getId()));
                break;
            case EDGE:
                hasCustomerId = edgeService.findEdgeById(tenantId, new EdgeId(entityId.getId()));
                break;
        }
        return hasCustomerId != null ? hasCustomerId.getCustomerId() : new CustomerId(NULL_UUID);
    }

    private static void validateEntityCountQuery(EntityCountQuery query) {
        if (query == null) {
            throw new IncorrectParameterException("Query must be specified.");
        } else if (query.getEntityFilter() == null) {
            throw new IncorrectParameterException("Query entity filter must be specified.");
        } else if (query.getEntityFilter().getType() == null) {
            throw new IncorrectParameterException("Query entity filter type must be specified.");
        } else if (query.getEntityFilter().getType().equals(EntityFilterType.RELATIONS_QUERY)) {
            validateRelationQuery((RelationsQueryFilter) query.getEntityFilter());
        }
    }

    private static void validateEntityDataQuery(EntityDataQuery query) {
        validateEntityCountQuery(query);
        Validator.validateEntityDataPageLink(query.getPageLink());
    }

    private static void validateRelationQuery(RelationsQueryFilter queryFilter) {
        if (queryFilter.isMultiRoot() && queryFilter.getMultiRootEntitiesType() ==null){
            throw new IncorrectParameterException("Multi-root relation query filter should contain 'multiRootEntitiesType'");
        }
        if (queryFilter.isMultiRoot() && CollectionUtils.isEmpty(queryFilter.getMultiRootEntityIds())) {
            throw new IncorrectParameterException("Multi-root relation query filter should contain 'multiRootEntityIds' array that contains string representation of UUIDs");
        }
        if (!queryFilter.isMultiRoot() && queryFilter.getRootEntity() == null) {
            throw new IncorrectParameterException("Relation query filter root entity should not be blank");
        }
    }
}

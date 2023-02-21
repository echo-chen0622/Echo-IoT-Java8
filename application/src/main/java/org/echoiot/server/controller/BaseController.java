package org.echoiot.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.cluster.TbClusterService;
import org.echoiot.server.common.data.*;
import org.echoiot.server.common.data.alarm.Alarm;
import org.echoiot.server.common.data.alarm.AlarmInfo;
import org.echoiot.server.common.data.asset.Asset;
import org.echoiot.server.common.data.asset.AssetInfo;
import org.echoiot.server.common.data.asset.AssetProfile;
import org.echoiot.server.common.data.edge.Edge;
import org.echoiot.server.common.data.edge.EdgeEventActionType;
import org.echoiot.server.common.data.edge.EdgeEventType;
import org.echoiot.server.common.data.edge.EdgeInfo;
import org.echoiot.server.common.data.exception.EchoiotErrorCode;
import org.echoiot.server.common.data.exception.EchoiotException;
import org.echoiot.server.common.data.id.*;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.data.page.SortOrder;
import org.echoiot.server.common.data.page.TimePageLink;
import org.echoiot.server.common.data.plugin.ComponentDescriptor;
import org.echoiot.server.common.data.plugin.ComponentType;
import org.echoiot.server.common.data.queue.Queue;
import org.echoiot.server.common.data.rpc.Rpc;
import org.echoiot.server.common.data.rule.RuleChain;
import org.echoiot.server.common.data.rule.RuleChainType;
import org.echoiot.server.common.data.rule.RuleNode;
import org.echoiot.server.common.data.widget.WidgetTypeDetails;
import org.echoiot.server.common.data.widget.WidgetsBundle;
import org.echoiot.server.dao.asset.AssetProfileService;
import org.echoiot.server.dao.asset.AssetService;
import org.echoiot.server.dao.attributes.AttributesService;
import org.echoiot.server.dao.audit.AuditLogService;
import org.echoiot.server.dao.customer.CustomerService;
import org.echoiot.server.dao.dashboard.DashboardService;
import org.echoiot.server.dao.device.ClaimDevicesService;
import org.echoiot.server.dao.device.DeviceCredentialsService;
import org.echoiot.server.dao.device.DeviceProfileService;
import org.echoiot.server.dao.device.DeviceService;
import org.echoiot.server.dao.edge.EdgeService;
import org.echoiot.server.dao.entityview.EntityViewService;
import org.echoiot.server.dao.exception.DataValidationException;
import org.echoiot.server.dao.exception.IncorrectParameterException;
import org.echoiot.server.dao.model.ModelConstants;
import org.echoiot.server.dao.oauth2.OAuth2ConfigTemplateService;
import org.echoiot.server.dao.oauth2.OAuth2Service;
import org.echoiot.server.dao.ota.OtaPackageService;
import org.echoiot.server.dao.queue.QueueService;
import org.echoiot.server.dao.relation.RelationService;
import org.echoiot.server.dao.rpc.RpcService;
import org.echoiot.server.dao.rule.RuleChainService;
import org.echoiot.server.dao.service.Validator;
import org.echoiot.server.dao.tenant.TbTenantProfileCache;
import org.echoiot.server.dao.tenant.TenantProfileService;
import org.echoiot.server.dao.tenant.TenantService;
import org.echoiot.server.dao.user.UserService;
import org.echoiot.server.dao.widget.WidgetTypeService;
import org.echoiot.server.dao.widget.WidgetsBundleService;
import org.echoiot.server.exception.EchoiotErrorResponseHandler;
import org.echoiot.server.queue.discovery.PartitionService;
import org.echoiot.server.queue.provider.TbQueueProducerProvider;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.component.ComponentDiscoveryService;
import org.echoiot.server.service.edge.rpc.EdgeRpcService;
import org.echoiot.server.service.entitiy.TbNotificationEntityService;
import org.echoiot.server.service.ota.OtaPackageStateService;
import org.echoiot.server.service.profile.TbAssetProfileCache;
import org.echoiot.server.service.profile.TbDeviceProfileCache;
import org.echoiot.server.service.resource.TbResourceService;
import org.echoiot.server.service.security.model.SecurityUser;
import org.echoiot.server.service.security.permission.AccessControlService;
import org.echoiot.server.service.security.permission.Operation;
import org.echoiot.server.service.security.permission.PerResource;
import org.echoiot.server.service.state.DeviceStateService;
import org.echoiot.server.service.sync.vc.EntitiesVersionControlService;
import org.echoiot.server.service.telemetry.AlarmSubscriptionService;
import org.echoiot.server.service.telemetry.TelemetrySubscriptionService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.context.request.async.DeferredResult;

import javax.annotation.Resource;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.echoiot.server.controller.ControllerConstants.INCORRECT_TENANT_ID;
import static org.echoiot.server.dao.service.Validator.validateId;

@Slf4j
@TbCoreComponent
public abstract class BaseController {

    /*Swagger UI description*/

    private static final ObjectMapper json = new ObjectMapper();

    @Resource
    private EchoiotErrorResponseHandler errorResponseHandler;

    @Resource
    protected AccessControlService accessControlService;

    @Resource
    protected TenantService tenantService;

    @Resource
    protected TenantProfileService tenantProfileService;

    @Resource
    protected CustomerService customerService;

    @Resource
    protected UserService userService;

    @Resource
    protected DeviceService deviceService;

    @Resource
    protected DeviceProfileService deviceProfileService;

    @Resource
    protected AssetService assetService;

    @Resource
    protected AssetProfileService assetProfileService;

    @Resource
    protected AlarmSubscriptionService alarmService;

    @Resource
    protected DeviceCredentialsService deviceCredentialsService;

    @Resource
    protected WidgetsBundleService widgetsBundleService;

    @Resource
    protected WidgetTypeService widgetTypeService;

    @Resource
    protected DashboardService dashboardService;

    @Resource
    protected OAuth2Service oAuth2Service;

    @Resource
    protected OAuth2ConfigTemplateService oAuth2ConfigTemplateService;

    @Resource
    protected ComponentDiscoveryService componentDescriptorService;

    @Resource
    protected RuleChainService ruleChainService;

    @Resource
    protected TbClusterService tbClusterService;

    @Resource
    protected RelationService relationService;

    @Resource
    protected AuditLogService auditLogService;

    @Resource
    protected DeviceStateService deviceStateService;

    @Resource
    protected EntityViewService entityViewService;

    @Resource
    protected TelemetrySubscriptionService tsSubService;

    @Resource
    protected AttributesService attributesService;

    @Resource
    protected ClaimDevicesService claimDevicesService;

    @Resource
    protected PartitionService partitionService;

    @Resource
    protected TbResourceService resourceService;

    @Resource
    protected OtaPackageService otaPackageService;

    @Resource
    protected OtaPackageStateService otaPackageStateService;

    @Resource
    protected RpcService rpcService;

    @Resource
    protected TbQueueProducerProvider producerProvider;

    @Resource
    protected TbTenantProfileCache tenantProfileCache;

    @Resource
    protected TbDeviceProfileCache deviceProfileCache;

    @Resource
    protected TbAssetProfileCache assetProfileCache;

    @Autowired(required = false)
    protected EdgeService edgeService;

    @Autowired(required = false)
    protected EdgeRpcService edgeRpcService;

    @Resource
    protected TbNotificationEntityService notificationEntityService;

    @Resource
    protected QueueService queueService;

    @Resource
    protected EntitiesVersionControlService vcService;

    @Value("${server.log_controller_error_stack_trace}")
    @Getter
    private boolean logControllerErrorStackTrace;

    @Value("${edges.enabled}")
    @Getter
    protected boolean edgesEnabled;

    @ExceptionHandler(Exception.class)
    public void handleControllerException(@NotNull Exception e, @NotNull HttpServletResponse response) {
        @NotNull EchoiotException echoiotException = handleException(e);
        if (echoiotException.getErrorCode() == EchoiotErrorCode.GENERAL && echoiotException.getCause() instanceof Exception
            && StringUtils.equals(echoiotException.getCause().getMessage(), echoiotException.getMessage())) {
            e = (Exception) echoiotException.getCause();
        } else {
            e = echoiotException;
        }
        errorResponseHandler.handle(e, response);
    }

    @ExceptionHandler(EchoiotException.class)
    public void handleEchoiotException(@NotNull EchoiotException ex, @NotNull HttpServletResponse response) {
        errorResponseHandler.handle(ex, response);
    }

    /**
     * @deprecated Exceptions that are not of {@link EchoiotException} type
     * are now caught and mapped to {@link EchoiotException} by
     * {@link ExceptionHandler} {@link BaseController#handleControllerException(Exception, HttpServletResponse)}
     * which basically acts like the following boilerplate:
     * {@code
     *  try {
     *      someExceptionThrowingMethod();
     *  } catch (Exception e) {
     *      throw handleException(e);
     *  }
     * }
     * */
    @NotNull
    @Deprecated
    EchoiotException handleException(@NotNull Exception exception) {
        return handleException(exception, true);
    }

    @NotNull
    private EchoiotException handleException(@NotNull Exception exception, boolean logException) {
        if (logException && logControllerErrorStackTrace) {
            log.error("Error [{}]", exception.getMessage(), exception);
        }

        String cause = "";
        if (exception.getCause() != null) {
            cause = exception.getCause().getClass().getCanonicalName();
        }

        if (exception instanceof EchoiotException) {
            return (EchoiotException) exception;
        } else if (exception instanceof IllegalArgumentException || exception instanceof IncorrectParameterException
                   || exception instanceof DataValidationException || cause.contains("IncorrectParameterException")) {
            return new EchoiotException(exception.getMessage(), EchoiotErrorCode.BAD_REQUEST_PARAMS);
        } else if (exception instanceof MessagingException) {
            return new EchoiotException("Unable to send mail: " + exception.getMessage(), EchoiotErrorCode.GENERAL);
        } else if (exception instanceof AsyncRequestTimeoutException) {
            return new EchoiotException("Request timeout", EchoiotErrorCode.GENERAL);
        } else {
            return new EchoiotException(exception.getMessage(), exception, EchoiotErrorCode.GENERAL);
        }
    }

    /**
     * Handles validation error for controller method arguments annotated with @{@link javax.validation.Valid}
     * */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public void handleValidationError(@NotNull MethodArgumentNotValidException e, @NotNull HttpServletResponse response) {
        @NotNull String errorMessage = "Validation error: " + e.getBindingResult().getAllErrors().stream()
                                                               .map(DefaultMessageSourceResolvable::getDefaultMessage)
                                                               .collect(Collectors.joining(", "));
        @NotNull EchoiotException echoiotException = new EchoiotException(errorMessage, EchoiotErrorCode.BAD_REQUEST_PARAMS);
        handleEchoiotException(echoiotException, response);
    }

    <T> T checkNotNull(@NotNull T reference) throws EchoiotException {
        return checkNotNull(reference, "Requested item wasn't found!");
    }

    @NotNull
    <T> T checkNotNull(@NotNull T reference, String notFoundMessage) throws EchoiotException {
        if (reference == null) {
            throw new EchoiotException(notFoundMessage, EchoiotErrorCode.ITEM_NOT_FOUND);
        }
        return reference;
    }

    <T> T checkNotNull(@NotNull Optional<T> reference) throws EchoiotException {
        return checkNotNull(reference, "Requested item wasn't found!");
    }

    @NotNull
    <T> T checkNotNull(@NotNull Optional<T> reference, String notFoundMessage) throws EchoiotException {
        if (reference.isPresent()) {
            return reference.get();
        } else {
            throw new EchoiotException(notFoundMessage, EchoiotErrorCode.ITEM_NOT_FOUND);
        }
    }

    void checkParameter(String name, String param) throws EchoiotException {
        if (StringUtils.isEmpty(param)) {
            throw new EchoiotException("Parameter '" + name + "' can't be empty!", EchoiotErrorCode.BAD_REQUEST_PARAMS);
        }
    }

    void checkArrayParameter(String name, @NotNull String[] params) throws EchoiotException {
        if (params == null || params.length == 0) {
            throw new EchoiotException("Parameter '" + name + "' can't be empty!", EchoiotErrorCode.BAD_REQUEST_PARAMS);
        } else {
            for (String param : params) {
                checkParameter(name, param);
            }
        }
    }

    @NotNull
    UUID toUUID(@NotNull String id) throws EchoiotException {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw handleException(e, false);
        }
    }

    @NotNull
    PageLink createPageLink(int pageSize, int page, String textSearch, String sortProperty, @NotNull String sortOrder) throws EchoiotException {
        if (StringUtils.isNotEmpty(sortProperty)) {
            if (!Validator.isValidProperty(sortProperty)) {
                throw new IllegalArgumentException("Invalid sort property");
            }
            @NotNull SortOrder.Direction direction = SortOrder.Direction.ASC;
            if (StringUtils.isNotEmpty(sortOrder)) {
                try {
                    direction = SortOrder.Direction.valueOf(sortOrder.toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new EchoiotException("Unsupported sort order '" + sortOrder + "'! Only 'ASC' or 'DESC' types are allowed.", EchoiotErrorCode.BAD_REQUEST_PARAMS);
                }
            }
            @NotNull SortOrder sort = new SortOrder(sortProperty, direction);
            return new PageLink(pageSize, page, textSearch, sort);
        } else {
            return new PageLink(pageSize, page, textSearch);
        }
    }

    @NotNull
    TimePageLink createTimePageLink(int pageSize, int page, String textSearch,
                                    String sortProperty, @NotNull String sortOrder, Long startTime, Long endTime) throws EchoiotException {
        @NotNull PageLink pageLink = this.createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return new TimePageLink(pageLink, startTime, endTime);
    }

    protected SecurityUser getCurrentUser() throws EchoiotException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof SecurityUser) {
            return (SecurityUser) authentication.getPrincipal();
        } else {
            throw new EchoiotException("You aren't authorized to perform this operation!", EchoiotErrorCode.AUTHENTICATION);
        }
    }

    Tenant checkTenantId(@NotNull TenantId tenantId, Operation operation) throws EchoiotException {
        try {
            validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
            Tenant tenant = tenantService.findTenantById(tenantId);
            checkNotNull(tenant, "Tenant with id [" + tenantId + "] is not found");
            accessControlService.checkPermission(getCurrentUser(), PerResource.TENANT, operation, tenantId, tenant);
            return tenant;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    TenantInfo checkTenantInfoId(@NotNull TenantId tenantId, Operation operation) throws EchoiotException {
        try {
            validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
            TenantInfo tenant = tenantService.findTenantInfoById(tenantId);
            checkNotNull(tenant, "Tenant with id [" + tenantId + "] is not found");
            accessControlService.checkPermission(getCurrentUser(), PerResource.TENANT, operation, tenantId, tenant);
            return tenant;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    TenantProfile checkTenantProfileId(@NotNull TenantProfileId tenantProfileId, Operation operation) throws EchoiotException {
        try {
            Validator.validateId(tenantProfileId, "Incorrect tenantProfileId " + tenantProfileId);
            TenantProfile tenantProfile = tenantProfileService.findTenantProfileById(getTenantId(), tenantProfileId);
            checkNotNull(tenantProfile, "Tenant profile with id [" + tenantProfileId + "] is not found");
            accessControlService.checkPermission(getCurrentUser(), PerResource.TENANT_PROFILE, operation);
            return tenantProfile;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    protected TenantId getTenantId() throws EchoiotException {
        return getCurrentUser().getTenantId();
    }

    Customer checkCustomerId(@NotNull CustomerId customerId, Operation operation) throws EchoiotException {
        try {
            Validator.validateId(customerId, "Incorrect customerId " + customerId);
            Customer customer = customerService.findCustomerById(getTenantId(), customerId);
            checkNotNull(customer, "Customer with id [" + customerId + "] is not found");
            accessControlService.checkPermission(getCurrentUser(), PerResource.CUSTOMER, operation, customerId, customer);
            return customer;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    User checkUserId(@NotNull UserId userId, Operation operation) throws EchoiotException {
        try {
            Validator.validateId(userId, "Incorrect userId " + userId);
            User user = userService.findUserById(getCurrentUser().getTenantId(), userId);
            checkNotNull(user, "User with id [" + userId + "] is not found");
            accessControlService.checkPermission(getCurrentUser(), PerResource.USER, operation, userId, user);
            return user;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    protected <I extends EntityId, T extends HasTenantId> void checkEntity(@Nullable I entityId, T entity, PerResource perResource) throws EchoiotException {
        if (entityId == null) {
            accessControlService
                    .checkPermission(getCurrentUser(), perResource, Operation.CREATE, null, entity);
        } else {
            checkEntityId(entityId, Operation.WRITE);
        }
    }

    protected void checkEntityId(@NotNull EntityId entityId, Operation operation) throws EchoiotException {
        try {
            if (entityId == null) {
                throw new EchoiotException("Parameter entityId can't be empty!", EchoiotErrorCode.BAD_REQUEST_PARAMS);
            }
            Validator.validateId(entityId.getId(), "Incorrect entityId " + entityId);
            switch (entityId.getEntityType()) {
                case ALARM:
                    checkAlarmId(new AlarmId(entityId.getId()), operation);
                    return;
                case DEVICE:
                    checkDeviceId(new DeviceId(entityId.getId()), operation);
                    return;
                case DEVICE_PROFILE:
                    checkDeviceProfileId(new DeviceProfileId(entityId.getId()), operation);
                    return;
                case CUSTOMER:
                    checkCustomerId(new CustomerId(entityId.getId()), operation);
                    return;
                case TENANT:
                    checkTenantId(TenantId.fromUUID(entityId.getId()), operation);
                    return;
                case TENANT_PROFILE:
                    checkTenantProfileId(new TenantProfileId(entityId.getId()), operation);
                    return;
                case RULE_CHAIN:
                    checkRuleChain(new RuleChainId(entityId.getId()), operation);
                    return;
                case RULE_NODE:
                    checkRuleNode(new RuleNodeId(entityId.getId()), operation);
                    return;
                case ASSET:
                    checkAssetId(new AssetId(entityId.getId()), operation);
                    return;
                case ASSET_PROFILE:
                    checkAssetProfileId(new AssetProfileId(entityId.getId()), operation);
                    return;
                case DASHBOARD:
                    checkDashboardId(new DashboardId(entityId.getId()), operation);
                    return;
                case USER:
                    checkUserId(new UserId(entityId.getId()), operation);
                    return;
                case ENTITY_VIEW:
                    checkEntityViewId(new EntityViewId(entityId.getId()), operation);
                    return;
                case EDGE:
                    checkEdgeId(new EdgeId(entityId.getId()), operation);
                    return;
                case WIDGETS_BUNDLE:
                    checkWidgetsBundleId(new WidgetsBundleId(entityId.getId()), operation);
                    return;
                case WIDGET_TYPE:
                    checkWidgetTypeId(new WidgetTypeId(entityId.getId()), operation);
                    return;
                case TB_RESOURCE:
                    checkResourceId(new TbResourceId(entityId.getId()), operation);
                    return;
                case OTA_PACKAGE:
                    checkOtaPackageId(new OtaPackageId(entityId.getId()), operation);
                    return;
                case QUEUE:
                    checkQueueId(new QueueId(entityId.getId()), operation);
                    return;
                default:
                    throw new IllegalArgumentException("Unsupported entity type: " + entityId.getEntityType());
            }
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    Device checkDeviceId(@NotNull DeviceId deviceId, Operation operation) throws EchoiotException {
        try {
            Validator.validateId(deviceId, "Incorrect deviceId " + deviceId);
            Device device = deviceService.findDeviceById(getCurrentUser().getTenantId(), deviceId);
            checkNotNull(device, "Device with id [" + deviceId + "] is not found");
            accessControlService.checkPermission(getCurrentUser(), PerResource.DEVICE, operation, deviceId, device);
            return device;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    DeviceInfo checkDeviceInfoId(@NotNull DeviceId deviceId, Operation operation) throws EchoiotException {
        try {
            Validator.validateId(deviceId, "Incorrect deviceId " + deviceId);
            DeviceInfo device = deviceService.findDeviceInfoById(getCurrentUser().getTenantId(), deviceId);
            checkNotNull(device, "Device with id [" + deviceId + "] is not found");
            accessControlService.checkPermission(getCurrentUser(), PerResource.DEVICE, operation, deviceId, device);
            return device;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    DeviceProfile checkDeviceProfileId(@NotNull DeviceProfileId deviceProfileId, Operation operation) throws EchoiotException {
        try {
            Validator.validateId(deviceProfileId, "Incorrect deviceProfileId " + deviceProfileId);
            DeviceProfile deviceProfile = deviceProfileService.findDeviceProfileById(getCurrentUser().getTenantId(), deviceProfileId);
            checkNotNull(deviceProfile, "Device profile with id [" + deviceProfileId + "] is not found");
            accessControlService.checkPermission(getCurrentUser(), PerResource.DEVICE_PROFILE, operation, deviceProfileId, deviceProfile);
            return deviceProfile;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    protected EntityView checkEntityViewId(@NotNull EntityViewId entityViewId, Operation operation) throws EchoiotException {
        try {
            Validator.validateId(entityViewId, "Incorrect entityViewId " + entityViewId);
            EntityView entityView = entityViewService.findEntityViewById(getCurrentUser().getTenantId(), entityViewId);
            checkNotNull(entityView, "Entity view with id [" + entityViewId + "] is not found");
            accessControlService.checkPermission(getCurrentUser(), PerResource.ENTITY_VIEW, operation, entityViewId, entityView);
            return entityView;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    EntityViewInfo checkEntityViewInfoId(@NotNull EntityViewId entityViewId, Operation operation) throws EchoiotException {
        try {
            Validator.validateId(entityViewId, "Incorrect entityViewId " + entityViewId);
            EntityViewInfo entityView = entityViewService.findEntityViewInfoById(getCurrentUser().getTenantId(), entityViewId);
            checkNotNull(entityView, "Entity view with id [" + entityViewId + "] is not found");
            accessControlService.checkPermission(getCurrentUser(), PerResource.ENTITY_VIEW, operation, entityViewId, entityView);
            return entityView;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    Asset checkAssetId(@NotNull AssetId assetId, Operation operation) throws EchoiotException {
        try {
            Validator.validateId(assetId, "Incorrect assetId " + assetId);
            Asset asset = assetService.findAssetById(getCurrentUser().getTenantId(), assetId);
            checkNotNull(asset, "Asset with id [" + assetId + "] is not found");
            accessControlService.checkPermission(getCurrentUser(), PerResource.ASSET, operation, assetId, asset);
            return asset;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    AssetInfo checkAssetInfoId(@NotNull AssetId assetId, Operation operation) throws EchoiotException {
        try {
            Validator.validateId(assetId, "Incorrect assetId " + assetId);
            AssetInfo asset = assetService.findAssetInfoById(getCurrentUser().getTenantId(), assetId);
            checkNotNull(asset, "Asset with id [" + assetId + "] is not found");
            accessControlService.checkPermission(getCurrentUser(), PerResource.ASSET, operation, assetId, asset);
            return asset;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    AssetProfile checkAssetProfileId(@NotNull AssetProfileId assetProfileId, Operation operation) throws EchoiotException {
        try {
            Validator.validateId(assetProfileId, "Incorrect assetProfileId " + assetProfileId);
            AssetProfile assetProfile = assetProfileService.findAssetProfileById(getCurrentUser().getTenantId(), assetProfileId);
            checkNotNull(assetProfile, "Asset profile with id [" + assetProfileId + "] is not found");
            accessControlService.checkPermission(getCurrentUser(), PerResource.ASSET_PROFILE, operation, assetProfileId, assetProfile);
            return assetProfile;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    Alarm checkAlarmId(@NotNull AlarmId alarmId, Operation operation) throws EchoiotException {
        try {
            Validator.validateId(alarmId, "Incorrect alarmId " + alarmId);
            Alarm alarm = alarmService.findAlarmByIdAsync(getCurrentUser().getTenantId(), alarmId).get();
            checkNotNull(alarm, "Alarm with id [" + alarmId + "] is not found");
            accessControlService.checkPermission(getCurrentUser(), PerResource.ALARM, operation, alarmId, alarm);
            return alarm;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    AlarmInfo checkAlarmInfoId(@NotNull AlarmId alarmId, Operation operation) throws EchoiotException {
        try {
            Validator.validateId(alarmId, "Incorrect alarmId " + alarmId);
            AlarmInfo alarmInfo = alarmService.findAlarmInfoByIdAsync(getCurrentUser().getTenantId(), alarmId).get();
            checkNotNull(alarmInfo, "Alarm with id [" + alarmId + "] is not found");
            accessControlService.checkPermission(getCurrentUser(), PerResource.ALARM, operation, alarmId, alarmInfo);
            return alarmInfo;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    WidgetsBundle checkWidgetsBundleId(@NotNull WidgetsBundleId widgetsBundleId, Operation operation) throws EchoiotException {
        try {
            Validator.validateId(widgetsBundleId, "Incorrect widgetsBundleId " + widgetsBundleId);
            WidgetsBundle widgetsBundle = widgetsBundleService.findWidgetsBundleById(getCurrentUser().getTenantId(), widgetsBundleId);
            checkNotNull(widgetsBundle, "Widgets bundle with id [" + widgetsBundleId + "] is not found");
            accessControlService.checkPermission(getCurrentUser(), PerResource.WIDGETS_BUNDLE, operation, widgetsBundleId, widgetsBundle);
            return widgetsBundle;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    WidgetTypeDetails checkWidgetTypeId(@NotNull WidgetTypeId widgetTypeId, Operation operation) throws EchoiotException {
        try {
            Validator.validateId(widgetTypeId, "Incorrect widgetTypeId " + widgetTypeId);
            WidgetTypeDetails widgetTypeDetails = widgetTypeService.findWidgetTypeDetailsById(getCurrentUser().getTenantId(), widgetTypeId);
            checkNotNull(widgetTypeDetails, "Widget type with id [" + widgetTypeId + "] is not found");
            accessControlService.checkPermission(getCurrentUser(), PerResource.WIDGET_TYPE, operation, widgetTypeId, widgetTypeDetails);
            return widgetTypeDetails;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    Dashboard checkDashboardId(@NotNull DashboardId dashboardId, Operation operation) throws EchoiotException {
        try {
            Validator.validateId(dashboardId, "Incorrect dashboardId " + dashboardId);
            Dashboard dashboard = dashboardService.findDashboardById(getCurrentUser().getTenantId(), dashboardId);
            checkNotNull(dashboard, "Dashboard with id [" + dashboardId + "] is not found");
            accessControlService.checkPermission(getCurrentUser(), PerResource.DASHBOARD, operation, dashboardId, dashboard);
            return dashboard;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    Edge checkEdgeId(@NotNull EdgeId edgeId, Operation operation) throws EchoiotException {
        try {
            Validator.validateId(edgeId, "Incorrect edgeId " + edgeId);
            Edge edge = edgeService.findEdgeById(getTenantId(), edgeId);
            checkNotNull(edge, "Edge with id [" + edgeId + "] is not found");
            accessControlService.checkPermission(getCurrentUser(), PerResource.EDGE, operation, edgeId, edge);
            return edge;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    EdgeInfo checkEdgeInfoId(@NotNull EdgeId edgeId, Operation operation) throws EchoiotException {
        try {
            Validator.validateId(edgeId, "Incorrect edgeId " + edgeId);
            EdgeInfo edge = edgeService.findEdgeInfoById(getCurrentUser().getTenantId(), edgeId);
            checkNotNull(edge, "Edge with id [" + edgeId + "] is not found");
            accessControlService.checkPermission(getCurrentUser(), PerResource.EDGE, operation, edgeId, edge);
            return edge;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    DashboardInfo checkDashboardInfoId(@NotNull DashboardId dashboardId, Operation operation) throws EchoiotException {
        try {
            Validator.validateId(dashboardId, "Incorrect dashboardId " + dashboardId);
            DashboardInfo dashboardInfo = dashboardService.findDashboardInfoById(getCurrentUser().getTenantId(), dashboardId);
            checkNotNull(dashboardInfo, "Dashboard with id [" + dashboardId + "] is not found");
            accessControlService.checkPermission(getCurrentUser(), PerResource.DASHBOARD, operation, dashboardId, dashboardInfo);
            return dashboardInfo;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    ComponentDescriptor checkComponentDescriptorByClazz(String clazz) throws EchoiotException {
        try {
            log.debug("[{}] Lookup component descriptor", clazz);
            return checkNotNull(componentDescriptorService.getComponent(clazz));
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    List<ComponentDescriptor> checkComponentDescriptorsByType(ComponentType type, RuleChainType ruleChainType) throws EchoiotException {
        try {
            log.debug("[{}] Lookup component descriptors", type);
            return componentDescriptorService.getComponents(type, ruleChainType);
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    List<ComponentDescriptor> checkComponentDescriptorsByTypes(Set<ComponentType> types, RuleChainType ruleChainType) throws EchoiotException {
        try {
            log.debug("[{}] Lookup component descriptors", types);
            return componentDescriptorService.getComponents(types, ruleChainType);
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    protected RuleChain checkRuleChain(@NotNull RuleChainId ruleChainId, Operation operation) throws EchoiotException {
        Validator.validateId(ruleChainId, "Incorrect ruleChainId " + ruleChainId);
        RuleChain ruleChain = ruleChainService.findRuleChainById(getCurrentUser().getTenantId(), ruleChainId);
        checkNotNull(ruleChain, "Rule chain with id [" + ruleChainId + "] is not found");
        accessControlService.checkPermission(getCurrentUser(), PerResource.RULE_CHAIN, operation, ruleChainId, ruleChain);
        return ruleChain;
    }

    @NotNull
    protected RuleNode checkRuleNode(@NotNull RuleNodeId ruleNodeId, Operation operation) throws EchoiotException {
        Validator.validateId(ruleNodeId, "Incorrect ruleNodeId " + ruleNodeId);
        RuleNode ruleNode = ruleChainService.findRuleNodeById(getTenantId(), ruleNodeId);
        checkNotNull(ruleNode, "Rule node with id [" + ruleNodeId + "] is not found");
        checkRuleChain(ruleNode.getRuleChainId(), operation);
        return ruleNode;
    }

    TbResource checkResourceId(@NotNull TbResourceId resourceId, Operation operation) throws EchoiotException {
        try {
            Validator.validateId(resourceId, "Incorrect resourceId " + resourceId);
            TbResource resource = resourceService.findResourceById(getCurrentUser().getTenantId(), resourceId);
            checkNotNull(resource, "Resource with id [" + resourceId + "] is not found");
            accessControlService.checkPermission(getCurrentUser(), PerResource.TB_RESOURCE, operation, resourceId, resource);
            return resource;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    TbResourceInfo checkResourceInfoId(@NotNull TbResourceId resourceId, Operation operation) throws EchoiotException {
        try {
            Validator.validateId(resourceId, "Incorrect resourceId " + resourceId);
            TbResourceInfo resourceInfo = resourceService.findResourceInfoById(getCurrentUser().getTenantId(), resourceId);
            checkNotNull(resourceInfo, "Resource with id [" + resourceId + "] is not found");
            accessControlService.checkPermission(getCurrentUser(), PerResource.TB_RESOURCE, operation, resourceId, resourceInfo);
            return resourceInfo;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    OtaPackage checkOtaPackageId(@NotNull OtaPackageId otaPackageId, Operation operation) throws EchoiotException {
        try {
            Validator.validateId(otaPackageId, "Incorrect otaPackageId " + otaPackageId);
            OtaPackage otaPackage = otaPackageService.findOtaPackageById(getCurrentUser().getTenantId(), otaPackageId);
            checkNotNull(otaPackage, "OTA package with id [" + otaPackageId + "] is not found");
            accessControlService.checkPermission(getCurrentUser(), PerResource.OTA_PACKAGE, operation, otaPackageId, otaPackage);
            return otaPackage;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    OtaPackageInfo checkOtaPackageInfoId(@NotNull OtaPackageId otaPackageId, Operation operation) throws EchoiotException {
        try {
            Validator.validateId(otaPackageId, "Incorrect otaPackageId " + otaPackageId);
            OtaPackageInfo otaPackageIn = otaPackageService.findOtaPackageInfoById(getCurrentUser().getTenantId(), otaPackageId);
            checkNotNull(otaPackageIn, "OTA package with id [" + otaPackageId + "] is not found");
            accessControlService.checkPermission(getCurrentUser(), PerResource.OTA_PACKAGE, operation, otaPackageId, otaPackageIn);
            return otaPackageIn;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    Rpc checkRpcId(@NotNull RpcId rpcId, Operation operation) throws EchoiotException {
        try {
            Validator.validateId(rpcId, "Incorrect rpcId " + rpcId);
            Rpc rpc = rpcService.findById(getCurrentUser().getTenantId(), rpcId);
            checkNotNull(rpc, "RPC with id [" + rpcId + "] is not found");
            accessControlService.checkPermission(getCurrentUser(), PerResource.RPC, operation, rpcId, rpc);
            return rpc;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    @NotNull
    protected Queue checkQueueId(@NotNull QueueId queueId, Operation operation) throws EchoiotException {
        Validator.validateId(queueId, "Incorrect queueId " + queueId);
        Queue queue = queueService.findQueueById(getCurrentUser().getTenantId(), queueId);
        checkNotNull(queue);
        accessControlService.checkPermission(getCurrentUser(), PerResource.QUEUE, operation, queueId, queue);
        TenantId tenantId = getTenantId();
        if (queue.getTenantId().isNullUid() && !tenantId.isNullUid()) {
            @Nullable TenantProfile tenantProfile = tenantProfileCache.get(tenantId);
            if (tenantProfile.isIsolatedTbRuleEngine()) {
                throw new EchoiotException(UserController.YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION,
                                               EchoiotErrorCode.PERMISSION_DENIED);
            }
        }
        return queue;
    }

    protected <I extends EntityId> I emptyId(@NotNull EntityType entityType) {
        return (I) EntityIdFactory.getByTypeAndUuid(entityType, ModelConstants.NULL_UUID);
    }

    public static Exception toException(@Nullable Throwable error) {
        return error != null ? (error instanceof Exception ? (Exception) error : new Exception(error)) : null;
    }

    protected void sendEntityNotificationMsg(TenantId tenantId, EntityId entityId, EdgeEventActionType action) {
        sendNotificationMsgToEdge(tenantId, null, entityId, null, null, action);
    }

    protected void sendEntityAssignToEdgeNotificationMsg(TenantId tenantId, EdgeId edgeId, EntityId entityId, EdgeEventActionType action) {
        sendNotificationMsgToEdge(tenantId, edgeId, entityId, null, null, action);
    }

    private void sendNotificationMsgToEdge(TenantId tenantId, EdgeId edgeId, EntityId entityId, String body, EdgeEventType type, EdgeEventActionType action) {
        tbClusterService.sendNotificationMsgToEdge(tenantId, edgeId, entityId, body, type, action);
    }

    protected void processDashboardIdFromAdditionalInfo(@NotNull ObjectNode additionalInfo, String requiredFields) throws EchoiotException {
        String dashboardId = additionalInfo.has(requiredFields) ? additionalInfo.get(requiredFields).asText() : null;
        if (dashboardId != null && !dashboardId.equals("null")) {
            if (dashboardService.findDashboardById(getTenantId(), new DashboardId(UUID.fromString(dashboardId))) == null) {
                additionalInfo.remove(requiredFields);
            }
        }
    }

    @NotNull
    protected MediaType parseMediaType(@NotNull String contentType) {
        try {
            return MediaType.parseMediaType(contentType);
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    @NotNull
    protected <T> DeferredResult<T> wrapFuture(@NotNull ListenableFuture<T> future) {
        @NotNull final DeferredResult<T> deferredResult = new DeferredResult<>();
        Futures.addCallback(future, new FutureCallback<>() {
            @Override
            public void onSuccess(T result) {
                deferredResult.setResult(result);
            }

            @Override
            public void onFailure(Throwable t) {
                deferredResult.setErrorResult(t);
            }
        }, MoreExecutors.directExecutor());
        return deferredResult;
    }
}

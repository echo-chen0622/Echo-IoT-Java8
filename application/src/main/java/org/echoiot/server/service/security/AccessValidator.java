package org.echoiot.server.service.security;

import com.google.common.base.Function;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.echoiot.common.util.EchoiotThreadFactory;
import org.echoiot.server.common.data.*;
import org.echoiot.server.common.data.asset.Asset;
import org.echoiot.server.common.data.asset.AssetProfile;
import org.echoiot.server.common.data.edge.Edge;
import org.echoiot.server.common.data.exception.EchoiotException;
import org.echoiot.server.common.data.id.*;
import org.echoiot.server.common.data.rpc.Rpc;
import org.echoiot.server.common.data.rule.RuleChain;
import org.echoiot.server.common.data.rule.RuleNode;
import org.echoiot.server.controller.HttpValidationCallback;
import org.echoiot.server.dao.alarm.AlarmService;
import org.echoiot.server.dao.asset.AssetProfileService;
import org.echoiot.server.dao.asset.AssetService;
import org.echoiot.server.dao.customer.CustomerService;
import org.echoiot.server.dao.device.DeviceProfileService;
import org.echoiot.server.dao.device.DeviceService;
import org.echoiot.server.dao.edge.EdgeService;
import org.echoiot.server.dao.entityview.EntityViewService;
import org.echoiot.server.dao.exception.IncorrectParameterException;
import org.echoiot.server.dao.ota.OtaPackageService;
import org.echoiot.server.dao.resource.ResourceService;
import org.echoiot.server.dao.rpc.RpcService;
import org.echoiot.server.dao.rule.RuleChainService;
import org.echoiot.server.dao.tenant.TenantService;
import org.echoiot.server.dao.usagerecord.ApiUsageStateService;
import org.echoiot.server.dao.user.UserService;
import org.echoiot.server.service.security.model.SecurityUser;
import org.echoiot.server.service.security.permission.AccessControlService;
import org.echoiot.server.service.security.permission.Operation;
import org.echoiot.server.service.security.permission.PerResource;
import org.echoiot.server.service.telemetry.exception.ToErrorResponseEntity;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.async.DeferredResult;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

/**
 * Created by Echo on 27.03.18.
 */
@Component
public class AccessValidator {

    public static final String ONLY_SYSTEM_ADMINISTRATOR_IS_ALLOWED_TO_PERFORM_THIS_OPERATION = "Only system administrator is allowed to perform this operation!";
    public static final String CUSTOMER_USER_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION = "Customer user is not allowed to perform this operation!";
    public static final String SYSTEM_ADMINISTRATOR_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION = "System administrator is not allowed to perform this operation!";
    public static final String DEVICE_WITH_REQUESTED_ID_NOT_FOUND = "Device with requested id wasn't found!";
    public static final String EDGE_WITH_REQUESTED_ID_NOT_FOUND = "Edge with requested id wasn't found!";
    public static final String ENTITY_VIEW_WITH_REQUESTED_ID_NOT_FOUND = "Entity-view with requested id wasn't found!";

    @Resource
    protected TenantService tenantService;

    @Resource
    protected CustomerService customerService;

    @Resource
    protected UserService userService;

    @Resource
    protected DeviceService deviceService;

    @Resource
    protected DeviceProfileService deviceProfileService;

    @Resource
    protected AssetProfileService assetProfileService;

    @Resource
    protected AssetService assetService;

    @Resource
    protected AlarmService alarmService;

    @Resource
    protected RuleChainService ruleChainService;

    @Resource
    protected EntityViewService entityViewService;

    @Autowired(required = false)
    protected EdgeService edgeService;

    @Resource
    protected AccessControlService accessControlService;

    @Resource
    protected ApiUsageStateService apiUsageStateService;

    @Resource
    protected ResourceService resourceService;

    @Resource
    protected OtaPackageService otaPackageService;

    @Resource
    protected RpcService rpcService;

    private ExecutorService executor;

    @PostConstruct
    public void initExecutor() {
        executor = Executors.newSingleThreadExecutor(EchoiotThreadFactory.forName("access-validator"));
    }

    @PreDestroy
    public void shutdownExecutor() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    public DeferredResult<ResponseEntity> validateEntityAndCallback(@NotNull SecurityUser currentUser, @NotNull Operation operation, String entityType, @NotNull String entityIdStr,
                                                                    @NotNull ThreeConsumer<DeferredResult<ResponseEntity>, TenantId, EntityId> onSuccess) throws EchoiotException {
        return validateEntityAndCallback(currentUser, operation, entityType, entityIdStr, onSuccess, (result, t) -> handleError(t, result, HttpStatus.INTERNAL_SERVER_ERROR));
    }

    public DeferredResult<ResponseEntity> validateEntityAndCallback(@NotNull SecurityUser currentUser, @NotNull Operation operation, String entityType, @NotNull String entityIdStr,
                                                                    @NotNull ThreeConsumer<DeferredResult<ResponseEntity>, TenantId, EntityId> onSuccess,
                                                                    @NotNull BiConsumer<DeferredResult<ResponseEntity>, Throwable> onFailure) throws EchoiotException {
        return validateEntityAndCallback(currentUser, operation, EntityIdFactory.getByTypeAndId(entityType, entityIdStr),
                                         onSuccess, onFailure);
    }

    public DeferredResult<ResponseEntity> validateEntityAndCallback(@NotNull SecurityUser currentUser, @NotNull Operation operation, @NotNull EntityId entityId,
                                                                    @NotNull ThreeConsumer<DeferredResult<ResponseEntity>, TenantId, EntityId> onSuccess) throws EchoiotException {
        return validateEntityAndCallback(currentUser, operation, entityId, onSuccess, (result, t) -> handleError(t, result, HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @NotNull
    public DeferredResult<ResponseEntity> validateEntityAndCallback(@NotNull SecurityUser currentUser, @NotNull Operation operation, @NotNull EntityId entityId,
                                                                    @NotNull ThreeConsumer<DeferredResult<ResponseEntity>, TenantId, EntityId> onSuccess,
                                                                    @NotNull BiConsumer<DeferredResult<ResponseEntity>, Throwable> onFailure) throws EchoiotException {

        @NotNull final DeferredResult<ResponseEntity> response = new DeferredResult<>();

        validate(currentUser, operation, entityId, new HttpValidationCallback(response,
                new FutureCallback<DeferredResult<ResponseEntity>>() {
                    @Override
                    public void onSuccess(@Nullable DeferredResult<ResponseEntity> result) {
                        try {
                            onSuccess.accept(response, currentUser.getTenantId(), entityId);
                        } catch (Exception e) {
                            onFailure(e);
                        }
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        onFailure.accept(response, t);
                    }
                }));

        return response;
    }

    public void validate(@NotNull SecurityUser currentUser, @NotNull Operation operation, @NotNull EntityId entityId, @NotNull FutureCallback<ValidationResult> callback) {
        switch (entityId.getEntityType()) {
            case DEVICE:
                validateDevice(currentUser, operation, entityId, callback);
                return;
            case DEVICE_PROFILE:
                validateDeviceProfile(currentUser, operation, entityId, callback);
                return;
            case ASSET:
                validateAsset(currentUser, operation, entityId, callback);
                return;
            case ASSET_PROFILE:
                validateAssetProfile(currentUser, operation, entityId, callback);
                return;
            case RULE_CHAIN:
                validateRuleChain(currentUser, operation, entityId, callback);
                return;
            case CUSTOMER:
                validateCustomer(currentUser, operation, entityId, callback);
                return;
            case TENANT:
                validateTenant(currentUser, operation, entityId, callback);
                return;
            case TENANT_PROFILE:
                validateTenantProfile(currentUser, operation, entityId, callback);
                return;
            case USER:
                validateUser(currentUser, operation, entityId, callback);
                return;
            case ENTITY_VIEW:
                validateEntityView(currentUser, operation, entityId, callback);
                return;
            case EDGE:
                validateEdge(currentUser, operation, entityId, callback);
                return;
            case API_USAGE_STATE:
                validateApiUsageState(currentUser, operation, entityId, callback);
                return;
            case TB_RESOURCE:
                validateResource(currentUser, operation, entityId, callback);
                return;
            case OTA_PACKAGE:
                validateOtaPackage(currentUser, operation, entityId, callback);
                return;
            case RPC:
                validateRpc(currentUser, operation, entityId, callback);
                return;
            default:
                //TODO: add support of other entities
                throw new IllegalStateException("Not Implemented!");
        }
    }

    private void validateDevice(@NotNull final SecurityUser currentUser, Operation operation, @NotNull EntityId entityId, @NotNull FutureCallback<ValidationResult> callback) {
        if (currentUser.isSystemAdmin()) {
            callback.onSuccess(ValidationResult.accessDenied(SYSTEM_ADMINISTRATOR_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else {
            ListenableFuture<Device> deviceFuture = deviceService.findDeviceByIdAsync(currentUser.getTenantId(), new DeviceId(entityId.getId()));
            Futures.addCallback(deviceFuture, getCallback(callback, device -> {
                if (device == null) {
                    return ValidationResult.entityNotFound(DEVICE_WITH_REQUESTED_ID_NOT_FOUND);
                } else {
                    try {
                        accessControlService.checkPermission(currentUser, PerResource.DEVICE, operation, entityId, device);
                    } catch (EchoiotException e) {
                        return ValidationResult.accessDenied(e.getMessage());
                    }
                    return ValidationResult.ok(device);
                }
            }), executor);
        }
    }

    private void validateRpc(@NotNull final SecurityUser currentUser, Operation operation, @NotNull EntityId entityId, @NotNull FutureCallback<ValidationResult> callback) {
        ListenableFuture<Rpc> rpcFurure = rpcService.findRpcByIdAsync(currentUser.getTenantId(), new RpcId(entityId.getId()));
        Futures.addCallback(rpcFurure, getCallback(callback, rpc -> {
            if (rpc == null) {
                return ValidationResult.entityNotFound("Rpc with requested id wasn't found!");
            } else {
                try {
                    accessControlService.checkPermission(currentUser, PerResource.RPC, operation, entityId, rpc);
                } catch (EchoiotException e) {
                    return ValidationResult.accessDenied(e.getMessage());
                }
                return ValidationResult.ok(rpc);
            }
        }), executor);
    }

    private void validateDeviceProfile(@NotNull final SecurityUser currentUser, Operation operation, @NotNull EntityId entityId, @NotNull FutureCallback<ValidationResult> callback) {
        if (currentUser.isSystemAdmin()) {
            callback.onSuccess(ValidationResult.accessDenied(SYSTEM_ADMINISTRATOR_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else {
            DeviceProfile deviceProfile = deviceProfileService.findDeviceProfileById(currentUser.getTenantId(), new DeviceProfileId(entityId.getId()));
            if (deviceProfile == null) {
                callback.onSuccess(ValidationResult.entityNotFound("Device profile with requested id wasn't found!"));
            } else {
                try {
                    accessControlService.checkPermission(currentUser, PerResource.DEVICE_PROFILE, operation, entityId, deviceProfile);
                } catch (EchoiotException e) {
                    callback.onSuccess(ValidationResult.accessDenied(e.getMessage()));
                }
                callback.onSuccess(ValidationResult.ok(deviceProfile));
            }
        }
    }

    private void validateAssetProfile(@NotNull final SecurityUser currentUser, Operation operation, @NotNull EntityId entityId, @NotNull FutureCallback<ValidationResult> callback) {
        if (currentUser.isSystemAdmin()) {
            callback.onSuccess(ValidationResult.accessDenied(SYSTEM_ADMINISTRATOR_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else {
            AssetProfile assetProfile = assetProfileService.findAssetProfileById(currentUser.getTenantId(), new AssetProfileId(entityId.getId()));
            if (assetProfile == null) {
                callback.onSuccess(ValidationResult.entityNotFound("Asset profile with requested id wasn't found!"));
            } else {
                try {
                    accessControlService.checkPermission(currentUser, PerResource.ASSET_PROFILE, operation, entityId, assetProfile);
                } catch (EchoiotException e) {
                    callback.onSuccess(ValidationResult.accessDenied(e.getMessage()));
                }
                callback.onSuccess(ValidationResult.ok(assetProfile));
            }
        }
    }

    private void validateApiUsageState(@NotNull final SecurityUser currentUser, @NotNull Operation operation, @NotNull EntityId entityId, @NotNull FutureCallback<ValidationResult> callback) {
        if (currentUser.isSystemAdmin()) {
            callback.onSuccess(ValidationResult.accessDenied(SYSTEM_ADMINISTRATOR_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else {
            if (!operation.equals(Operation.READ_TELEMETRY)) {
                callback.onSuccess(ValidationResult.accessDenied("Allowed only READ_TELEMETRY operation!"));
            }
            ApiUsageState apiUsageState = apiUsageStateService.findApiUsageStateById(currentUser.getTenantId(), new ApiUsageStateId(entityId.getId()));
            if (apiUsageState == null) {
                callback.onSuccess(ValidationResult.entityNotFound("Api Usage State with requested id wasn't found!"));
            } else {
                try {
                    accessControlService.checkPermission(currentUser, PerResource.API_USAGE_STATE, operation, entityId, apiUsageState);
                } catch (EchoiotException e) {
                    callback.onSuccess(ValidationResult.accessDenied(e.getMessage()));
                }
                callback.onSuccess(ValidationResult.ok(apiUsageState));
            }
        }
    }

    private void validateOtaPackage(@NotNull final SecurityUser currentUser, Operation operation, @NotNull EntityId entityId, @NotNull FutureCallback<ValidationResult> callback) {
        if (currentUser.isSystemAdmin()) {
            callback.onSuccess(ValidationResult.accessDenied(SYSTEM_ADMINISTRATOR_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else {
            OtaPackageInfo otaPackage = otaPackageService.findOtaPackageInfoById(currentUser.getTenantId(), new OtaPackageId(entityId.getId()));
            if (otaPackage == null) {
                callback.onSuccess(ValidationResult.entityNotFound("OtaPackage with requested id wasn't found!"));
            } else {
                try {
                    accessControlService.checkPermission(currentUser, PerResource.OTA_PACKAGE, operation, entityId, otaPackage);
                } catch (EchoiotException e) {
                    callback.onSuccess(ValidationResult.accessDenied(e.getMessage()));
                }
                callback.onSuccess(ValidationResult.ok(otaPackage));
            }
        }
    }

    private void validateResource(@NotNull SecurityUser currentUser, Operation operation, @NotNull EntityId entityId, @NotNull FutureCallback<ValidationResult> callback) {
        ListenableFuture<TbResourceInfo> resourceFuture = resourceService.findResourceInfoByIdAsync(currentUser.getTenantId(), new TbResourceId(entityId.getId()));
        Futures.addCallback(resourceFuture, getCallback(callback, resource -> {
            if (resource == null) {
                return ValidationResult.entityNotFound("Resource with requested id wasn't found!");
            } else {
                try {
                    accessControlService.checkPermission(currentUser, PerResource.TB_RESOURCE, operation, entityId, resource);
                } catch (EchoiotException e) {
                    return ValidationResult.accessDenied(e.getMessage());
                }
                return ValidationResult.ok(resource);
            }
        }), executor);
    }

    private void validateAsset(@NotNull final SecurityUser currentUser, Operation operation, @NotNull EntityId entityId, @NotNull FutureCallback<ValidationResult> callback) {
        if (currentUser.isSystemAdmin()) {
            callback.onSuccess(ValidationResult.accessDenied(SYSTEM_ADMINISTRATOR_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else {
            ListenableFuture<Asset> assetFuture = assetService.findAssetByIdAsync(currentUser.getTenantId(), new AssetId(entityId.getId()));
            Futures.addCallback(assetFuture, getCallback(callback, asset -> {
                if (asset == null) {
                    return ValidationResult.entityNotFound("Asset with requested id wasn't found!");
                } else {
                    try {
                        accessControlService.checkPermission(currentUser, PerResource.ASSET, operation, entityId, asset);
                    } catch (EchoiotException e) {
                        return ValidationResult.accessDenied(e.getMessage());
                    }
                    return ValidationResult.ok(asset);
                }
            }), executor);
        }
    }

    private void validateRuleChain(@NotNull final SecurityUser currentUser, Operation operation, @NotNull EntityId entityId, @NotNull FutureCallback<ValidationResult> callback) {
        if (currentUser.isCustomerUser()) {
            callback.onSuccess(ValidationResult.accessDenied(CUSTOMER_USER_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else {
            ListenableFuture<RuleChain> ruleChainFuture = ruleChainService.findRuleChainByIdAsync(currentUser.getTenantId(), new RuleChainId(entityId.getId()));
            Futures.addCallback(ruleChainFuture, getCallback(callback, ruleChain -> {
                if (ruleChain == null) {
                    return ValidationResult.entityNotFound("Rule chain with requested id wasn't found!");
                } else {
                    try {
                        accessControlService.checkPermission(currentUser, PerResource.RULE_CHAIN, operation, entityId, ruleChain);
                    } catch (EchoiotException e) {
                        return ValidationResult.accessDenied(e.getMessage());
                    }
                    return ValidationResult.ok(ruleChain);
                }
            }), executor);
        }
    }

    private void validateRule(@NotNull final SecurityUser currentUser, Operation operation, @NotNull EntityId entityId, @NotNull FutureCallback<ValidationResult> callback) {
        if (currentUser.isCustomerUser()) {
            callback.onSuccess(ValidationResult.accessDenied(CUSTOMER_USER_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else {
            ListenableFuture<RuleNode> ruleNodeFuture = ruleChainService.findRuleNodeByIdAsync(currentUser.getTenantId(), new RuleNodeId(entityId.getId()));
            Futures.addCallback(ruleNodeFuture, getCallback(callback, ruleNodeTmp -> {
                RuleNode ruleNode = ruleNodeTmp;
                if (ruleNode == null) {
                    return ValidationResult.entityNotFound("Rule node with requested id wasn't found!");
                } else if (ruleNode.getRuleChainId() == null) {
                    return ValidationResult.entityNotFound("Rule chain with requested node id wasn't found!");
                } else {
                    //TODO: make async
                    RuleChain ruleChain = ruleChainService.findRuleChainById(currentUser.getTenantId(), ruleNode.getRuleChainId());
                    try {
                        accessControlService.checkPermission(currentUser, PerResource.RULE_CHAIN, operation, ruleNode.getRuleChainId(), ruleChain);
                    } catch (EchoiotException e) {
                        return ValidationResult.accessDenied(e.getMessage());
                    }
                    return ValidationResult.ok(ruleNode);
                }
            }), executor);
        }
    }

    private void validateCustomer(@NotNull final SecurityUser currentUser, Operation operation, @NotNull EntityId entityId, @NotNull FutureCallback<ValidationResult> callback) {
        if (currentUser.isSystemAdmin()) {
            callback.onSuccess(ValidationResult.accessDenied(SYSTEM_ADMINISTRATOR_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else {
            ListenableFuture<Customer> customerFuture = customerService.findCustomerByIdAsync(currentUser.getTenantId(), new CustomerId(entityId.getId()));
            Futures.addCallback(customerFuture, getCallback(callback, customer -> {
                if (customer == null) {
                    return ValidationResult.entityNotFound("Customer with requested id wasn't found!");
                } else {
                    try {
                        accessControlService.checkPermission(currentUser, PerResource.CUSTOMER, operation, entityId, customer);
                    } catch (EchoiotException e) {
                        return ValidationResult.accessDenied(e.getMessage());
                    }
                    return ValidationResult.ok(customer);
                }
            }), executor);
        }
    }

    private void validateTenant(@NotNull final SecurityUser currentUser, Operation operation, @NotNull EntityId entityId, @NotNull FutureCallback<ValidationResult> callback) {
        if (currentUser.isCustomerUser()) {
            callback.onSuccess(ValidationResult.accessDenied(CUSTOMER_USER_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else if (currentUser.isSystemAdmin()) {
            callback.onSuccess(ValidationResult.ok(null));
        } else {
            ListenableFuture<Tenant> tenantFuture = tenantService.findTenantByIdAsync(currentUser.getTenantId(), TenantId.fromUUID(entityId.getId()));
            Futures.addCallback(tenantFuture, getCallback(callback, tenant -> {
                if (tenant == null) {
                    return ValidationResult.entityNotFound("Tenant with requested id wasn't found!");
                }
                try {
                    accessControlService.checkPermission(currentUser, PerResource.TENANT, operation, entityId, tenant);
                } catch (EchoiotException e) {
                    return ValidationResult.accessDenied(e.getMessage());
                }
                return ValidationResult.ok(tenant);

            }), executor);
        }
    }

    private void validateTenantProfile(@NotNull final SecurityUser currentUser, Operation operation, EntityId entityId, @NotNull FutureCallback<ValidationResult> callback) {
        if (currentUser.isSystemAdmin()) {
            callback.onSuccess(ValidationResult.ok(null));
        } else {
            callback.onSuccess(ValidationResult.accessDenied(ONLY_SYSTEM_ADMINISTRATOR_IS_ALLOWED_TO_PERFORM_THIS_OPERATION));
        }
    }

    private void validateUser(@NotNull final SecurityUser currentUser, Operation operation, @NotNull EntityId entityId, @NotNull FutureCallback<ValidationResult> callback) {
        ListenableFuture<User> userFuture = userService.findUserByIdAsync(currentUser.getTenantId(), new UserId(entityId.getId()));
        Futures.addCallback(userFuture, getCallback(callback, user -> {
            if (user == null) {
                return ValidationResult.entityNotFound("User with requested id wasn't found!");
            }
            try {
                accessControlService.checkPermission(currentUser, PerResource.USER, operation, entityId, user);
            } catch (EchoiotException e) {
                return ValidationResult.accessDenied(e.getMessage());
            }
            return ValidationResult.ok(user);

        }), executor);
    }

    private void validateEntityView(@NotNull final SecurityUser currentUser, Operation operation, @NotNull EntityId entityId, @NotNull FutureCallback<ValidationResult> callback) {
        if (currentUser.isSystemAdmin()) {
            callback.onSuccess(ValidationResult.accessDenied(SYSTEM_ADMINISTRATOR_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else {
            ListenableFuture<EntityView> entityViewFuture = entityViewService.findEntityViewByIdAsync(currentUser.getTenantId(), new EntityViewId(entityId.getId()));
            Futures.addCallback(entityViewFuture, getCallback(callback, entityView -> {
                if (entityView == null) {
                    return ValidationResult.entityNotFound(ENTITY_VIEW_WITH_REQUESTED_ID_NOT_FOUND);
                } else {
                    try {
                        accessControlService.checkPermission(currentUser, PerResource.ENTITY_VIEW, operation, entityId, entityView);
                    } catch (EchoiotException e) {
                        return ValidationResult.accessDenied(e.getMessage());
                    }
                    return ValidationResult.ok(entityView);
                }
            }), executor);
        }
    }

    private void validateEdge(@NotNull final SecurityUser currentUser, Operation operation, @NotNull EntityId entityId, @NotNull FutureCallback<ValidationResult> callback) {
        if (currentUser.isSystemAdmin()) {
            callback.onSuccess(ValidationResult.accessDenied(SYSTEM_ADMINISTRATOR_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else {
            ListenableFuture<Edge> edgeFuture = edgeService.findEdgeByIdAsync(currentUser.getTenantId(), new EdgeId(entityId.getId()));
            Futures.addCallback(edgeFuture, getCallback(callback, edge -> {
                if (edge == null) {
                    return ValidationResult.entityNotFound(EDGE_WITH_REQUESTED_ID_NOT_FOUND);
                } else {
                    try {
                        accessControlService.checkPermission(currentUser, PerResource.EDGE, operation, entityId, edge);
                    } catch (EchoiotException e) {
                        return ValidationResult.accessDenied(e.getMessage());
                    }
                    return ValidationResult.ok(edge);
                }
            }), executor);
        }
    }

    @NotNull
    private <T, V> FutureCallback<T> getCallback(@NotNull FutureCallback<ValidationResult> callback, @NotNull Function<T, ValidationResult<V>> transformer) {
        return new FutureCallback<T>() {
            @Override
            public void onSuccess(@Nullable T result) {
                callback.onSuccess(transformer.apply(result));
            }

            @Override
            public void onFailure(Throwable t) {
                callback.onFailure(t);
            }
        };
    }

    public static void handleError(Throwable e, @NotNull final DeferredResult<ResponseEntity> response, @NotNull HttpStatus defaultErrorStatus) {
        ResponseEntity responseEntity;
        if (e instanceof ToErrorResponseEntity) {
            responseEntity = ((ToErrorResponseEntity) e).toErrorResponseEntity();
        } else if (e instanceof IllegalArgumentException || e instanceof IncorrectParameterException) {
            responseEntity = new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } else {
            responseEntity = new ResponseEntity<>(defaultErrorStatus);
        }
        response.setResult(responseEntity);
    }

    public interface ThreeConsumer<A, B, C> {
        void accept(A a, B b, C c);
    }
}

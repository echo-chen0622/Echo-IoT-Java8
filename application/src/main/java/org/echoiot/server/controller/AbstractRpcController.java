package org.echoiot.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.FutureCallback;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.server.common.data.DataConstants;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.audit.ActionType;
import org.echoiot.server.common.data.exception.EchoiotErrorCode;
import org.echoiot.server.common.data.exception.EchoiotException;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.id.UUIDBased;
import org.echoiot.server.common.data.rpc.RpcError;
import org.echoiot.server.common.data.rpc.ToDeviceRpcRequestBody;
import org.echoiot.server.common.msg.rpc.FromDeviceRpcResponse;
import org.echoiot.server.common.msg.rpc.ToDeviceRpcRequest;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.rpc.LocalRequestMetaData;
import org.echoiot.server.service.rpc.TbCoreDeviceRpcService;
import org.echoiot.server.service.security.AccessValidator;
import org.echoiot.server.service.security.model.SecurityUser;
import org.echoiot.server.service.security.permission.Operation;
import org.echoiot.server.service.telemetry.exception.ToErrorResponseEntity;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;

import javax.annotation.Nullable;
import javax.annotation.Resource;
import java.util.Optional;
import java.util.UUID;

/**
 * Created by Echo on 22.03.18.
 */
@TbCoreComponent
@Slf4j
public abstract class AbstractRpcController extends BaseController {

    @Resource
    protected TbCoreDeviceRpcService deviceRpcService;

    @Resource
    protected AccessValidator accessValidator;

    @Value("${server.rest.server_side_rpc.min_timeout:5000}")
    protected long minTimeout;

    @Value("${server.rest.server_side_rpc.default_timeout:10000}")
    protected long defaultTimeout;

    @NotNull
    protected DeferredResult<ResponseEntity> handleDeviceRPCRequest(boolean oneWay, @NotNull DeviceId deviceId, String requestBody, @NotNull HttpStatus timeoutStatus, @NotNull HttpStatus noActiveConnectionStatus) throws
                                                                                                                                                                                          EchoiotException {
        try {
            JsonNode rpcRequestBody = JacksonUtil.toJsonNode(requestBody);
            @NotNull ToDeviceRpcRequestBody body = new ToDeviceRpcRequestBody(rpcRequestBody.get("method").asText(), JacksonUtil.toString(rpcRequestBody.get("params")));
            SecurityUser currentUser = getCurrentUser();
            TenantId tenantId = currentUser.getTenantId();
            @NotNull final DeferredResult<ResponseEntity> response = new DeferredResult<>();
            long timeout = rpcRequestBody.has(DataConstants.TIMEOUT) ? rpcRequestBody.get(DataConstants.TIMEOUT).asLong() : defaultTimeout;
            long expTime = rpcRequestBody.has(DataConstants.EXPIRATION_TIME) ? rpcRequestBody.get(DataConstants.EXPIRATION_TIME).asLong() : System.currentTimeMillis() + Math.max(minTimeout, timeout);
            @NotNull UUID rpcRequestUUID = rpcRequestBody.has("requestUUID") ? UUID.fromString(rpcRequestBody.get("requestUUID").asText()) : UUID.randomUUID();
            boolean persisted = rpcRequestBody.has(DataConstants.PERSISTENT) && rpcRequestBody.get(DataConstants.PERSISTENT).asBoolean();
            @org.jetbrains.annotations.Nullable String additionalInfo =  JacksonUtil.toString(rpcRequestBody.get(DataConstants.ADDITIONAL_INFO));
            Integer retries = rpcRequestBody.has(DataConstants.RETRIES) ? rpcRequestBody.get(DataConstants.RETRIES).asInt() : null;
            accessValidator.validate(currentUser, Operation.RPC_CALL, deviceId, new HttpValidationCallback(response, new FutureCallback<>() {
                @Override
                public void onSuccess(@Nullable DeferredResult<ResponseEntity> result) {
                    @NotNull ToDeviceRpcRequest rpcRequest = new ToDeviceRpcRequest(rpcRequestUUID,
                                                                                    tenantId,
                                                                                    deviceId,
                                                                                    oneWay,
                                                                                    expTime,
                                                                                    body,
                                                                                    persisted,
                                                                                    retries,
                                                                                    additionalInfo
                    );
                    deviceRpcService.processRestApiRpcRequest(rpcRequest, fromDeviceRpcResponse -> reply(new LocalRequestMetaData(rpcRequest, currentUser, result), fromDeviceRpcResponse, timeoutStatus, noActiveConnectionStatus), currentUser);
                }

                @Override
                public void onFailure(Throwable e) {
                    ResponseEntity entity;
                    if (e instanceof ToErrorResponseEntity) {
                        entity = ((ToErrorResponseEntity) e).toErrorResponseEntity();
                    } else {
                        entity = new ResponseEntity(HttpStatus.UNAUTHORIZED);
                    }
                    logRpcCall(currentUser, deviceId, body, oneWay, Optional.empty(), e);
                    response.setResult(entity);
                }
            }));
            return response;
        } catch (IllegalArgumentException ioe) {
            throw new EchoiotException("Invalid request body", ioe, EchoiotErrorCode.BAD_REQUEST_PARAMS);
        }
    }

    public void reply(@NotNull LocalRequestMetaData rpcRequest, @NotNull FromDeviceRpcResponse response, @NotNull HttpStatus timeoutStatus, @NotNull HttpStatus noActiveConnectionStatus) {
        @NotNull Optional<RpcError> rpcError = response.getError();
        DeferredResult<ResponseEntity> responseWriter = rpcRequest.getResponseWriter();
        if (rpcError.isPresent()) {
            logRpcCall(rpcRequest, rpcError, null);
            @NotNull RpcError error = rpcError.get();
            switch (error) {
                case TIMEOUT:
                    responseWriter.setResult(new ResponseEntity<>(timeoutStatus));
                    break;
                case NO_ACTIVE_CONNECTION:
                    responseWriter.setResult(new ResponseEntity<>(noActiveConnectionStatus));
                    break;
                default:
                    responseWriter.setResult(new ResponseEntity<>(timeoutStatus));
                    break;
            }
        } else {
            @NotNull Optional<String> responseData = response.getResponse();
            if (responseData.isPresent() && !StringUtils.isEmpty(responseData.get())) {
                @NotNull String data = responseData.get();
                try {
                    logRpcCall(rpcRequest, rpcError, null);
                    responseWriter.setResult(new ResponseEntity<>(JacksonUtil.toJsonNode(data), HttpStatus.OK));
                } catch (IllegalArgumentException e) {
                    log.debug("Failed to decode device response: {}", data, e);
                    logRpcCall(rpcRequest, rpcError, e);
                    responseWriter.setResult(new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE));
                }
            } else {
                logRpcCall(rpcRequest, rpcError, null);
                responseWriter.setResult(new ResponseEntity<>(HttpStatus.OK));
            }
        }
    }

    private void logRpcCall(@NotNull LocalRequestMetaData rpcRequest, @NotNull Optional<RpcError> rpcError, Throwable e) {
        logRpcCall(rpcRequest.getUser(), rpcRequest.getRequest().getDeviceId(), rpcRequest.getRequest().getBody(), rpcRequest.getRequest().isOneway(), rpcError, null);
    }


    private void logRpcCall(@NotNull SecurityUser user, EntityId entityId, @NotNull ToDeviceRpcRequestBody body, boolean oneWay, @NotNull Optional<RpcError> rpcError, Throwable e) {
        @NotNull String rpcErrorStr = "";
        if (rpcError.isPresent()) {
            rpcErrorStr = "RPC Error: " + rpcError.get().name();
        }
        String method = body.getMethod();
        String params = body.getParams();

        auditLogService.logEntityAction(
                user.getTenantId(),
                user.getCustomerId(),
                user.getId(),
                user.getName(),
                (UUIDBased & EntityId) entityId,
                null,
                ActionType.RPC_CALL,
                BaseController.toException(e),
                rpcErrorStr,
                oneWay,
                method,
                params);
    }


}

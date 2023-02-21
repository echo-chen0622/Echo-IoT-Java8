package org.echoiot.server.service.edge.rpc.processor;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.server.common.data.*;
import org.echoiot.server.common.data.device.data.DeviceData;
import org.echoiot.server.common.data.edge.Edge;
import org.echoiot.server.common.data.edge.EdgeEvent;
import org.echoiot.server.common.data.edge.EdgeEventActionType;
import org.echoiot.server.common.data.edge.EdgeEventType;
import org.echoiot.server.common.data.id.*;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.data.relation.EntityRelation;
import org.echoiot.server.common.data.relation.RelationTypeGroup;
import org.echoiot.server.common.data.rpc.RpcError;
import org.echoiot.server.common.data.security.DeviceCredentials;
import org.echoiot.server.common.data.security.DeviceCredentialsType;
import org.echoiot.server.common.msg.TbMsg;
import org.echoiot.server.common.msg.TbMsgDataType;
import org.echoiot.server.common.msg.TbMsgMetaData;
import org.echoiot.server.common.msg.rpc.FromDeviceRpcResponse;
import org.echoiot.server.common.msg.session.SessionMsgType;
import org.echoiot.server.dao.exception.DataValidationException;
import org.echoiot.server.gen.edge.v1.*;
import org.echoiot.server.gen.transport.TransportProtos;
import org.echoiot.server.queue.TbQueueCallback;
import org.echoiot.server.queue.TbQueueMsgMetadata;
import org.echoiot.server.queue.util.DataDecodingEncodingService;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.rpc.FromDeviceRpcResponseActorMsg;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

@Component
@Slf4j
@TbCoreComponent
public class DeviceEdgeProcessor extends BaseEdgeProcessor {

    @Resource
    private DataDecodingEncodingService dataDecodingEncodingService;

    private static final ReentrantLock deviceCreationLock = new ReentrantLock();

    public ListenableFuture<Void> processDeviceFromEdge(TenantId tenantId, @NotNull Edge edge, @NotNull DeviceUpdateMsg deviceUpdateMsg) {
        log.trace("[{}] onDeviceUpdate [{}] from edge [{}]", tenantId, deviceUpdateMsg, edge.getName());
        switch (deviceUpdateMsg.getMsgType()) {
            case ENTITY_CREATED_RPC_MESSAGE:
                @NotNull String deviceName = deviceUpdateMsg.getName();
                Device device = deviceService.findDeviceByTenantIdAndName(tenantId, deviceName);
                if (device != null) {
                    boolean deviceAlreadyExistsForThisEdge = isDeviceAlreadyExistsOnCloudForThisEdge(tenantId, edge, device);
                    if (deviceAlreadyExistsForThisEdge) {
                        log.info("[{}] Device with name '{}' already exists on the cloud, and related to this edge [{}]. " +
                                "deviceUpdateMsg [{}], Updating device", tenantId, deviceName, edge.getId(), deviceUpdateMsg);
                        return updateDevice(tenantId, edge, deviceUpdateMsg);
                    } else {
                        log.info("[{}] Device with name '{}' already exists on the cloud, but not related to this edge [{}]. deviceUpdateMsg [{}]." +
                                "Creating a new device with random prefix and relate to this edge", tenantId, deviceName, edge.getId(), deviceUpdateMsg);
                        @NotNull String newDeviceName = deviceUpdateMsg.getName() + "_" + StringUtils.randomAlphabetic(15);
                        Device newDevice;
                        try {
                            newDevice = createDevice(tenantId, edge, deviceUpdateMsg, newDeviceName);
                        } catch (DataValidationException e) {
                            log.error("[{}] Device update msg can't be processed due to data validation [{}]", tenantId, deviceUpdateMsg, e);
                            return Futures.immediateFuture(null);
                        }
                        ObjectNode body = JacksonUtil.OBJECT_MAPPER.createObjectNode();
                        body.put("conflictName", deviceName);
                        @NotNull ListenableFuture<Void> input = saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.DEVICE, EdgeEventActionType.ENTITY_MERGE_REQUEST, newDevice.getId(), body);
                        return Futures.transformAsync(input, unused ->
                                saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.DEVICE, EdgeEventActionType.CREDENTIALS_REQUEST, newDevice.getId(), null),
                                dbCallbackExecutorService);
                    }
                } else {
                    log.info("[{}] Creating new device on the cloud [{}]", tenantId, deviceUpdateMsg);
                    try {
                        device = createDevice(tenantId, edge, deviceUpdateMsg, deviceUpdateMsg.getName());
                    } catch (DataValidationException e) {
                        log.error("[{}] Device update msg can't be processed due to data validation [{}]", tenantId, deviceUpdateMsg, e);
                        return Futures.immediateFuture(null);
                    }
                    return saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.DEVICE, EdgeEventActionType.CREDENTIALS_REQUEST, device.getId(), null);
                }
            case ENTITY_UPDATED_RPC_MESSAGE:
                return updateDevice(tenantId, edge, deviceUpdateMsg);
            case ENTITY_DELETED_RPC_MESSAGE:
                @NotNull DeviceId deviceId = new DeviceId(new UUID(deviceUpdateMsg.getIdMSB(), deviceUpdateMsg.getIdLSB()));
                Device deviceToDelete = deviceService.findDeviceById(tenantId, deviceId);
                if (deviceToDelete != null) {
                    deviceService.unassignDeviceFromEdge(tenantId, deviceId, edge.getId());
                }
                return Futures.immediateFuture(null);
            case UNRECOGNIZED:
            default:
                log.error("Unsupported msg type {}", deviceUpdateMsg.getMsgType());
                return Futures.immediateFailedFuture(new RuntimeException("Unsupported msg type " + deviceUpdateMsg.getMsgType()));
        }
    }

    private boolean isDeviceAlreadyExistsOnCloudForThisEdge(TenantId tenantId, @NotNull Edge edge, @NotNull Device device) {
        PageLink pageLink = new PageLink(DEFAULT_PAGE_SIZE);
        PageData<EdgeId> pageData;
        do {
            pageData = edgeService.findRelatedEdgeIdsByEntityId(tenantId, device.getId(), pageLink);
            if (pageData != null && pageData.getData() != null && !pageData.getData().isEmpty()) {
                if (pageData.getData().contains(edge.getId())) {
                    return true;
                }
                if (pageData.hasNext()) {
                    pageLink = pageLink.nextPageLink();
                }
            }
        } while (pageData != null && pageData.hasNext());
        return false;
    }

    @NotNull
    public ListenableFuture<Void> processDeviceCredentialsFromEdge(TenantId tenantId, @NotNull DeviceCredentialsUpdateMsg deviceCredentialsUpdateMsg) {
        log.debug("Executing onDeviceCredentialsUpdate, deviceCredentialsUpdateMsg [{}]", deviceCredentialsUpdateMsg);
        @NotNull DeviceId deviceId = new DeviceId(new UUID(deviceCredentialsUpdateMsg.getDeviceIdMSB(), deviceCredentialsUpdateMsg.getDeviceIdLSB()));
        ListenableFuture<Device> deviceFuture = deviceService.findDeviceByIdAsync(tenantId, deviceId);
        return Futures.transform(deviceFuture, device -> {
            if (device != null) {
                log.debug("Updating device credentials for device [{}]. New device credentials Id [{}], value [{}]",
                        device.getName(), deviceCredentialsUpdateMsg.getCredentialsId(), deviceCredentialsUpdateMsg.getCredentialsValue());
                try {
                    DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, device.getId());
                    deviceCredentials.setCredentialsType(DeviceCredentialsType.valueOf(deviceCredentialsUpdateMsg.getCredentialsType()));
                    deviceCredentials.setCredentialsId(deviceCredentialsUpdateMsg.getCredentialsId());
                    if (deviceCredentialsUpdateMsg.hasCredentialsValue()) {
                        deviceCredentials.setCredentialsValue(deviceCredentialsUpdateMsg.getCredentialsValue());
                    }
                    deviceCredentialsService.updateDeviceCredentials(tenantId, deviceCredentials);
                } catch (Exception e) {
                    log.error("Can't update device credentials for device [{}], deviceCredentialsUpdateMsg [{}]", device.getName(), deviceCredentialsUpdateMsg, e);
                    throw new RuntimeException(e);
                }
            }
            return null;
        }, dbCallbackExecutorService);
    }


    private ListenableFuture<Void> updateDevice(TenantId tenantId, @NotNull Edge edge, @NotNull DeviceUpdateMsg deviceUpdateMsg) {
        @NotNull DeviceId deviceId = new DeviceId(new UUID(deviceUpdateMsg.getIdMSB(), deviceUpdateMsg.getIdLSB()));
        Device device = deviceService.findDeviceById(tenantId, deviceId);
        if (device != null) {
            device.setName(deviceUpdateMsg.getName());
            device.setType(deviceUpdateMsg.getType());
            if (deviceUpdateMsg.hasLabel()) {
                device.setLabel(deviceUpdateMsg.getLabel());
            }
            if (deviceUpdateMsg.hasAdditionalInfo()) {
                device.setAdditionalInfo(JacksonUtil.toJsonNode(deviceUpdateMsg.getAdditionalInfo()));
            }
            if (deviceUpdateMsg.hasDeviceProfileIdMSB() && deviceUpdateMsg.hasDeviceProfileIdLSB()) {
                @NotNull DeviceProfileId deviceProfileId = new DeviceProfileId(
                        new UUID(deviceUpdateMsg.getDeviceProfileIdMSB(),
                                deviceUpdateMsg.getDeviceProfileIdLSB()));
                device.setDeviceProfileId(deviceProfileId);
            }
            device.setCustomerId(getCustomerId(deviceUpdateMsg));
            Optional<DeviceData> deviceDataOpt =
                    dataDecodingEncodingService.decode(deviceUpdateMsg.getDeviceDataBytes().toByteArray());
            if (deviceDataOpt.isPresent()) {
                device.setDeviceData(deviceDataOpt.get());
            }
            Device savedDevice = deviceService.saveDevice(device);
            tbClusterService.onDeviceUpdated(savedDevice, device, false);
            return saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.DEVICE, EdgeEventActionType.CREDENTIALS_REQUEST, deviceId, null);
        } else {
            String errMsg = String.format("[%s] can't find device [%s], edge [%s]", tenantId, deviceUpdateMsg, edge.getId());
            log.warn(errMsg);
            return Futures.immediateFailedFuture(new RuntimeException(errMsg));
        }
    }

    @NotNull
    private Device createDevice(TenantId tenantId, @NotNull Edge edge, @NotNull DeviceUpdateMsg deviceUpdateMsg, String deviceName) {
        Device device;
        deviceCreationLock.lock();
        try {
            log.debug("[{}] Creating device entity [{}] from edge [{}]", tenantId, deviceUpdateMsg, edge.getName());
            @NotNull DeviceId deviceId = new DeviceId(new UUID(deviceUpdateMsg.getIdMSB(), deviceUpdateMsg.getIdLSB()));
            device = deviceService.findDeviceById(tenantId, deviceId);
            boolean created = false;
            if (device == null) {
                device = new Device();
                device.setTenantId(tenantId);
                device.setCreatedTime(Uuids.unixTimestamp(deviceId.getId()));
                created = true;
            }
            device.setName(deviceName);
            device.setType(deviceUpdateMsg.getType());
            if (deviceUpdateMsg.hasLabel()) {
                device.setLabel(deviceUpdateMsg.getLabel());
            }
            if (deviceUpdateMsg.hasAdditionalInfo()) {
                device.setAdditionalInfo(JacksonUtil.toJsonNode(deviceUpdateMsg.getAdditionalInfo()));
            }
            if (deviceUpdateMsg.hasDeviceProfileIdMSB() && deviceUpdateMsg.hasDeviceProfileIdLSB()) {
                @NotNull DeviceProfileId deviceProfileId = new DeviceProfileId(
                        new UUID(deviceUpdateMsg.getDeviceProfileIdMSB(),
                                deviceUpdateMsg.getDeviceProfileIdLSB()));
                device.setDeviceProfileId(deviceProfileId);
            }
            device.setCustomerId(getCustomerId(deviceUpdateMsg));
            Optional<DeviceData> deviceDataOpt =
                    dataDecodingEncodingService.decode(deviceUpdateMsg.getDeviceDataBytes().toByteArray());
            if (deviceDataOpt.isPresent()) {
                device.setDeviceData(deviceDataOpt.get());
            }
            if (created) {
                deviceValidator.validate(device, Device::getTenantId);
                device.setId(deviceId);
            } else {
                deviceValidator.validate(device, Device::getTenantId);
            }
            Device savedDevice = deviceService.saveDevice(device, false);
            tbClusterService.onDeviceUpdated(savedDevice, created ? null : device, false);
            if (created) {
                @NotNull DeviceCredentials deviceCredentials = new DeviceCredentials();
                deviceCredentials.setDeviceId(new DeviceId(savedDevice.getUuidId()));
                deviceCredentials.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN);
                deviceCredentials.setCredentialsId(StringUtils.randomAlphanumeric(20));
                deviceCredentialsService.createDeviceCredentials(device.getTenantId(), deviceCredentials);
            }
            createRelationFromEdge(tenantId, edge.getId(), device.getId());
            pushDeviceCreatedEventToRuleEngine(tenantId, edge, device);
            deviceService.assignDeviceToEdge(edge.getTenantId(), device.getId(), edge.getId());
        } finally {
            deviceCreationLock.unlock();
        }
        return device;
    }

    @Nullable
    private CustomerId getCustomerId(@NotNull DeviceUpdateMsg deviceUpdateMsg) {
        if (deviceUpdateMsg.hasCustomerIdMSB() && deviceUpdateMsg.hasCustomerIdLSB()) {
            return new CustomerId(new UUID(deviceUpdateMsg.getCustomerIdMSB(), deviceUpdateMsg.getCustomerIdLSB()));
        } else {
            return null;
        }
    }

    private void createRelationFromEdge(TenantId tenantId, EdgeId edgeId, EntityId entityId) {
        @NotNull EntityRelation relation = new EntityRelation();
        relation.setFrom(edgeId);
        relation.setTo(entityId);
        relation.setTypeGroup(RelationTypeGroup.COMMON);
        relation.setType(EntityRelation.EDGE_TYPE);
        relationService.saveRelation(tenantId, relation);
    }

    private void pushDeviceCreatedEventToRuleEngine(TenantId tenantId, @NotNull Edge edge, @NotNull Device device) {
        try {
            DeviceId deviceId = device.getId();
            ObjectNode entityNode = JacksonUtil.OBJECT_MAPPER.valueToTree(device);
            @NotNull TbMsg tbMsg = TbMsg.newMsg(DataConstants.ENTITY_CREATED, deviceId, device.getCustomerId(),
                                                getActionTbMsgMetaData(edge, device.getCustomerId()), TbMsgDataType.JSON, JacksonUtil.OBJECT_MAPPER.writeValueAsString(entityNode));
            tbClusterService.pushMsgToRuleEngine(tenantId, deviceId, tbMsg, new TbQueueCallback() {
                @Override
                public void onSuccess(TbQueueMsgMetadata metadata) {
                    log.debug("Successfully send ENTITY_CREATED EVENT to rule engine [{}]", device);
                }

                @Override
                public void onFailure(Throwable t) {
                    log.debug("Failed to send ENTITY_CREATED EVENT to rule engine [{}]", device, t);
                }
            });
        } catch (JsonProcessingException | IllegalArgumentException e) {
            log.warn("[{}] Failed to push device action to rule engine: {}", device.getId(), DataConstants.ENTITY_CREATED, e);
        }
    }

    @NotNull
    private TbMsgMetaData getActionTbMsgMetaData(@NotNull Edge edge, @Nullable CustomerId customerId) {
        @NotNull TbMsgMetaData metaData = getTbMsgMetaData(edge);
        if (customerId != null && !customerId.isNullUid()) {
            metaData.putValue("customerId", customerId.toString());
        }
        return metaData;
    }

    @NotNull
    private TbMsgMetaData getTbMsgMetaData(@NotNull Edge edge) {
        @NotNull TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("edgeId", edge.getId().toString());
        metaData.putValue("edgeName", edge.getName());
        return metaData;
    }

    @NotNull
    public ListenableFuture<Void> processDeviceRpcCallFromEdge(TenantId tenantId, @NotNull Edge edge, @NotNull DeviceRpcCallMsg deviceRpcCallMsg) {
        log.trace("[{}] processDeviceRpcCallFromEdge [{}]", tenantId, deviceRpcCallMsg);
        if (deviceRpcCallMsg.hasResponseMsg()) {
            return processDeviceRpcResponseFromEdge(tenantId, deviceRpcCallMsg);
        } else if (deviceRpcCallMsg.hasRequestMsg()) {
            return processDeviceRpcRequestFromEdge(tenantId, edge, deviceRpcCallMsg);
        }
        return Futures.immediateFuture(null);
    }

    @NotNull
    private ListenableFuture<Void> processDeviceRpcResponseFromEdge(TenantId tenantId, @NotNull DeviceRpcCallMsg deviceRpcCallMsg) {
        @NotNull SettableFuture<Void> futureToSet = SettableFuture.create();
        @NotNull UUID requestUuid = new UUID(deviceRpcCallMsg.getRequestUuidMSB(), deviceRpcCallMsg.getRequestUuidLSB());
        @NotNull DeviceId deviceId = new DeviceId(new UUID(deviceRpcCallMsg.getDeviceIdMSB(), deviceRpcCallMsg.getDeviceIdLSB()));

        FromDeviceRpcResponse response;
        if (!StringUtils.isEmpty(deviceRpcCallMsg.getResponseMsg().getError())) {
            response = new FromDeviceRpcResponse(requestUuid, null, RpcError.valueOf(deviceRpcCallMsg.getResponseMsg().getError()));
        } else {
            response = new FromDeviceRpcResponse(requestUuid, deviceRpcCallMsg.getResponseMsg().getResponse(), null);
        }
        @NotNull TbQueueCallback callback = new TbQueueCallback() {
            @Override
            public void onSuccess(TbQueueMsgMetadata metadata) {
                futureToSet.set(null);
            }

            @Override
            public void onFailure(@NotNull Throwable t) {
                log.error("Can't process push notification to core [{}]", deviceRpcCallMsg, t);
                futureToSet.setException(t);
            }
        };
        @NotNull FromDeviceRpcResponseActorMsg msg =
                new FromDeviceRpcResponseActorMsg(deviceRpcCallMsg.getRequestId(),
                        tenantId,
                        deviceId, response);
        tbClusterService.pushMsgToCore(msg, callback);
        return futureToSet;
    }

    @NotNull
    private ListenableFuture<Void> processDeviceRpcRequestFromEdge(TenantId tenantId, @NotNull Edge edge, @NotNull DeviceRpcCallMsg deviceRpcCallMsg) {
        @NotNull DeviceId deviceId = new DeviceId(new UUID(deviceRpcCallMsg.getDeviceIdMSB(), deviceRpcCallMsg.getDeviceIdLSB()));
        try {
            @NotNull TbMsgMetaData metaData = new TbMsgMetaData();
            @NotNull String requestId = Integer.toString(deviceRpcCallMsg.getRequestId());
            metaData.putValue("requestId", requestId);
            metaData.putValue("serviceId", deviceRpcCallMsg.getServiceId());
            metaData.putValue("sessionId", deviceRpcCallMsg.getSessionId());
            metaData.putValue(DataConstants.EDGE_ID, edge.getId().toString());
            Device device = deviceService.findDeviceById(tenantId, deviceId);
            if (device != null) {
                metaData.putValue("deviceName", device.getName());
                metaData.putValue("deviceType", device.getType());
                metaData.putValue(DataConstants.DEVICE_ID, deviceId.getId().toString());
            }
            ObjectNode data = JacksonUtil.OBJECT_MAPPER.createObjectNode();
            data.put("method", deviceRpcCallMsg.getRequestMsg().getMethod());
            data.put("params", deviceRpcCallMsg.getRequestMsg().getParams());
            @NotNull TbMsg tbMsg = TbMsg.newMsg(SessionMsgType.TO_SERVER_RPC_REQUEST.name(), deviceId, null, metaData,
                                                TbMsgDataType.JSON, JacksonUtil.OBJECT_MAPPER.writeValueAsString(data));
            tbClusterService.pushMsgToRuleEngine(tenantId, deviceId, tbMsg, new TbQueueCallback() {
                @Override
                public void onSuccess(TbQueueMsgMetadata metadata) {
                    log.debug("Successfully send TO_SERVER_RPC_REQUEST to rule engine [{}], deviceRpcCallMsg {}",
                            device, deviceRpcCallMsg);
                }

                @Override
                public void onFailure(Throwable t) {
                    log.debug("Failed to send TO_SERVER_RPC_REQUEST to rule engine [{}], deviceRpcCallMsg {}",
                            device, deviceRpcCallMsg, t);
                }
            });
        } catch (JsonProcessingException | IllegalArgumentException e) {
            log.warn("[{}] Failed to push TO_SERVER_RPC_REQUEST to rule engine. deviceRpcCallMsg {}", deviceId, deviceRpcCallMsg, e);
        }

        return Futures.immediateFuture(null);
    }

    @Nullable
    public DownlinkMsg convertDeviceEventToDownlink(@NotNull EdgeEvent edgeEvent) {
        @NotNull DeviceId deviceId = new DeviceId(edgeEvent.getEntityId());
        @Nullable DownlinkMsg downlinkMsg = null;
        switch (edgeEvent.getAction()) {
            case ADDED:
            case UPDATED:
            case ASSIGNED_TO_EDGE:
            case ASSIGNED_TO_CUSTOMER:
            case UNASSIGNED_FROM_CUSTOMER:
                Device device = deviceService.findDeviceById(edgeEvent.getTenantId(), deviceId);
                if (device != null) {
                    @NotNull UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    DeviceUpdateMsg deviceUpdateMsg =
                            deviceMsgConstructor.constructDeviceUpdatedMsg(msgType, device, null);
                    @NotNull DownlinkMsg.Builder builder = DownlinkMsg.newBuilder()
                                                                      .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                                                                      .addDeviceUpdateMsg(deviceUpdateMsg);
                    if (UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE.equals(msgType)) {
                        DeviceProfile deviceProfile = deviceProfileService.findDeviceProfileById(edgeEvent.getTenantId(), device.getDeviceProfileId());
                        builder.addDeviceProfileUpdateMsg(deviceProfileMsgConstructor.constructDeviceProfileUpdatedMsg(msgType, deviceProfile));
                    }
                    downlinkMsg = builder.build();
                }
                break;
            case DELETED:
            case UNASSIGNED_FROM_EDGE:
                DeviceUpdateMsg deviceUpdateMsg =
                        deviceMsgConstructor.constructDeviceDeleteMsg(deviceId);
                downlinkMsg = DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addDeviceUpdateMsg(deviceUpdateMsg)
                        .build();
                break;
            case CREDENTIALS_UPDATED:
                DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(edgeEvent.getTenantId(), deviceId);
                if (deviceCredentials != null) {
                    DeviceCredentialsUpdateMsg deviceCredentialsUpdateMsg =
                            deviceMsgConstructor.constructDeviceCredentialsUpdatedMsg(deviceCredentials);
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addDeviceCredentialsUpdateMsg(deviceCredentialsUpdateMsg)
                            .build();
                }
                break;
            case RPC_CALL:
                return convertRpcCallEventToDownlink(edgeEvent);
            case CREDENTIALS_REQUEST:
                return convertCredentialsRequestEventToDownlink(edgeEvent);
            case ENTITY_MERGE_REQUEST:
                return convertEntityMergeRequestEventToDownlink(edgeEvent);
        }
        return downlinkMsg;
    }

    @NotNull
    private DownlinkMsg convertRpcCallEventToDownlink(@NotNull EdgeEvent edgeEvent) {
        log.trace("Executing convertRpcCallEventToDownlink, edgeEvent [{}]", edgeEvent);
        return DownlinkMsg.newBuilder()
                .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                .addDeviceRpcCallMsg(deviceMsgConstructor.constructDeviceRpcCallMsg(edgeEvent.getEntityId(), edgeEvent.getBody()))
                .build();
    }

    @NotNull
    private DownlinkMsg convertCredentialsRequestEventToDownlink(@NotNull EdgeEvent edgeEvent) {
        @NotNull DeviceId deviceId = new DeviceId(edgeEvent.getEntityId());
        @NotNull DeviceCredentialsRequestMsg deviceCredentialsRequestMsg = DeviceCredentialsRequestMsg.newBuilder()
                                                                                                      .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                                                                                                      .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                                                                                                      .build();
        @NotNull DownlinkMsg.Builder builder = DownlinkMsg.newBuilder()
                                                          .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                                                          .addDeviceCredentialsRequestMsg(deviceCredentialsRequestMsg);
        return builder.build();
    }

    @NotNull
    public DownlinkMsg convertEntityMergeRequestEventToDownlink(@NotNull EdgeEvent edgeEvent) {
        @NotNull DeviceId deviceId = new DeviceId(edgeEvent.getEntityId());
        Device device = deviceService.findDeviceById(edgeEvent.getTenantId(), deviceId);
        @Nullable String conflictName = null;
        if(edgeEvent.getBody() != null) {
            conflictName = edgeEvent.getBody().get("conflictName").asText();
        }
        DeviceUpdateMsg deviceUpdateMsg = deviceMsgConstructor
                .constructDeviceUpdatedMsg(UpdateMsgType.ENTITY_MERGE_RPC_MESSAGE, device, conflictName);
        return DownlinkMsg.newBuilder()
                .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                .addDeviceUpdateMsg(deviceUpdateMsg)
                .build();
    }

    public ListenableFuture<Void> processDeviceNotification(TenantId tenantId, @NotNull TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        return processEntityNotification(tenantId, edgeNotificationMsg);
    }
}

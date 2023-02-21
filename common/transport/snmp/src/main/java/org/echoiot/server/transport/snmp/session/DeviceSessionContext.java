package org.echoiot.server.transport.snmp.session;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.Device;
import org.echoiot.server.common.data.DeviceProfile;
import org.echoiot.server.common.data.device.data.SnmpDeviceTransportConfiguration;
import org.echoiot.server.common.data.device.profile.SnmpDeviceProfileTransportConfiguration;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.rpc.RpcStatus;
import org.echoiot.server.common.transport.SessionMsgListener;
import org.echoiot.server.common.transport.TransportServiceCallback;
import org.echoiot.server.common.transport.session.DeviceAwareSessionContext;
import org.echoiot.server.gen.transport.TransportProtos;
import org.echoiot.server.gen.transport.TransportProtos.*;
import org.echoiot.server.transport.snmp.SnmpTransportContext;
import org.jetbrains.annotations.NotNull;
import org.snmp4j.Target;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.event.ResponseListener;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class DeviceSessionContext extends DeviceAwareSessionContext implements SessionMsgListener, ResponseListener {
    @Getter
    private Target target;
    private final String token;
    @Getter
    @Setter
    private SnmpDeviceProfileTransportConfiguration profileTransportConfiguration;
    @Getter
    @Setter
    private SnmpDeviceTransportConfiguration deviceTransportConfiguration;
    @NotNull
    @Getter
    private final Device device;

    private final SnmpTransportContext snmpTransportContext;

    private final AtomicInteger msgIdSeq = new AtomicInteger(0);
    @Getter
    private boolean isActive = true;

    @Getter
    private final List<ScheduledFuture<?>> queryingTasks = new LinkedList<>();

    public DeviceSessionContext(@NotNull Device device, DeviceProfile deviceProfile, String token,
                                @NotNull SnmpDeviceProfileTransportConfiguration profileTransportConfiguration,
                                @NotNull SnmpDeviceTransportConfiguration deviceTransportConfiguration,
                                SnmpTransportContext snmpTransportContext) throws Exception {
        super(UUID.randomUUID());
        super.setDeviceId(device.getId());
        super.setDeviceProfile(deviceProfile);
        this.device = device;

        this.token = token;
        this.snmpTransportContext = snmpTransportContext;

        this.profileTransportConfiguration = profileTransportConfiguration;
        this.deviceTransportConfiguration = deviceTransportConfiguration;

        initializeTarget(profileTransportConfiguration, deviceTransportConfiguration);
    }

    @Override
    public void onDeviceProfileUpdate(TransportProtos.SessionInfoProto newSessionInfo, @NotNull DeviceProfile deviceProfile) {
        super.onDeviceProfileUpdate(newSessionInfo, deviceProfile);
        if (isActive) {
            snmpTransportContext.onDeviceProfileUpdated(deviceProfile, this);
        }
    }

    @Override
    public void onDeviceDeleted(DeviceId deviceId) {
        snmpTransportContext.onDeviceDeleted(this);
    }

    @Override
    public void onResponse(@NotNull ResponseEvent event) {
        if (isActive) {
            snmpTransportContext.getSnmpTransportService().processResponseEvent(this, event);
        }
    }

    public void initializeTarget(@NotNull SnmpDeviceProfileTransportConfiguration profileTransportConfig, @NotNull SnmpDeviceTransportConfiguration deviceTransportConfig) throws Exception {
        log.trace("Initializing target for SNMP session of device {}", device);
        this.target = snmpTransportContext.getSnmpAuthService().setUpSnmpTarget(profileTransportConfig, deviceTransportConfig);
        log.debug("SNMP target initialized: {}", target);
    }

    public void close() {
        isActive = false;
    }

    public String getToken() {
        return token;
    }

    @Override
    public int nextMsgId() {
        return msgIdSeq.incrementAndGet();
    }

    @Override
    public void onGetAttributesResponse(GetAttributeResponseMsg getAttributesResponse) {
    }

    @Override
    public void onAttributeUpdate(UUID sessionId, @NotNull AttributeUpdateNotificationMsg attributeUpdateNotification) {
        log.trace("[{}] Received attributes update notification to device", sessionId);
        snmpTransportContext.getSnmpTransportService().onAttributeUpdate(this, attributeUpdateNotification);
    }

    @Override
    public void onRemoteSessionCloseCommand(UUID sessionId, @NotNull SessionCloseNotificationProto sessionCloseNotification) {
        log.trace("[{}] Received the remote command to close the session: {}", sessionId, sessionCloseNotification.getMessage());
    }

    @Override
    public void onToDeviceRpcRequest(UUID sessionId, @NotNull ToDeviceRpcRequestMsg toDeviceRequest) {
        log.trace("[{}] Received RPC command to device", sessionId);
        snmpTransportContext.getSnmpTransportService().onToDeviceRpcRequest(this, toDeviceRequest);
        snmpTransportContext.getTransportService().process(getSessionInfo(), toDeviceRequest, RpcStatus.DELIVERED, TransportServiceCallback.EMPTY);
    }

    @Override
    public void onToServerRpcResponse(ToServerRpcResponseMsg toServerResponse) {
    }
}

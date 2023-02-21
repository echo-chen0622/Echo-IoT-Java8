package org.echoiot.server.common.transport;

import org.echoiot.server.common.data.Device;
import org.echoiot.server.common.data.DeviceProfile;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.gen.transport.TransportProtos;
import org.echoiot.server.gen.transport.TransportProtos.*;

import java.util.Optional;
import java.util.UUID;

/**
 * Created by Echo on 04.10.18.
 */
public interface SessionMsgListener {

    void onGetAttributesResponse(GetAttributeResponseMsg getAttributesResponse);

    void onAttributeUpdate(UUID sessionId, AttributeUpdateNotificationMsg attributeUpdateNotification);

    void onRemoteSessionCloseCommand(UUID sessionId, SessionCloseNotificationProto sessionCloseNotification);

    void onToDeviceRpcRequest(UUID sessionId, ToDeviceRpcRequestMsg toDeviceRequest);

    void onToServerRpcResponse(ToServerRpcResponseMsg toServerResponse);

    void onDeviceDeleted(DeviceId deviceId);

    default void onUplinkNotification(UplinkNotificationMsg notificationMsg){}

    default void onToTransportUpdateCredentials(ToTransportUpdateCredentialsProto toTransportUpdateCredentials){}

    default void onDeviceProfileUpdate(TransportProtos.SessionInfoProto newSessionInfo, DeviceProfile deviceProfile) {}

    default void onDeviceUpdate(TransportProtos.SessionInfoProto sessionInfo, Device device,
                                Optional<DeviceProfile> deviceProfileOpt) {}

    default void onResourceUpdate(TransportProtos.ResourceUpdateMsg resourceUpdateMsgOpt) {}

    default void onResourceDelete(TransportProtos.ResourceDeleteMsg resourceUpdateMsgOpt) {}
}

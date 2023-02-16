package org.echoiot.server.common.transport;

import org.echoiot.server.common.transport.auth.GetOrCreateDeviceFromGatewayResponse;
import org.echoiot.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.echoiot.server.common.transport.service.SessionMetaData;
import org.echoiot.server.common.data.DeviceProfile;
import org.echoiot.server.common.data.DeviceTransportType;
import org.echoiot.server.common.data.rpc.RpcStatus;
import org.echoiot.server.gen.transport.TransportProtos;
import org.echoiot.server.gen.transport.TransportProtos.ClaimDeviceMsg;
import org.echoiot.server.gen.transport.TransportProtos.GetAttributeRequestMsg;
import org.echoiot.server.gen.transport.TransportProtos.GetDeviceCredentialsRequestMsg;
import org.echoiot.server.gen.transport.TransportProtos.GetDeviceCredentialsResponseMsg;
import org.echoiot.server.gen.transport.TransportProtos.GetDeviceRequestMsg;
import org.echoiot.server.gen.transport.TransportProtos.GetDeviceResponseMsg;
import org.echoiot.server.gen.transport.TransportProtos.GetEntityProfileRequestMsg;
import org.echoiot.server.gen.transport.TransportProtos.GetEntityProfileResponseMsg;
import org.echoiot.server.gen.transport.TransportProtos.GetOrCreateDeviceFromGatewayRequestMsg;
import org.echoiot.server.gen.transport.TransportProtos.GetOtaPackageRequestMsg;
import org.echoiot.server.gen.transport.TransportProtos.GetOtaPackageResponseMsg;
import org.echoiot.server.gen.transport.TransportProtos.GetResourceRequestMsg;
import org.echoiot.server.gen.transport.TransportProtos.GetResourceResponseMsg;
import org.echoiot.server.gen.transport.TransportProtos.GetSnmpDevicesRequestMsg;
import org.echoiot.server.gen.transport.TransportProtos.GetSnmpDevicesResponseMsg;
import org.echoiot.server.gen.transport.TransportProtos.LwM2MRequestMsg;
import org.echoiot.server.gen.transport.TransportProtos.LwM2MResponseMsg;
import org.echoiot.server.gen.transport.TransportProtos.PostAttributeMsg;
import org.echoiot.server.gen.transport.TransportProtos.PostTelemetryMsg;
import org.echoiot.server.gen.transport.TransportProtos.ProvisionDeviceRequestMsg;
import org.echoiot.server.gen.transport.TransportProtos.ProvisionDeviceResponseMsg;
import org.echoiot.server.gen.transport.TransportProtos.SessionEventMsg;
import org.echoiot.server.gen.transport.TransportProtos.SessionInfoProto;
import org.echoiot.server.gen.transport.TransportProtos.SubscribeToAttributeUpdatesMsg;
import org.echoiot.server.gen.transport.TransportProtos.SubscribeToRPCMsg;
import org.echoiot.server.gen.transport.TransportProtos.SubscriptionInfoProto;
import org.echoiot.server.gen.transport.TransportProtos.ToDeviceRpcRequestMsg;
import org.echoiot.server.gen.transport.TransportProtos.ToDeviceRpcResponseMsg;
import org.echoiot.server.gen.transport.TransportProtos.ToServerRpcRequestMsg;
import org.echoiot.server.gen.transport.TransportProtos.TransportToDeviceActorMsg;
import org.echoiot.server.gen.transport.TransportProtos.ValidateBasicMqttCredRequestMsg;
import org.echoiot.server.gen.transport.TransportProtos.ValidateDeviceLwM2MCredentialsRequestMsg;
import org.echoiot.server.gen.transport.TransportProtos.ValidateDeviceTokenRequestMsg;
import org.echoiot.server.gen.transport.TransportProtos.ValidateDeviceX509CertRequestMsg;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Echo on 04.10.18.
 */
public interface TransportService {

    GetEntityProfileResponseMsg getEntityProfile(GetEntityProfileRequestMsg msg);

    List<TransportProtos.GetQueueRoutingInfoResponseMsg> getQueueRoutingInfo(TransportProtos.GetAllQueueRoutingInfoRequestMsg msg);

    GetResourceResponseMsg getResource(GetResourceRequestMsg msg);

    GetSnmpDevicesResponseMsg getSnmpDevicesIds(GetSnmpDevicesRequestMsg requestMsg);

    GetDeviceResponseMsg getDevice(GetDeviceRequestMsg requestMsg);

    GetDeviceCredentialsResponseMsg getDeviceCredentials(GetDeviceCredentialsRequestMsg requestMsg);

    void process(DeviceTransportType transportType, ValidateDeviceTokenRequestMsg msg,
                 TransportServiceCallback<ValidateDeviceCredentialsResponse> callback);

    void process(DeviceTransportType transportType, ValidateBasicMqttCredRequestMsg msg,
                 TransportServiceCallback<ValidateDeviceCredentialsResponse> callback);

    void process(DeviceTransportType transportType, ValidateDeviceX509CertRequestMsg msg,
                 TransportServiceCallback<ValidateDeviceCredentialsResponse> callback);

    void process(ValidateDeviceLwM2MCredentialsRequestMsg msg,
                 TransportServiceCallback<ValidateDeviceCredentialsResponse> callback);

    void process(GetOrCreateDeviceFromGatewayRequestMsg msg,
                 TransportServiceCallback<GetOrCreateDeviceFromGatewayResponse> callback);

    void process(ProvisionDeviceRequestMsg msg,
                 TransportServiceCallback<ProvisionDeviceResponseMsg> callback);

    void onProfileUpdate(DeviceProfile deviceProfile);

    void process(LwM2MRequestMsg msg,
                 TransportServiceCallback<LwM2MResponseMsg> callback);

    void process(SessionInfoProto sessionInfo, SessionEventMsg msg, TransportServiceCallback<Void> callback);

    void process(SessionInfoProto sessionInfo, PostTelemetryMsg msg, TransportServiceCallback<Void> callback);

    void process(SessionInfoProto sessionInfo, PostAttributeMsg msg, TransportServiceCallback<Void> callback);

    void process(SessionInfoProto sessionInfo, GetAttributeRequestMsg msg, TransportServiceCallback<Void> callback);

    void process(SessionInfoProto sessionInfo, SubscribeToAttributeUpdatesMsg msg, TransportServiceCallback<Void> callback);

    void process(SessionInfoProto sessionInfo, SubscribeToRPCMsg msg, TransportServiceCallback<Void> callback);

    void process(SessionInfoProto sessionInfo, ToDeviceRpcResponseMsg msg, TransportServiceCallback<Void> callback);

    void process(SessionInfoProto sessionInfo, ToServerRpcRequestMsg msg, TransportServiceCallback<Void> callback);

    void process(SessionInfoProto sessionInfo, ToDeviceRpcRequestMsg msg, RpcStatus rpcStatus, TransportServiceCallback<Void> callback);

    void process(SessionInfoProto sessionInfo, SubscriptionInfoProto msg, TransportServiceCallback<Void> callback);

    void process(SessionInfoProto sessionInfo, ClaimDeviceMsg msg, TransportServiceCallback<Void> callback);

    void process(TransportToDeviceActorMsg msg, TransportServiceCallback<Void> callback);

    void process(SessionInfoProto sessionInfoProto, GetOtaPackageRequestMsg msg, TransportServiceCallback<GetOtaPackageResponseMsg> callback);

    SessionMetaData registerAsyncSession(SessionInfoProto sessionInfo, SessionMsgListener listener);

    SessionMetaData registerSyncSession(SessionInfoProto sessionInfo, SessionMsgListener listener, long timeout);

    void reportActivity(SessionInfoProto sessionInfo);

    void deregisterSession(SessionInfoProto sessionInfo);

    void log(SessionInfoProto sessionInfo, String msg);

    void notifyAboutUplink(SessionInfoProto sessionInfo, TransportProtos.UplinkNotificationMsg build, TransportServiceCallback<Void> empty);

    ExecutorService getCallbackExecutor();

    boolean hasSession(SessionInfoProto sessionInfo);

    void createGaugeStats(String openConnections, AtomicInteger connectionsCounter);
}

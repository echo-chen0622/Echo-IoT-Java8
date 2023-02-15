package org.thingsboard.server.transport.lwm2m.server.uplink;

import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.CreateRequest;
import org.eclipse.leshan.core.request.SendRequest;
import org.eclipse.leshan.core.request.WriteCompositeRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.response.ReadCompositeResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.server.registration.Registration;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;

import java.util.Collection;
import java.util.Optional;

public interface LwM2mUplinkMsgHandler {

    void onRegistered(Registration registration, Collection<Observation> previousObsersations);

    void updatedReg(Registration registration);

    void unReg(Registration registration, Collection<Observation> observations);

    void onSleepingDev(Registration registration);

    void onUpdateValueAfterReadResponse(Registration registration, String path, ReadResponse response);

    void onUpdateValueAfterReadCompositeResponse(Registration registration, ReadCompositeResponse response);

    void onUpdateValueWithSendRequest(Registration registration, SendRequest sendRequest);

    void onDeviceProfileUpdate(TransportProtos.SessionInfoProto sessionInfo, DeviceProfile deviceProfile);

    void onDeviceUpdate(TransportProtos.SessionInfoProto sessionInfo, Device device, Optional<DeviceProfile> deviceProfileOpt);

    void onDeviceDelete(DeviceId deviceId);

    void onResourceUpdate(TransportProtos.ResourceUpdateMsg resourceUpdateMsgOpt);

    void onResourceDelete(TransportProtos.ResourceDeleteMsg resourceDeleteMsgOpt);

    void onAwakeDev(Registration registration);

    void onWriteResponseOk(LwM2mClient client, String path, WriteRequest request, int code);

    void onCreateResponseOk(LwM2mClient client, String path, CreateRequest request);

    void onWriteCompositeResponseOk(LwM2mClient client, WriteCompositeRequest request, int code);

    void onToTransportUpdateCredentials(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.ToTransportUpdateCredentialsProto updateCredentials);

    LwM2MTransportServerConfig getConfig();
}

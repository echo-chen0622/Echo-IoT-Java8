package org.echoiot.server.transport.lwm2m.server.rpc;

import org.echoiot.server.gen.transport.TransportProtos;

public interface LwM2MRpcRequestHandler {

    void onToDeviceRpcRequest(TransportProtos.ToDeviceRpcRequestMsg toDeviceRequest, TransportProtos.SessionInfoProto sessionInfo);

    void onToDeviceRpcResponse(TransportProtos.ToDeviceRpcResponseMsg toDeviceRpcResponse, TransportProtos.SessionInfoProto sessionInfo);

    void onToServerRpcResponse(TransportProtos.ToServerRpcResponseMsg toServerResponse);


}
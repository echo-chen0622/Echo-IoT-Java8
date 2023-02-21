package org.echoiot.server.transport.coap.adaptors;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.transport.adaptor.AdaptorException;
import org.echoiot.server.common.transport.adaptor.JsonConverter;
import org.echoiot.server.common.transport.adaptor.ProtoConverter;
import org.echoiot.server.gen.transport.TransportProtos;
import org.echoiot.server.transport.coap.CoapTransportResource;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
public class ProtoCoapAdaptor implements CoapTransportAdaptor {

    @Override
    public TransportProtos.PostTelemetryMsg convertToPostTelemetry(UUID sessionId, @NotNull Request inbound, Descriptors.Descriptor telemetryMsgDescriptor) throws AdaptorException {
        ProtoConverter.validateDescriptor(telemetryMsgDescriptor);
        try {
            return JsonConverter.convertToTelemetryProto(new JsonParser().parse(ProtoConverter.dynamicMsgToJson(inbound.getPayload(), telemetryMsgDescriptor)));
        } catch (Exception e) {
            throw new AdaptorException(e);
        }
    }

    @Override
    public TransportProtos.PostAttributeMsg convertToPostAttributes(UUID sessionId, @NotNull Request inbound, Descriptors.Descriptor attributesMsgDescriptor) throws AdaptorException {
        ProtoConverter.validateDescriptor(attributesMsgDescriptor);
        try {
            return JsonConverter.convertToAttributesProto(new JsonParser().parse(ProtoConverter.dynamicMsgToJson(inbound.getPayload(), attributesMsgDescriptor)));
        } catch (Exception e) {
            throw new AdaptorException(e);
        }
    }

    @Override
    public TransportProtos.GetAttributeRequestMsg convertToGetAttributes(UUID sessionId, @NotNull Request inbound) throws AdaptorException {
        return CoapAdaptorUtils.toGetAttributeRequestMsg(inbound);
    }

    @Override
    public TransportProtos.ToDeviceRpcResponseMsg convertToDeviceRpcResponse(UUID sessionId, @NotNull Request inbound, Descriptors.Descriptor rpcResponseMsgDescriptor) throws AdaptorException {
        Optional<Integer> requestId = CoapTransportResource.getRequestId(inbound);
        if (requestId.isEmpty()) {
            throw new AdaptorException("Request id is missing!");
        } else {
            ProtoConverter.validateDescriptor(rpcResponseMsgDescriptor);
            try {
                JsonElement response = new JsonParser().parse(ProtoConverter.dynamicMsgToJson(inbound.getPayload(), rpcResponseMsgDescriptor));
                return TransportProtos.ToDeviceRpcResponseMsg.newBuilder().setRequestId(requestId.orElseThrow(() -> new AdaptorException("Request id is missing!")))
                        .setPayload(response.toString()).build();
            } catch (Exception e) {
                throw new AdaptorException(e);
            }
        }
    }

    @Override
    public TransportProtos.ToServerRpcRequestMsg convertToServerRpcRequest(UUID sessionId, @NotNull Request inbound) throws AdaptorException {
        try {
            return ProtoConverter.convertToServerRpcRequest(inbound.getPayload(), 0);
        } catch (InvalidProtocolBufferException ex) {
            throw new AdaptorException(ex);
        }
    }

    @Override
    public TransportProtos.ClaimDeviceMsg convertToClaimDevice(UUID sessionId, @NotNull Request inbound, @NotNull TransportProtos.SessionInfoProto sessionInfo) throws AdaptorException {
        @NotNull DeviceId deviceId = new DeviceId(new UUID(sessionInfo.getDeviceIdMSB(), sessionInfo.getDeviceIdLSB()));
        try {
            return ProtoConverter.convertToClaimDeviceProto(deviceId, inbound.getPayload());
        } catch (InvalidProtocolBufferException ex) {
            throw new AdaptorException(ex);
        }
    }

    @Override
    public TransportProtos.ProvisionDeviceRequestMsg convertToProvisionRequestMsg(UUID sessionId, @NotNull Request inbound) throws AdaptorException {
        try {
            return ProtoConverter.convertToProvisionRequestMsg(inbound.getPayload());
        } catch (InvalidProtocolBufferException ex) {
            throw new AdaptorException(ex);
        }
    }

    @NotNull
    @Override
    public Response convertToPublish(@NotNull TransportProtos.AttributeUpdateNotificationMsg msg) throws AdaptorException {
        return getObserveNotification(msg.toByteArray());
    }

    @NotNull
    @Override
    public Response convertToPublish(@NotNull TransportProtos.ToDeviceRpcRequestMsg rpcRequest, DynamicMessage.Builder rpcRequestDynamicMessageBuilder) throws AdaptorException {
        return getObserveNotification(ProtoConverter.convertToRpcRequest(rpcRequest, rpcRequestDynamicMessageBuilder));
    }

    @NotNull
    @Override
    public Response convertToPublish(@NotNull TransportProtos.ToServerRpcResponseMsg msg) throws AdaptorException {
        @NotNull Response response = new Response(CoAP.ResponseCode.CONTENT);
        response.setPayload(msg.toByteArray());
        return response;
    }

    @NotNull
    @Override
    public Response convertToPublish(@NotNull TransportProtos.GetAttributeResponseMsg msg) throws AdaptorException {
        if (msg.getSharedStateMsg()) {
            if (StringUtils.isEmpty(msg.getError())) {
                @NotNull Response response = new Response(CoAP.ResponseCode.CONTENT);
                TransportProtos.AttributeUpdateNotificationMsg notificationMsg = TransportProtos.AttributeUpdateNotificationMsg.newBuilder().addAllSharedUpdated(msg.getSharedAttributeListList()).build();
                response.setPayload(notificationMsg.toByteArray());
                return response;
            } else {
                return new Response(CoAP.ResponseCode.INTERNAL_SERVER_ERROR);
            }
        } else {
            if (msg.getClientAttributeListCount() == 0 && msg.getSharedAttributeListCount() == 0) {
                return new Response(CoAP.ResponseCode.NOT_FOUND);
            } else {
                @NotNull Response response = new Response(CoAP.ResponseCode.CONTENT);
                response.setPayload(msg.toByteArray());
                return response;
            }
        }
    }

    @NotNull
    private Response getObserveNotification(byte[] notification) {
        @NotNull Response response = new Response(CoAP.ResponseCode.CONTENT);
        response.setPayload(notification);
        return response;
    }

    @Override
    public int getContentFormat() {
        return MediaTypeRegistry.APPLICATION_OCTET_STREAM;
    }
}

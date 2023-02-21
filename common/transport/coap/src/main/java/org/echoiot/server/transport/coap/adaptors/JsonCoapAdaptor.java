package org.echoiot.server.transport.coap.adaptors;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.transport.adaptor.AdaptorException;
import org.echoiot.server.common.transport.adaptor.JsonConverter;
import org.echoiot.server.gen.transport.TransportProtos;
import org.echoiot.server.transport.coap.CoapTransportResource;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
public class JsonCoapAdaptor implements CoapTransportAdaptor {

    @Override
    public TransportProtos.PostTelemetryMsg convertToPostTelemetry(UUID sessionId, @NotNull Request inbound, Descriptors.Descriptor telemetryMsgDescriptor) throws AdaptorException {
        @Nullable String payload = validatePayload(sessionId, inbound, false);
        try {
            return JsonConverter.convertToTelemetryProto(new JsonParser().parse(payload));
        } catch (IllegalStateException | JsonSyntaxException ex) {
            throw new AdaptorException(ex);
        }
    }

    @Override
    public TransportProtos.PostAttributeMsg convertToPostAttributes(UUID sessionId, @NotNull Request inbound, Descriptors.Descriptor attributesMsgDescriptor) throws AdaptorException {
        @Nullable String payload = validatePayload(sessionId, inbound, false);
        try {
            return JsonConverter.convertToAttributesProto(new JsonParser().parse(payload));
        } catch (IllegalStateException | JsonSyntaxException ex) {
            throw new AdaptorException(ex);
        }
    }

    @Override
    public TransportProtos.GetAttributeRequestMsg convertToGetAttributes(UUID sessionId, @NotNull Request inbound) throws AdaptorException {
        return CoapAdaptorUtils.toGetAttributeRequestMsg(inbound);
    }

    @Override
    public TransportProtos.ToDeviceRpcResponseMsg convertToDeviceRpcResponse(UUID sessionId, @NotNull Request inbound, Descriptors.Descriptor rpcResponseMsgDescriptor) throws AdaptorException {
        Optional<Integer> requestId = CoapTransportResource.getRequestId(inbound);
        @Nullable String payload = validatePayload(sessionId, inbound, false);
        JsonObject response = new JsonParser().parse(payload).getAsJsonObject();
        return TransportProtos.ToDeviceRpcResponseMsg.newBuilder().setRequestId(requestId.orElseThrow(() -> new AdaptorException("Request id is missing!")))
                .setPayload(response.toString()).build();
    }

    @Override
    public TransportProtos.ToServerRpcRequestMsg convertToServerRpcRequest(UUID sessionId, @NotNull Request inbound) throws AdaptorException {
        @Nullable String payload = validatePayload(sessionId, inbound, false);
        return JsonConverter.convertToServerRpcRequest(new JsonParser().parse(payload), 0);
    }

    @Override
    public TransportProtos.ClaimDeviceMsg convertToClaimDevice(UUID sessionId, @NotNull Request inbound, @NotNull TransportProtos.SessionInfoProto sessionInfo) throws AdaptorException {
        @NotNull DeviceId deviceId = new DeviceId(new UUID(sessionInfo.getDeviceIdMSB(), sessionInfo.getDeviceIdLSB()));
        @Nullable String payload = validatePayload(sessionId, inbound, true);
        try {
            return JsonConverter.convertToClaimDeviceProto(deviceId, payload);
        } catch (IllegalStateException | JsonSyntaxException ex) {
            throw new AdaptorException(ex);
        }
    }

    @NotNull
    @Override
    public Response convertToPublish(@NotNull TransportProtos.AttributeUpdateNotificationMsg msg) throws AdaptorException {
        return getObserveNotification(JsonConverter.toJson(msg));
    }

    @NotNull
    @Override
    public Response convertToPublish(TransportProtos.ToDeviceRpcRequestMsg msg, DynamicMessage.Builder rpcRequestDynamicMessageBuilder) throws AdaptorException {
        return getObserveNotification(JsonConverter.toJson(msg, true));
    }

    @NotNull
    @Override
    public Response convertToPublish(@NotNull TransportProtos.ToServerRpcResponseMsg msg) throws AdaptorException {
        @NotNull Response response = new Response(CoAP.ResponseCode.CONTENT);
        JsonElement result = JsonConverter.toJson(msg);
        response.setPayload(result.toString());
        return response;
    }

    @Override
    public TransportProtos.ProvisionDeviceRequestMsg convertToProvisionRequestMsg(UUID sessionId, @NotNull Request inbound) throws AdaptorException {
        @Nullable String payload = validatePayload(sessionId, inbound, false);
        try {
            return JsonConverter.convertToProvisionRequestMsg(payload);
        } catch (IllegalStateException | JsonSyntaxException ex) {
            throw new AdaptorException(ex);
        }
    }

    @NotNull
    @Override
    public Response convertToPublish(@NotNull TransportProtos.GetAttributeResponseMsg msg) throws AdaptorException {
        if (msg.getSharedStateMsg()) {
            if (StringUtils.isEmpty(msg.getError())) {
                @NotNull Response response = new Response(CoAP.ResponseCode.CONTENT);
                TransportProtos.AttributeUpdateNotificationMsg notificationMsg = TransportProtos.AttributeUpdateNotificationMsg.newBuilder().addAllSharedUpdated(msg.getSharedAttributeListList()).build();
                @NotNull JsonObject result = JsonConverter.toJson(notificationMsg);
                response.setPayload(result.toString());
                return response;
            } else {
                return new Response(CoAP.ResponseCode.INTERNAL_SERVER_ERROR);
            }
        } else {
            if (msg.getClientAttributeListCount() == 0 && msg.getSharedAttributeListCount() == 0) {
                return new Response(CoAP.ResponseCode.NOT_FOUND);
            } else {
                @NotNull Response response = new Response(CoAP.ResponseCode.CONTENT);
                @NotNull JsonObject result = JsonConverter.toJson(msg);
                response.setPayload(result.toString());
                return response;
            }
        }
    }

    @NotNull
    private Response getObserveNotification(@NotNull JsonElement json) {
        @NotNull Response response = new Response(CoAP.ResponseCode.CONTENT);
        response.setPayload(json.toString());
        return response;
    }

    @Nullable
    private String validatePayload(UUID sessionId, @NotNull Request inbound, boolean isEmptyPayloadAllowed) throws AdaptorException {
        String payload = inbound.getPayloadString();
        if (payload == null) {
            log.debug("[{}] Payload is empty!", sessionId);
            if (!isEmptyPayloadAllowed) {
                throw new AdaptorException(new IllegalArgumentException("Payload is empty!"));
            }
        }
        return payload;
    }

    @Override
    public int getContentFormat() {
        return MediaTypeRegistry.APPLICATION_JSON;
    }

}

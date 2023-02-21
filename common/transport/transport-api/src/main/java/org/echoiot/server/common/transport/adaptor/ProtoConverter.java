package org.echoiot.server.common.transport.adaptor;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.DataConstants;
import org.echoiot.server.common.data.DynamicProtoUtils;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.gen.transport.TransportApiProtos;
import org.echoiot.server.gen.transport.TransportProtos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class ProtoConverter {

    public static final Gson GSON = new Gson();
    public static final JsonParser JSON_PARSER = new JsonParser();

    public static TransportProtos.PostTelemetryMsg convertToTelemetryProto(byte[] payload) throws InvalidProtocolBufferException, IllegalArgumentException {
        TransportProtos.TsKvListProto protoPayload = TransportProtos.TsKvListProto.parseFrom(payload);
        TransportProtos.PostTelemetryMsg.Builder postTelemetryMsgBuilder = TransportProtos.PostTelemetryMsg.newBuilder();
        TransportProtos.TsKvListProto tsKvListProto = validateTsKvListProto(protoPayload);
        postTelemetryMsgBuilder.addTsKvList(tsKvListProto);
        return postTelemetryMsgBuilder.build();
    }

    public static TransportProtos.PostTelemetryMsg validatePostTelemetryMsg(byte[] payload) throws InvalidProtocolBufferException, IllegalArgumentException {
        TransportProtos.PostTelemetryMsg msg = TransportProtos.PostTelemetryMsg.parseFrom(payload);
        TransportProtos.PostTelemetryMsg.Builder postTelemetryMsgBuilder = TransportProtos.PostTelemetryMsg.newBuilder();
        List<TransportProtos.TsKvListProto> tsKvListProtoList = msg.getTsKvListList();
        if (!CollectionUtils.isEmpty(tsKvListProtoList)) {
            @NotNull List<TransportProtos.TsKvListProto> tsKvListProtos = new ArrayList<>();
            tsKvListProtoList.forEach(tsKvListProto -> {
                TransportProtos.TsKvListProto transportTsKvListProto = validateTsKvListProto(tsKvListProto);
                tsKvListProtos.add(transportTsKvListProto);
            });
            postTelemetryMsgBuilder.addAllTsKvList(tsKvListProtos);
            return postTelemetryMsgBuilder.build();
        } else {
            throw new IllegalArgumentException("TsKv list is empty!");
        }
    }

    public static TransportProtos.PostAttributeMsg validatePostAttributeMsg(byte[] bytes) throws IllegalArgumentException, InvalidProtocolBufferException {
        TransportProtos.PostAttributeMsg proto = TransportProtos.PostAttributeMsg.parseFrom(bytes);
        List<TransportProtos.KeyValueProto> kvList = proto.getKvList();
        if (!CollectionUtils.isEmpty(kvList)) {
            @NotNull List<TransportProtos.KeyValueProto> keyValueProtos = validateKeyValueProtos(kvList);
            TransportProtos.PostAttributeMsg.Builder result = TransportProtos.PostAttributeMsg.newBuilder();
            result.addAllKv(keyValueProtos);
            return result.build();
        } else {
            throw new IllegalArgumentException("KeyValue list is empty!");
        }
    }

    public static TransportProtos.ClaimDeviceMsg convertToClaimDeviceProto(@NotNull DeviceId deviceId, @Nullable byte[] bytes) throws InvalidProtocolBufferException {
        if (bytes == null) {
            return buildClaimDeviceMsg(deviceId, DataConstants.DEFAULT_SECRET_KEY, 0);
        }
        TransportApiProtos.ClaimDevice proto = TransportApiProtos.ClaimDevice.parseFrom(bytes);
        @NotNull String secretKey = proto.getSecretKey() != null ? proto.getSecretKey() : DataConstants.DEFAULT_SECRET_KEY;
        long durationMs = proto.getDurationMs();
        return buildClaimDeviceMsg(deviceId, secretKey, durationMs);
    }

    public static TransportProtos.GetAttributeRequestMsg convertToGetAttributeRequestMessage(byte[] bytes, int requestId) throws InvalidProtocolBufferException, RuntimeException {
        TransportApiProtos.AttributesRequest proto = TransportApiProtos.AttributesRequest.parseFrom(bytes);
        TransportProtos.GetAttributeRequestMsg.Builder result = TransportProtos.GetAttributeRequestMsg.newBuilder();
        result.setRequestId(requestId);
        @NotNull String clientKeys = proto.getClientKeys();
        @NotNull String sharedKeys = proto.getSharedKeys();
        if (!StringUtils.isEmpty(clientKeys)) {
            @NotNull List<String> clientKeysList = Arrays.asList(clientKeys.split(","));
            result.addAllClientAttributeNames(clientKeysList);
        }
        if (!StringUtils.isEmpty(sharedKeys)) {
            @NotNull List<String> sharedKeysList = Arrays.asList(sharedKeys.split(","));
            result.addAllSharedAttributeNames(sharedKeysList);
        }
        return result.build();
    }

    public static TransportProtos.ToServerRpcRequestMsg convertToServerRpcRequest(byte[] bytes, int requestId) throws InvalidProtocolBufferException {
        TransportApiProtos.RpcRequest proto = TransportApiProtos.RpcRequest.parseFrom(bytes);
        @NotNull String method = proto.getMethod();
        @NotNull String params = proto.getParams();
        return TransportProtos.ToServerRpcRequestMsg.newBuilder().setRequestId(requestId).setMethodName(method).setParams(params).build();
    }

    private static TransportProtos.ClaimDeviceMsg buildClaimDeviceMsg(@NotNull DeviceId deviceId, String secretKey, long durationMs) {
        TransportProtos.ClaimDeviceMsg.Builder result = TransportProtos.ClaimDeviceMsg.newBuilder();
        return result
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .setSecretKey(secretKey)
                .setDurationMs(durationMs)
                .build();
    }

    private static TransportProtos.TsKvListProto validateTsKvListProto(@NotNull TransportProtos.TsKvListProto tsKvListProto) {
        TransportProtos.TsKvListProto.Builder tsKvListBuilder = TransportProtos.TsKvListProto.newBuilder();
        long ts = tsKvListProto.getTs();
        if (ts == 0) {
            ts = System.currentTimeMillis();
        }
        tsKvListBuilder.setTs(ts);
        List<TransportProtos.KeyValueProto> kvList = tsKvListProto.getKvList();
        if (!CollectionUtils.isEmpty(kvList)) {
            @NotNull List<TransportProtos.KeyValueProto> keyValueListProtos = validateKeyValueProtos(kvList);
            tsKvListBuilder.addAllKv(keyValueListProtos);
            return tsKvListBuilder.build();
        } else {
            throw new IllegalArgumentException("KeyValue list is empty!");
        }
    }

    public static TransportProtos.ProvisionDeviceRequestMsg convertToProvisionRequestMsg(byte[] bytes) throws InvalidProtocolBufferException {
        return TransportProtos.ProvisionDeviceRequestMsg.parseFrom(bytes);
    }

    @NotNull
    private static List<TransportProtos.KeyValueProto> validateKeyValueProtos(@NotNull List<TransportProtos.KeyValueProto> kvList) {
        kvList.forEach(keyValueProto -> {
            String key = keyValueProto.getKey();
            if (StringUtils.isEmpty(key)) {
                throw new IllegalArgumentException("Invalid key value: " + key + "!");
            }
            TransportProtos.KeyValueType type = keyValueProto.getType();
            switch (type) {
                case BOOLEAN_V:
                case LONG_V:
                case DOUBLE_V:
                    break;
                case STRING_V:
                    if (StringUtils.isEmpty(keyValueProto.getStringV())) {
                        throw new IllegalArgumentException("Value is empty for key: " + key + "!");
                    }
                    break;
                case JSON_V:
                    try {
                        JSON_PARSER.parse(keyValueProto.getJsonV());
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Can't parse value: " + keyValueProto.getJsonV() + " for key: " + key + "!");
                    }
                    break;
                case UNRECOGNIZED:
                    throw new IllegalArgumentException("Unsupported keyValueType: " + type + "!");
            }
        });
        return kvList;
    }

    public static byte[] convertToRpcRequest(@NotNull TransportProtos.ToDeviceRpcRequestMsg toDeviceRpcRequestMsg, DynamicMessage.Builder rpcRequestDynamicMessageBuilder) throws AdaptorException {
        rpcRequestDynamicMessageBuilder = rpcRequestDynamicMessageBuilder.getDefaultInstanceForType().newBuilderForType();
        @NotNull JsonObject rpcRequestJson = new JsonObject();
        rpcRequestJson.addProperty("method", toDeviceRpcRequestMsg.getMethodName());
        rpcRequestJson.addProperty("requestId", toDeviceRpcRequestMsg.getRequestId());
        String params = toDeviceRpcRequestMsg.getParams();
        try {
            JsonElement paramsElement = JSON_PARSER.parse(params);
            rpcRequestJson.add("params", paramsElement);
            @NotNull DynamicMessage dynamicRpcRequest = DynamicProtoUtils.jsonToDynamicMessage(rpcRequestDynamicMessageBuilder, GSON.toJson(rpcRequestJson));
            return dynamicRpcRequest.toByteArray();
        } catch (Exception e) {
            throw new AdaptorException("Failed to convert ToDeviceRpcRequestMsg to Dynamic Rpc request message due to: ", e);
        }
    }

    @NotNull
    public static Descriptors.Descriptor validateDescriptor(@NotNull Descriptors.Descriptor descriptor) throws AdaptorException {
        if (descriptor == null) {
            throw new AdaptorException("Failed to get dynamic message descriptor!");
        }
        return descriptor;
    }

    public static String dynamicMsgToJson(byte[] bytes, @NotNull Descriptors.Descriptor descriptor) throws InvalidProtocolBufferException {
        return DynamicProtoUtils.dynamicMsgToJson(descriptor, bytes);
    }

}

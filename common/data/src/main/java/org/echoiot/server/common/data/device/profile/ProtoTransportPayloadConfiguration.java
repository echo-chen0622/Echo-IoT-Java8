package org.echoiot.server.common.data.device.profile;

import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.squareup.wire.schema.Location;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.DynamicProtoUtils;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.TransportPayloadType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Slf4j
@Data
public class ProtoTransportPayloadConfiguration implements TransportPayloadTypeConfiguration {

    public static final Location LOCATION = new Location("", "", -1, -1);
    public static final String ATTRIBUTES_PROTO_SCHEMA = "attributes proto schema";
    public static final String TELEMETRY_PROTO_SCHEMA = "telemetry proto schema";
    public static final String RPC_RESPONSE_PROTO_SCHEMA = "rpc response proto schema";
    public static final String RPC_REQUEST_PROTO_SCHEMA = "rpc request proto schema";
    private static final String PROTO_3_SYNTAX = "proto3";

    private String deviceTelemetryProtoSchema;
    private String deviceAttributesProtoSchema;
    private String deviceRpcRequestProtoSchema;
    private String deviceRpcResponseProtoSchema;

    private boolean enableCompatibilityWithJsonPayloadFormat;
    private boolean useJsonPayloadFormatForDefaultDownlinkTopics;

    @NotNull
    @Override
    public TransportPayloadType getTransportPayloadType() {
        return TransportPayloadType.PROTOBUF;
    }

    @Nullable
    public Descriptors.Descriptor getTelemetryDynamicMessageDescriptor(String deviceTelemetryProtoSchema) {
        return DynamicProtoUtils.getDescriptor(deviceTelemetryProtoSchema, TELEMETRY_PROTO_SCHEMA);
    }

    @Nullable
    public Descriptors.Descriptor getAttributesDynamicMessageDescriptor(String deviceAttributesProtoSchema) {
        return DynamicProtoUtils.getDescriptor(deviceAttributesProtoSchema, ATTRIBUTES_PROTO_SCHEMA);
    }

    @Nullable
    public Descriptors.Descriptor getRpcResponseDynamicMessageDescriptor(String deviceRpcResponseProtoSchema) {
        return DynamicProtoUtils.getDescriptor(deviceRpcResponseProtoSchema, RPC_RESPONSE_PROTO_SCHEMA);
    }

    public DynamicMessage.Builder getRpcRequestDynamicMessageBuilder(String deviceRpcRequestProtoSchema) {
        return DynamicProtoUtils.getDynamicMessageBuilder(deviceRpcRequestProtoSchema, RPC_REQUEST_PROTO_SCHEMA);
    }

    @NotNull
    public String getDeviceRpcResponseProtoSchema() {
        if (StringUtils.isNotEmpty(deviceRpcResponseProtoSchema)) {
            return deviceRpcResponseProtoSchema;
        } else {
            return "syntax =\"proto3\";\n" +
                    "package rpc;\n" +
                    "\n" +
                    "message RpcResponseMsg {\n" +
                    "  optional string payload = 1;\n" +
                    "}";
        }
    }

    @NotNull
    public String getDeviceRpcRequestProtoSchema() {
        if (StringUtils.isNotEmpty(deviceRpcRequestProtoSchema)) {
            return deviceRpcRequestProtoSchema;
        } else {
            return "syntax =\"proto3\";\n" +
                    "package rpc;\n" +
                    "\n" +
                    "message RpcRequestMsg {\n" +
                    "  optional string method = 1;\n" +
                    "  optional int32 requestId = 2;\n" +
                    "  optional string params = 3;\n" +
                    "}";
        }
    }

}

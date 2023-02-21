package org.echoiot.server.transport.mqtt.rpc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.os72.protobuf.dynamic.DynamicSchema;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.nimbusds.jose.util.StandardCharset;
import com.squareup.wire.schema.internal.parser.ProtoFileElement;
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.server.common.data.*;
import org.echoiot.server.common.data.device.profile.DeviceProfileTransportConfiguration;
import org.echoiot.server.common.data.device.profile.MqttDeviceProfileTransportConfiguration;
import org.echoiot.server.common.data.device.profile.ProtoTransportPayloadConfiguration;
import org.echoiot.server.common.data.device.profile.TransportPayloadTypeConfiguration;
import org.echoiot.server.gen.transport.TransportApiProtos;
import org.echoiot.server.transport.mqtt.AbstractMqttIntegrationTest;
import org.echoiot.server.transport.mqtt.MqttTestCallback;
import org.echoiot.server.transport.mqtt.MqttTestClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.echoiot.server.common.data.device.profile.MqttTopics.*;
import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
public abstract class AbstractMqttServerSideRpcIntegrationTest extends AbstractMqttIntegrationTest {

    protected static final  String RPC_REQUEST_PROTO_SCHEMA = "syntax =\"proto3\";\n" +
            "package rpc;\n" +
            "\n" +
            "message RpcRequestMsg {\n" +
            "  optional string method = 1;\n" +
            "  optional int32 requestId = 2;\n" +
            "  Params params = 3;\n" +
            "\n" +
            "  message Params {\n" +
            "      optional string pin = 1;\n" +
            "      optional int32 value = 2;\n" +
            "   }\n" +
            "}";

    private static final String DEVICE_RESPONSE = "{\"value1\":\"A\",\"value2\":\"B\"}";

    protected static final Long asyncContextTimeoutToUseRpcPlugin = 10000L;

    protected void processOneWayRpcTest(@NotNull String rpcSubTopic) throws Exception {
        @NotNull MqttTestClient client = new MqttTestClient();
        client.connectAndWait(accessToken);
        @NotNull MqttTestCallback callback = new MqttTestCallback(rpcSubTopic.replace("+", "0"));
        client.setCallback(callback);
        client.subscribeAndWait(rpcSubTopic, MqttQoS.AT_MOST_ONCE);

        @NotNull String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"23\",\"value\": 1}}";
        String result = doPostAsync("/api/rpc/oneway/" + savedDevice.getId(), setGpioRequest, String.class, status().isOk());
        assertTrue(StringUtils.isEmpty(result));
        callback.getSubscribeLatch().await(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        DeviceTransportType deviceTransportType = deviceProfile.getTransportType();
        if (deviceTransportType.equals(DeviceTransportType.MQTT)) {
            DeviceProfileTransportConfiguration transportConfiguration = deviceProfile.getProfileData().getTransportConfiguration();
            assertTrue(transportConfiguration instanceof MqttDeviceProfileTransportConfiguration);
            @NotNull MqttDeviceProfileTransportConfiguration configuration = (MqttDeviceProfileTransportConfiguration) transportConfiguration;
            TransportPayloadType transportPayloadType = configuration.getTransportPayloadTypeConfiguration().getTransportPayloadType();
            if (transportPayloadType.equals(TransportPayloadType.PROTOBUF)) {
                // TODO: add correct validation of proto requests to device
                assertTrue(callback.getPayloadBytes().length > 0);
            } else {
                assertEquals(JacksonUtil.toJsonNode(setGpioRequest), JacksonUtil.fromBytes(callback.getPayloadBytes()));
            }
        } else {
            assertEquals(JacksonUtil.toJsonNode(setGpioRequest), JacksonUtil.fromBytes(callback.getPayloadBytes()));
        }
        assertEquals(MqttQoS.AT_MOST_ONCE.value(), callback.getQoS());
        client.disconnect();
    }

    protected void processJsonOneWayRpcTestGateway(String deviceName) throws Exception {
        @NotNull MqttTestClient client = new MqttTestClient();
        client.connectAndWait(gatewayAccessToken);
        @NotNull String payload = "{\"device\":\"" + deviceName + "\"}";
        @NotNull byte[] payloadBytes = payload.getBytes();
        validateOneWayRpcGatewayResponse(deviceName, client, payloadBytes);
        client.disconnect();
    }

    protected void processJsonTwoWayRpcTest(@NotNull String rpcSubTopic) throws Exception {
        @NotNull MqttTestClient client = new MqttTestClient();
        client.connectAndWait(accessToken);
        client.subscribeAndWait(rpcSubTopic, MqttQoS.AT_LEAST_ONCE);
        @NotNull MqttTestRpcJsonCallback callback = new MqttTestRpcJsonCallback(client, rpcSubTopic.replace("+", "0"));
        client.setCallback(callback);
        @NotNull String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"26\",\"value\": 1}}";
        String actualRpcResponse = doPostAsync("/api/rpc/twoway/" + savedDevice.getId(), setGpioRequest, String.class, status().isOk());
        callback.getSubscribeLatch().await(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertEquals(JacksonUtil.toJsonNode(setGpioRequest), JacksonUtil.fromBytes(callback.getPayloadBytes()));
        assertEquals("{\"value1\":\"A\",\"value2\":\"B\"}", actualRpcResponse);
        client.disconnect();
    }

    protected void processProtoTwoWayRpcTest(@NotNull String rpcSubTopic) throws Exception {
        @NotNull MqttTestClient client = new MqttTestClient();
        client.connectAndWait(accessToken);
        client.subscribeAndWait(rpcSubTopic, MqttQoS.AT_LEAST_ONCE);

        @NotNull MqttTestRpcProtoCallback callback = new MqttTestRpcProtoCallback(client, rpcSubTopic.replace("+", "0"));
        client.setCallback(callback);

        @NotNull String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"26\",\"value\": 1}}";
        String deviceId = savedDevice.getId().getId().toString();

        String actualRpcResponse = doPostAsync("/api/rpc/twoway/" + deviceId, setGpioRequest, String.class, status().isOk());
        callback.getSubscribeLatch().await(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        // TODO: add correct validation of proto requests to device
        assertTrue(callback.getPayloadBytes().length > 0);
        assertEquals("{\"payload\":\"{\\\"value1\\\":\\\"A\\\",\\\"value2\\\":\\\"B\\\"}\"}", actualRpcResponse);
        client.disconnect();
    }

    protected void processProtoTwoWayRpcTestGateway(String deviceName) throws Exception {
        @NotNull MqttTestClient client = new MqttTestClient();
        client.connectAndWait(gatewayAccessToken);
        @NotNull TransportApiProtos.ConnectMsg connectMsgProto = getConnectProto(deviceName);
        byte[] payloadBytes = connectMsgProto.toByteArray();
        validateProtoTwoWayRpcGatewayResponse(deviceName, client, payloadBytes);
        client.disconnect();
    }

    protected void processProtoOneWayRpcTestGateway(String deviceName) throws Exception {
        @NotNull MqttTestClient client = new MqttTestClient();
        client.connectAndWait(gatewayAccessToken);
        @NotNull TransportApiProtos.ConnectMsg connectMsgProto = getConnectProto(deviceName);
        byte[] payloadBytes = connectMsgProto.toByteArray();
        validateOneWayRpcGatewayResponse(deviceName, client, payloadBytes);
        client.disconnect();
    }

    @NotNull
    private TransportApiProtos.ConnectMsg getConnectProto(String deviceName) {
        TransportApiProtos.ConnectMsg.Builder builder = TransportApiProtos.ConnectMsg.newBuilder();
        builder.setDeviceName(deviceName);
        builder.setDeviceType(TransportPayloadType.PROTOBUF.name());
        return builder.build();
    }

    protected void processSequenceTwoWayRpcTest() throws Exception {
        @NotNull List<String> expected = new ArrayList<>();
        @NotNull List<String> result = new ArrayList<>();

        String deviceId = savedDevice.getId().getId().toString();

        for (int i = 0; i < 10; i++) {
            ObjectNode request = JacksonUtil.newObjectNode();
            request.put("method", "test");
            request.put("params", i);
            expected.add(JacksonUtil.toString(request));
            request.put("persistent", true);
            doPostAsync("/api/rpc/twoway/" + deviceId, JacksonUtil.toString(request), String.class, status().isOk());
        }

        @NotNull MqttTestClient client = new MqttTestClient();
        client.connectAndWait(accessToken);
        client.enableManualAcks();
        @NotNull MqttTestSequenceCallback callback = new MqttTestSequenceCallback(client, 10, result);
        client.setCallback(callback);
        client.subscribeAndWait(DEVICE_RPC_REQUESTS_SUB_TOPIC, MqttQoS.AT_LEAST_ONCE);

        callback.getSubscribeLatch().await(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertEquals(expected, result);
    }

    protected void processJsonTwoWayRpcTestGateway(String deviceName) throws Exception {
        @NotNull MqttTestClient client = new MqttTestClient();
        client.connectAndWait(gatewayAccessToken);

        @NotNull String payload = "{\"device\":\"" + deviceName + "\"}";
        @NotNull byte[] payloadBytes = payload.getBytes();

        validateJsonTwoWayRpcGatewayResponse(deviceName, client, payloadBytes);
        client.disconnect();
    }

    protected void validateOneWayRpcGatewayResponse(String deviceName, @NotNull MqttTestClient client, byte[] connectPayloadBytes) throws Exception {
        client.publish(GATEWAY_CONNECT_TOPIC, connectPayloadBytes);
        Device savedDevice = doExecuteWithRetriesAndInterval(
                () -> getDeviceByName(deviceName),
                20,
                100
        );
        assertNotNull(savedDevice);

        @NotNull MqttTestCallback  callback = new MqttTestCallback(GATEWAY_RPC_TOPIC);
        client.setCallback(callback);
        client.subscribeAndWait(GATEWAY_RPC_TOPIC, MqttQoS.AT_MOST_ONCE);
        @NotNull String setGpioRequest = "{\"method\": \"toggle_gpio\", \"params\": {\"pin\":1}}";
        String deviceId = savedDevice.getId().getId().toString();
        String result = doPostAsync("/api/rpc/oneway/" + deviceId, setGpioRequest, String.class, status().isOk());
        assertTrue(StringUtils.isEmpty(result));
        callback.getSubscribeLatch().await(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        DeviceTransportType deviceTransportType = deviceProfile.getTransportType();
        if (deviceTransportType.equals(DeviceTransportType.MQTT)) {
            DeviceProfileTransportConfiguration transportConfiguration = deviceProfile.getProfileData().getTransportConfiguration();
            assertTrue(transportConfiguration instanceof MqttDeviceProfileTransportConfiguration);
            @NotNull MqttDeviceProfileTransportConfiguration configuration = (MqttDeviceProfileTransportConfiguration) transportConfiguration;
            TransportPayloadType transportPayloadType = configuration.getTransportPayloadTypeConfiguration().getTransportPayloadType();
            if (transportPayloadType.equals(TransportPayloadType.PROTOBUF)) {
                // TODO: add correct validation of proto requests to device
                assertTrue(callback.getPayloadBytes().length > 0);
            } else {
                @NotNull JsonNode expectedJsonRequestData = getExpectedGatewayJsonRequestData(deviceName, setGpioRequest);
                assertEquals(expectedJsonRequestData, JacksonUtil.fromBytes(callback.getPayloadBytes()));
            }
        } else {
            @NotNull JsonNode expectedJsonRequestData = getExpectedGatewayJsonRequestData(deviceName, setGpioRequest);
            assertEquals(expectedJsonRequestData, JacksonUtil.fromBytes(callback.getPayloadBytes()));
        }
        assertEquals(MqttQoS.AT_MOST_ONCE.value(), callback.getQoS());
    }

    @NotNull
    private JsonNode getExpectedGatewayJsonRequestData(String deviceName, String requestStr) {
        ObjectNode deviceData = (ObjectNode) JacksonUtil.toJsonNode(requestStr);
        deviceData.put("id", 0);
        ObjectNode expectedRequest = JacksonUtil.newObjectNode();
        expectedRequest.put("device", deviceName);
        expectedRequest.set("data", deviceData);
        return expectedRequest;
    }

    protected void validateJsonTwoWayRpcGatewayResponse(String deviceName, @NotNull MqttTestClient client, byte[] connectPayloadBytes) throws Exception {
        client.publish(GATEWAY_CONNECT_TOPIC, connectPayloadBytes);

        Device savedDevice = doExecuteWithRetriesAndInterval(
                () -> getDeviceByName(deviceName),
                20,
                100
        );
        assertNotNull(savedDevice);

        @NotNull MqttTestRpcJsonCallback callback = new MqttTestRpcJsonCallback(client, GATEWAY_RPC_TOPIC);
        client.setCallback(callback);
        client.subscribeAndWait(GATEWAY_RPC_TOPIC, MqttQoS.AT_MOST_ONCE);

        @NotNull String setGpioRequest = "{\"method\": \"toggle_gpio\", \"params\": {\"pin\":1}}";
        String deviceId = savedDevice.getId().getId().toString();
        String actualRpcResponse = doPostAsync("/api/rpc/twoway/" + deviceId, setGpioRequest, String.class, status().isOk());
        callback.getSubscribeLatch().await(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        log.warn("request payload: {}", JacksonUtil.fromBytes(callback.getPayloadBytes()));
        assertEquals("{\"success\":true}", actualRpcResponse);
        assertEquals(MqttQoS.AT_MOST_ONCE.value(), callback.getQoS());
    }

    protected void validateProtoTwoWayRpcGatewayResponse(String deviceName, @NotNull MqttTestClient client, byte[] connectPayloadBytes) throws Exception {
        client.publish(GATEWAY_CONNECT_TOPIC, connectPayloadBytes);

        Device savedDevice = doExecuteWithRetriesAndInterval(
                () -> getDeviceByName(deviceName),
                20,
                100
        );
        assertNotNull(savedDevice);

        @NotNull MqttTestRpcProtoCallback callback = new MqttTestRpcProtoCallback(client, GATEWAY_RPC_TOPIC);
        client.setCallback(callback);
        client.subscribeAndWait(GATEWAY_RPC_TOPIC, MqttQoS.AT_MOST_ONCE);

        @NotNull String setGpioRequest = "{\"method\": \"toggle_gpio\", \"params\": {\"pin\":1}}";
        String deviceId = savedDevice.getId().getId().toString();
        String actualRpcResponse = doPostAsync("/api/rpc/twoway/" + deviceId, setGpioRequest, String.class, status().isOk());
        callback.getSubscribeLatch().await(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertEquals("{\"success\":true}", actualRpcResponse);
        assertEquals(MqttQoS.AT_MOST_ONCE.value(), callback.getQoS());
    }

    private Device getDeviceByName(String deviceName) throws Exception {
        return doGet("/api/tenant/devices?deviceName=" + deviceName, Device.class);
    }

    protected byte[] processJsonMessageArrived(@NotNull String requestTopic, @NotNull MqttMessage mqttMessage) {
        if (requestTopic.startsWith(BASE_DEVICE_API_TOPIC) || requestTopic.startsWith(BASE_DEVICE_API_TOPIC_V2)) {
            return DEVICE_RESPONSE.getBytes(StandardCharset.UTF_8);
        } else {
            JsonNode requestMsgNode = JacksonUtil.toJsonNode(new String(mqttMessage.getPayload(), StandardCharset.UTF_8));
            String deviceName = requestMsgNode.get("device").asText();
            int requestId = requestMsgNode.get("data").get("id").asInt();
            @NotNull String response = "{\"device\": \"" + deviceName + "\", \"id\": " + requestId + ", \"data\": {\"success\": true}}";
            return response.getBytes(StandardCharset.UTF_8);
        }
    }

    protected class MqttTestRpcJsonCallback extends MqttTestCallback {

        private final MqttTestClient client;

        public MqttTestRpcJsonCallback(MqttTestClient client, String awaitSubTopic) {
            super(awaitSubTopic);
            this.client = client;
        }

        @Override
        protected void messageArrivedOnAwaitSubTopic(@NotNull String requestTopic, @NotNull MqttMessage mqttMessage) {
            log.warn("messageArrived on topic: {}, awaitSubTopic: {}", requestTopic, awaitSubTopic);
            if (awaitSubTopic.equals(requestTopic)) {
                qoS = mqttMessage.getQos();
                payloadBytes = mqttMessage.getPayload();
                String responseTopic;
                if (requestTopic.startsWith(BASE_DEVICE_API_TOPIC_V2)) {
                    responseTopic = requestTopic.replace("req", "res");
                } else {
                    responseTopic = requestTopic.replace("request", "response");
                }
                try {
                    client.publish(responseTopic, processJsonMessageArrived(requestTopic, mqttMessage));
                } catch (MqttException e) {
                    log.warn("Failed to publish response on topic: {} due to: ", responseTopic, e);
                }
                subscribeLatch.countDown();
            }
        }
    }

    protected class MqttTestRpcProtoCallback extends MqttTestCallback {

        private final MqttTestClient client;

        public MqttTestRpcProtoCallback(MqttTestClient client, String awaitSubTopic) {
            super(awaitSubTopic);
            this.client = client;
        }

        @Override
        protected void messageArrivedOnAwaitSubTopic(@NotNull String requestTopic, @NotNull MqttMessage mqttMessage) {
            log.warn("messageArrived on topic: {}, awaitSubTopic: {}", requestTopic, awaitSubTopic);
            if (awaitSubTopic.equals(requestTopic)) {
                qoS = mqttMessage.getQos();
                payloadBytes = mqttMessage.getPayload();
                String responseTopic;
                if (requestTopic.startsWith(BASE_DEVICE_API_TOPIC_V2)) {
                    responseTopic = requestTopic.replace("req", "res");
                } else {
                    responseTopic = requestTopic.replace("request", "response");
                }
                try {
                    client.publish(responseTopic, processProtoMessageArrived(requestTopic, mqttMessage));
                } catch (Exception e) {
                    log.warn("Failed to publish response on topic: {} due to: ", responseTopic, e);
                }
                subscribeLatch.countDown();
            }
        }
    }

    protected byte[] processProtoMessageArrived(@NotNull String requestTopic, @NotNull MqttMessage mqttMessage) throws MqttException, InvalidProtocolBufferException {
        if (requestTopic.startsWith(BASE_DEVICE_API_TOPIC) || requestTopic.startsWith(BASE_DEVICE_API_TOPIC_V2)) {
            @NotNull ProtoTransportPayloadConfiguration protoTransportPayloadConfiguration = getProtoTransportPayloadConfiguration();
            @NotNull ProtoFileElement rpcRequestProtoFileElement = DynamicProtoUtils.getProtoFileElement(protoTransportPayloadConfiguration.getDeviceRpcRequestProtoSchema());
            DynamicSchema rpcRequestProtoSchema = DynamicProtoUtils.getDynamicSchema(rpcRequestProtoFileElement, ProtoTransportPayloadConfiguration.RPC_REQUEST_PROTO_SCHEMA);

            byte[] requestPayload = mqttMessage.getPayload();
            DynamicMessage.Builder rpcRequestMsg = rpcRequestProtoSchema.newMessageBuilder("RpcRequestMsg");
            Descriptors.Descriptor rpcRequestMsgDescriptor = rpcRequestMsg.getDescriptorForType();
            assertNotNull(rpcRequestMsgDescriptor);
            try {
                @NotNull DynamicMessage dynamicMessage = DynamicMessage.parseFrom(rpcRequestMsgDescriptor, requestPayload);
                @NotNull List<Descriptors.FieldDescriptor> fields = rpcRequestMsgDescriptor.getFields();
                for (@NotNull Descriptors.FieldDescriptor fieldDescriptor: fields) {
                    assertTrue(dynamicMessage.hasField(fieldDescriptor));
                }
                @NotNull ProtoFileElement rpcResponseProtoFileElement = DynamicProtoUtils.getProtoFileElement(protoTransportPayloadConfiguration.getDeviceRpcResponseProtoSchema());
                DynamicSchema rpcResponseProtoSchema = DynamicProtoUtils.getDynamicSchema(rpcResponseProtoFileElement, ProtoTransportPayloadConfiguration.RPC_RESPONSE_PROTO_SCHEMA);

                DynamicMessage.Builder rpcResponseBuilder = rpcResponseProtoSchema.newMessageBuilder("RpcResponseMsg");
                Descriptors.Descriptor rpcResponseMsgDescriptor = rpcResponseBuilder.getDescriptorForType();
                assertNotNull(rpcResponseMsgDescriptor);
                @NotNull DynamicMessage rpcResponseMsg = rpcResponseBuilder
                        .setField(rpcResponseMsgDescriptor.findFieldByName("payload"), DEVICE_RESPONSE)
                        .build();
                return rpcResponseMsg.toByteArray();
            } catch (InvalidProtocolBufferException e) {
                throw new RuntimeException("Command Response Ack Error, Invalid response received: ", e);
            }
        } else {
            TransportApiProtos.GatewayDeviceRpcRequestMsg msg = TransportApiProtos.GatewayDeviceRpcRequestMsg.parseFrom(mqttMessage.getPayload());
            @NotNull String deviceName = msg.getDeviceName();
            int requestId = msg.getRpcRequestMsg().getRequestId();
            @NotNull TransportApiProtos.GatewayRpcResponseMsg gatewayRpcResponseMsg = TransportApiProtos.GatewayRpcResponseMsg.newBuilder()
                                                                                                                              .setDeviceName(deviceName)
                                                                                                                              .setId(requestId)
                                                                                                                              .setData("{\"success\": true}")
                                                                                                                              .build();
            return gatewayRpcResponseMsg.toByteArray();
        }
    }

    @NotNull
    private ProtoTransportPayloadConfiguration getProtoTransportPayloadConfiguration() {
        DeviceProfileTransportConfiguration transportConfiguration = deviceProfile.getProfileData().getTransportConfiguration();
        assertTrue(transportConfiguration instanceof MqttDeviceProfileTransportConfiguration);
        @NotNull MqttDeviceProfileTransportConfiguration mqttTransportConfiguration = (MqttDeviceProfileTransportConfiguration) transportConfiguration;
        @NotNull TransportPayloadTypeConfiguration transportPayloadTypeConfiguration = mqttTransportConfiguration.getTransportPayloadTypeConfiguration();
        assertTrue(transportPayloadTypeConfiguration instanceof ProtoTransportPayloadConfiguration);
        return (ProtoTransportPayloadConfiguration) transportPayloadTypeConfiguration;
    }

    protected class MqttTestSequenceCallback extends MqttTestCallback {

        private final MqttTestClient client;
        private final List<String> expected;

        MqttTestSequenceCallback(MqttTestClient client, int subscribeCount, List<String> expected) {
            super(subscribeCount);
            this.client = client;
            this.expected = expected;
        }

        @Override
        public void messageArrived(@NotNull String requestTopic, @NotNull MqttMessage mqttMessage) {
            log.warn("messageArrived on topic: {}, awaitSubTopic: {}", requestTopic, awaitSubTopic);
            expected.add(new String(mqttMessage.getPayload()));
            @NotNull String responseTopic = requestTopic.replace("request", "response");
            qoS = mqttMessage.getQos();
            try {
                client.messageArrivedComplete(mqttMessage);
                client.publish(responseTopic, processJsonMessageArrived(requestTopic, mqttMessage));
            } catch (MqttException e) {
                log.warn("Failed to publish response on topic: {} due to: ", responseTopic, e);
            }
            subscribeLatch.countDown();
        }
    }
}

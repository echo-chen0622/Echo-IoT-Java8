package org.echoiot.server.transport.coap.attributes;

import com.github.os72.protobuf.dynamic.DynamicSchema;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.squareup.wire.schema.internal.parser.ProtoFileElement;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.server.common.data.DynamicProtoUtils;
import org.echoiot.server.common.data.device.profile.*;
import org.echoiot.server.common.data.query.EntityKey;
import org.echoiot.server.common.data.query.EntityKeyType;
import org.echoiot.server.common.data.query.SingleEntityFilter;
import org.echoiot.server.common.msg.session.FeatureType;
import org.echoiot.server.common.transport.service.DefaultTransportService;
import org.echoiot.server.gen.transport.TransportProtos;
import org.echoiot.server.transport.coap.AbstractCoapIntegrationTest;
import org.echoiot.server.transport.coap.CoapTestCallback;
import org.echoiot.server.transport.coap.CoapTestClient;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.echoiot.server.common.data.query.EntityKeyType.CLIENT_ATTRIBUTE;
import static org.echoiot.server.common.data.query.EntityKeyType.SHARED_ATTRIBUTE;
import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
public abstract class AbstractCoapAttributesIntegrationTest extends AbstractCoapIntegrationTest {

    @Resource
    DefaultTransportService defaultTransportService;

    public static final String ATTRIBUTES_SCHEMA_STR = "syntax =\"proto3\";\n" +
            "\n" +
            "package test;\n" +
            "\n" +
            "message PostAttributes {\n" +
            "  string clientStr = 1;\n" +
            "  bool clientBool = 2;\n" +
            "  double clientDbl = 3;\n" +
            "  int32 clientLong = 4;\n" +
            "  JsonObject clientJson = 5;\n" +
            "\n" +
            "  message JsonObject {\n" +
            "    int32 someNumber = 6;\n" +
            "    repeated int32 someArray = 7;\n" +
            "    NestedJsonObject someNestedObject = 8;\n" +
            "    message NestedJsonObject {\n" +
            "       string key = 9;\n" +
            "    }\n" +
            "  }\n" +
            "}";

    private static final String CLIENT_ATTRIBUTES_PAYLOAD = "{\"clientStr\":\"value1\",\"clientBool\":true,\"clientDbl\":42.0,\"clientLong\":73," +
            "\"clientJson\":{\"someNumber\":42,\"someArray\":[1,2,3],\"someNestedObject\":{\"key\":\"value\"}}}";

    private static final String SHARED_ATTRIBUTES_PAYLOAD = "{\"sharedStr\":\"value1\",\"sharedBool\":true,\"sharedDbl\":42.0,\"sharedLong\":73," +
            "\"sharedJson\":{\"someNumber\":42,\"someArray\":[1,2,3],\"someNestedObject\":{\"key\":\"value\"}}}";

    protected static final String SHARED_ATTRIBUTES_PAYLOAD_ON_CURRENT_STATE_NOTIFICATION = "{\"sharedStr\":\"value\",\"sharedBool\":false,\"sharedDbl\":41.0,\"sharedLong\":72," +
            "\"sharedJson\":{\"someNumber\":41,\"someArray\":[],\"someNestedObject\":{\"key\":\"value\"}}}";

    private static final String SHARED_ATTRIBUTES_DELETED_RESPONSE = "{\"deleted\":[\"sharedJson\"]}";

    @NotNull
    private List<TransportProtos.TsKvProto> getTsKvProtoList(String attributePrefix) {
        TransportProtos.TsKvProto tsKvProtoAttribute1 = getTsKvProto(attributePrefix + "Str", "value1", TransportProtos.KeyValueType.STRING_V);
        TransportProtos.TsKvProto tsKvProtoAttribute2 = getTsKvProto(attributePrefix + "Bool", "true", TransportProtos.KeyValueType.BOOLEAN_V);
        TransportProtos.TsKvProto tsKvProtoAttribute3 = getTsKvProto(attributePrefix + "Dbl", "42.0", TransportProtos.KeyValueType.DOUBLE_V);
        TransportProtos.TsKvProto tsKvProtoAttribute4 = getTsKvProto(attributePrefix + "Long", "73", TransportProtos.KeyValueType.LONG_V);
        TransportProtos.TsKvProto tsKvProtoAttribute5 = getTsKvProto(attributePrefix + "Json", "{\"someNumber\":42,\"someArray\":[1,2,3],\"someNestedObject\":{\"key\":\"value\"}}", TransportProtos.KeyValueType.JSON_V);
        @NotNull List<TransportProtos.TsKvProto> tsKvProtoList = new ArrayList<>();
        tsKvProtoList.add(tsKvProtoAttribute1);
        tsKvProtoList.add(tsKvProtoAttribute2);
        tsKvProtoList.add(tsKvProtoAttribute3);
        tsKvProtoList.add(tsKvProtoAttribute4);
        tsKvProtoList.add(tsKvProtoAttribute5);
        return tsKvProtoList;
    }

    protected TransportProtos.TsKvProto getTsKvProto(String key, String value, TransportProtos.KeyValueType keyValueType) {
        TransportProtos.TsKvProto.Builder tsKvProtoBuilder = TransportProtos.TsKvProto.newBuilder();
        TransportProtos.KeyValueProto keyValueProto = getKeyValueProto(key, value, keyValueType);
        tsKvProtoBuilder.setKv(keyValueProto);
        return tsKvProtoBuilder.build();
    }

    @NotNull
    private List<EntityKey> getEntityKeys(@NotNull List<String> keys, EntityKeyType scope) {
        return keys.stream().map(key -> new EntityKey(scope, key)).collect(Collectors.toList());
    }

    private byte[] getAttributesProtoPayloadBytes() {
        DeviceProfileTransportConfiguration transportConfiguration = deviceProfile.getProfileData().getTransportConfiguration();
        assertTrue(transportConfiguration instanceof CoapDeviceProfileTransportConfiguration);
        @NotNull CoapDeviceProfileTransportConfiguration coapTransportConfiguration = (CoapDeviceProfileTransportConfiguration) transportConfiguration;
        @NotNull CoapDeviceTypeConfiguration coapDeviceTypeConfiguration = coapTransportConfiguration.getCoapDeviceTypeConfiguration();
        assertTrue(coapDeviceTypeConfiguration instanceof DefaultCoapDeviceTypeConfiguration);
        @NotNull DefaultCoapDeviceTypeConfiguration defaultCoapDeviceTypeConfiguration = (DefaultCoapDeviceTypeConfiguration) coapDeviceTypeConfiguration;
        @NotNull TransportPayloadTypeConfiguration transportPayloadTypeConfiguration = defaultCoapDeviceTypeConfiguration.getTransportPayloadTypeConfiguration();
        assertTrue(transportPayloadTypeConfiguration instanceof ProtoTransportPayloadConfiguration);
        @NotNull ProtoTransportPayloadConfiguration protoTransportPayloadConfiguration = (ProtoTransportPayloadConfiguration) transportPayloadTypeConfiguration;
        @NotNull ProtoFileElement protoFileElement = DynamicProtoUtils.getProtoFileElement(protoTransportPayloadConfiguration.getDeviceAttributesProtoSchema());
        DynamicSchema attributesSchema = DynamicProtoUtils.getDynamicSchema(protoFileElement, ProtoTransportPayloadConfiguration.ATTRIBUTES_PROTO_SCHEMA);

        DynamicMessage.Builder nestedJsonObjectBuilder = attributesSchema.newMessageBuilder("PostAttributes.JsonObject.NestedJsonObject");
        Descriptors.Descriptor nestedJsonObjectBuilderDescriptor = nestedJsonObjectBuilder.getDescriptorForType();
        assertNotNull(nestedJsonObjectBuilderDescriptor);
        @NotNull DynamicMessage nestedJsonObject = nestedJsonObjectBuilder.setField(nestedJsonObjectBuilderDescriptor.findFieldByName("key"), "value").build();

        DynamicMessage.Builder jsonObjectBuilder = attributesSchema.newMessageBuilder("PostAttributes.JsonObject");
        Descriptors.Descriptor jsonObjectBuilderDescriptor = jsonObjectBuilder.getDescriptorForType();
        assertNotNull(jsonObjectBuilderDescriptor);
        @NotNull DynamicMessage jsonObject = jsonObjectBuilder
                .setField(jsonObjectBuilderDescriptor.findFieldByName("someNumber"), 42)
                .addRepeatedField(jsonObjectBuilderDescriptor.findFieldByName("someArray"), 1)
                .addRepeatedField(jsonObjectBuilderDescriptor.findFieldByName("someArray"), 2)
                .addRepeatedField(jsonObjectBuilderDescriptor.findFieldByName("someArray"), 3)
                .setField(jsonObjectBuilderDescriptor.findFieldByName("someNestedObject"), nestedJsonObject)
                .build();

        DynamicMessage.Builder postAttributesBuilder = attributesSchema.newMessageBuilder("PostAttributes");
        Descriptors.Descriptor postAttributesMsgDescriptor = postAttributesBuilder.getDescriptorForType();
        assertNotNull(postAttributesMsgDescriptor);
        @NotNull DynamicMessage postAttributesMsg = postAttributesBuilder
                .setField(postAttributesMsgDescriptor.findFieldByName("clientStr"), "value1")
                .setField(postAttributesMsgDescriptor.findFieldByName("clientBool"), true)
                .setField(postAttributesMsgDescriptor.findFieldByName("clientDbl"), 42.0)
                .setField(postAttributesMsgDescriptor.findFieldByName("clientLong"), 73)
                .setField(postAttributesMsgDescriptor.findFieldByName("clientJson"), jsonObject)
                .build();
        return postAttributesMsg.toByteArray();
    }

    protected void processJsonTestRequestAttributesValuesFromTheServer() throws Exception {
        client = new CoapTestClient(accessToken, FeatureType.ATTRIBUTES);
        @NotNull SingleEntityFilter dtf = new SingleEntityFilter();
        dtf.setSingleEntity(savedDevice.getId());
        @NotNull String clientKeysStr = "clientStr,clientBool,clientDbl,clientLong,clientJson";
        @NotNull String sharedKeysStr = "sharedStr,sharedBool,sharedDbl,sharedLong,sharedJson";
        @NotNull List<String> clientKeysList = List.of(clientKeysStr.split(","));
        @NotNull List<String> sharedKeysList = List.of(sharedKeysStr.split(","));
        @NotNull List<EntityKey> csKeys = getEntityKeys(clientKeysList, CLIENT_ATTRIBUTE);
        @NotNull List<EntityKey> shKeys = getEntityKeys(sharedKeysList, SHARED_ATTRIBUTE);
        @NotNull List<EntityKey> keys = new ArrayList<>();
        keys.addAll(csKeys);
        keys.addAll(shKeys);
        getWsClient().subscribeLatestUpdate(keys, dtf);
        getWsClient().registerWaitForUpdate(2);

        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/attributes/SHARED_SCOPE",
                SHARED_ATTRIBUTES_PAYLOAD, String.class, status().isOk());

        CoapResponse coapResponse = client.postMethod(CLIENT_ATTRIBUTES_PAYLOAD);
        assertEquals(CoAP.ResponseCode.CREATED, coapResponse.getCode());

        String update = getWsClient().waitForUpdate();
        assertThat(update).as("ws update received").isNotBlank();

        @NotNull String featureTokenUrl = CoapTestClient.getFeatureTokenUrl(accessToken, FeatureType.ATTRIBUTES) + "?clientKeys=" + clientKeysStr + "&sharedKeys=" + sharedKeysStr;
        client.setURI(featureTokenUrl);
        validateJsonResponse(client.getMethod());
    }

    protected void processProtoTestRequestAttributesValuesFromTheServer() throws Exception {
        client = new CoapTestClient(accessToken, FeatureType.ATTRIBUTES);
        @NotNull SingleEntityFilter dtf = new SingleEntityFilter();
        dtf.setSingleEntity(savedDevice.getId());
        @NotNull String clientKeysStr = "clientStr,clientBool,clientDbl,clientLong,clientJson";
        @NotNull String sharedKeysStr = "sharedStr,sharedBool,sharedDbl,sharedLong,sharedJson";
        @NotNull List<String> clientKeysList = List.of(clientKeysStr.split(","));
        @NotNull List<String> sharedKeysList = List.of(sharedKeysStr.split(","));
        @NotNull List<EntityKey> csKeys = getEntityKeys(clientKeysList, CLIENT_ATTRIBUTE);
        @NotNull List<EntityKey> shKeys = getEntityKeys(sharedKeysList, SHARED_ATTRIBUTE);
        @NotNull List<EntityKey> keys = new ArrayList<>();
        keys.addAll(csKeys);
        keys.addAll(shKeys);
        getWsClient().subscribeLatestUpdate(keys, dtf);
        getWsClient().registerWaitForUpdate(2);

        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/attributes/SHARED_SCOPE",
                SHARED_ATTRIBUTES_PAYLOAD, String.class, status().isOk());

        CoapResponse coapResponse = client.postMethod(getAttributesProtoPayloadBytes());
        assertEquals(CoAP.ResponseCode.CREATED, coapResponse.getCode());

        String update = getWsClient().waitForUpdate();
        assertThat(update).as("ws update received").isNotBlank();

        @NotNull String featureTokenUrl = CoapTestClient.getFeatureTokenUrl(accessToken, FeatureType.ATTRIBUTES) + "?clientKeys=" + clientKeysStr + "&sharedKeys=" + sharedKeysStr;
        client.setURI(featureTokenUrl);
        validateProtoResponse(client.getMethod());
    }

    protected void processJsonTestSubscribeToAttributesUpdates(boolean emptyCurrentStateNotification) throws Exception {
        if (!emptyCurrentStateNotification) {
            doPostAsync("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/attributes/SHARED_SCOPE", SHARED_ATTRIBUTES_PAYLOAD_ON_CURRENT_STATE_NOTIFICATION, String.class, status().isOk());
        }

        client = new CoapTestClient(accessToken, FeatureType.ATTRIBUTES);
        @NotNull CoapTestCallback callbackCoap = new CoapTestCallback(1);

        CoapObserveRelation observeRelation = client.getObserveRelation(callbackCoap);
        @NotNull String awaitAlias = "await Json Test Subscribe To AttributesUpdates (client.getObserveRelation)";
        await(awaitAlias)
                .atMost(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .until(() -> CoAP.ResponseCode.CONTENT.equals(callbackCoap.getResponseCode()) &&
                        callbackCoap.getObserve() != null &&
                        0 == callbackCoap.getObserve().intValue());
        if (emptyCurrentStateNotification) {
            validateUpdateAttributesJsonResponse(callbackCoap, "{}");
        } else {
            validateUpdateAttributesJsonResponse(callbackCoap, SHARED_ATTRIBUTES_PAYLOAD_ON_CURRENT_STATE_NOTIFICATION);
        }

        int expectedObserveForAttributesUpdate = callbackCoap.getObserve().intValue() + 1;
        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/attributes/SHARED_SCOPE", SHARED_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        awaitAlias = "await Json Test Subscribe To AttributesUpdates (add attributes)";
        await(awaitAlias)
                .atMost(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .until(() -> CoAP.ResponseCode.CONTENT.equals(callbackCoap.getResponseCode()) &&
                        callbackCoap.getObserve() != null &&
                        expectedObserveForAttributesUpdate == callbackCoap.getObserve().intValue());
        validateUpdateAttributesJsonResponse(callbackCoap, SHARED_ATTRIBUTES_PAYLOAD);

        int expectedObserveForAttributesDelete = callbackCoap.getObserve().intValue() + 1;
        doDelete("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/SHARED_SCOPE?keys=sharedJson", String.class);
        awaitAlias = "await Json Test Subscribe To AttributesUpdates (deleted attributes)";
        await(awaitAlias)
                .atMost(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .until(() -> CoAP.ResponseCode.CONTENT.equals(callbackCoap.getResponseCode()) &&
                        callbackCoap.getObserve() != null &&
                        expectedObserveForAttributesDelete == callbackCoap.getObserve().intValue());
        validateUpdateAttributesJsonResponse(callbackCoap, SHARED_ATTRIBUTES_DELETED_RESPONSE);

        observeRelation.proactiveCancel();
        assertTrue(observeRelation.isCanceled());

        awaitClientAfterCancelObserve();
    }

    protected void processProtoTestSubscribeToAttributesUpdates(boolean emptyCurrentStateNotification) throws Exception {
        if (!emptyCurrentStateNotification) {
            doPostAsync("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/attributes/SHARED_SCOPE", SHARED_ATTRIBUTES_PAYLOAD_ON_CURRENT_STATE_NOTIFICATION, String.class, status().isOk());
        }

        client = new CoapTestClient(accessToken, FeatureType.ATTRIBUTES);
        @NotNull CoapTestCallback callbackCoap = new CoapTestCallback(1);

        @NotNull String awaitAlias = "await Proto Test Subscribe To Attributes Updates (add attributes)";
        CoapObserveRelation observeRelation = client.getObserveRelation(callbackCoap);
        await(awaitAlias)
                .atMost(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .until(() -> CoAP.ResponseCode.CONTENT.equals(callbackCoap.getResponseCode()) &&
                        callbackCoap.getObserve() != null &&
                        0 == callbackCoap.getObserve().intValue());

        if (emptyCurrentStateNotification) {
            validateEmptyCurrentStateAttributesProtoResponse(callbackCoap);
        } else {
            validateCurrentStateAttributesProtoResponse(callbackCoap);
        }

        int expectedObserveForAttributesUpdate = callbackCoap.getObserve().intValue() + 1;
        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/attributes/SHARED_SCOPE", SHARED_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        awaitAlias = "await Proto Test Subscribe To Attributes Updates (add attributes)";
        await(awaitAlias)
                .atMost(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .until(() -> CoAP.ResponseCode.CONTENT.equals(callbackCoap.getResponseCode()) &&
                        callbackCoap.getObserve() != null &&
                        expectedObserveForAttributesUpdate == callbackCoap.getObserve().intValue());
        validateUpdateProtoAttributesResponse(callbackCoap, expectedObserveForAttributesUpdate);

        int expectedObserveForAttributesDelete = callbackCoap.getObserve().intValue() + 1;
        doDelete("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/SHARED_SCOPE?keys=sharedJson", String.class);
        awaitAlias = "await Proto Test Subscribe To Attributes Updates (deleted attributes)";
        await(awaitAlias)
                .atMost(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .until(() -> CoAP.ResponseCode.CONTENT.equals(callbackCoap.getResponseCode()) &&
                        callbackCoap.getObserve() != null &&
                        expectedObserveForAttributesDelete == callbackCoap.getObserve().intValue());
        validateDeleteProtoAttributesResponse(callbackCoap, expectedObserveForAttributesDelete);

        observeRelation.proactiveCancel();
        assertTrue(observeRelation.isCanceled());

        awaitClientAfterCancelObserve();
    }

    protected void validateJsonResponse(@NotNull CoapResponse getAttributesResponse) throws InvalidProtocolBufferException {
        assertEquals(CoAP.ResponseCode.CONTENT, getAttributesResponse.getCode());
        @NotNull String expectedResponse = "{\"client\":" + CLIENT_ATTRIBUTES_PAYLOAD + ",\"shared\":" + SHARED_ATTRIBUTES_PAYLOAD + "}";
        assertEquals(JacksonUtil.toJsonNode(expectedResponse), JacksonUtil.fromBytes(getAttributesResponse.getPayload()));
    }

    protected void validateProtoResponse(@NotNull CoapResponse getAttributesResponse) throws InterruptedException, InvalidProtocolBufferException {
        TransportProtos.GetAttributeResponseMsg expectedAttributesResponse = getExpectedAttributeResponseMsg();
        TransportProtos.GetAttributeResponseMsg actualAttributesResponse = TransportProtos.GetAttributeResponseMsg.parseFrom(getAttributesResponse.getPayload());
        assertEquals(expectedAttributesResponse.getRequestId(), actualAttributesResponse.getRequestId());
        List<TransportProtos.KeyValueProto> expectedClientKeyValueProtos = expectedAttributesResponse.getClientAttributeListList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());
        List<TransportProtos.KeyValueProto> expectedSharedKeyValueProtos = expectedAttributesResponse.getSharedAttributeListList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());
        List<TransportProtos.KeyValueProto> actualClientKeyValueProtos = actualAttributesResponse.getClientAttributeListList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());
        List<TransportProtos.KeyValueProto> actualSharedKeyValueProtos = actualAttributesResponse.getSharedAttributeListList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());
        assertTrue(actualClientKeyValueProtos.containsAll(expectedClientKeyValueProtos));
        assertTrue(actualSharedKeyValueProtos.containsAll(expectedSharedKeyValueProtos));
    }

    protected void validateUpdateAttributesJsonResponse(@NotNull CoapTestCallback callback, String expectedResponse) {
        assertNotNull(callback.getPayloadBytes());
        @NotNull String response = new String(callback.getPayloadBytes(), StandardCharsets.UTF_8);
        assertEquals(JacksonUtil.toJsonNode(expectedResponse), JacksonUtil.toJsonNode(response));
    }

    protected void validateEmptyCurrentStateAttributesProtoResponse(@NotNull CoapTestCallback callback) throws InvalidProtocolBufferException {
        assertArrayEquals(EMPTY_PAYLOAD, callback.getPayloadBytes());
    }

    protected void validateCurrentStateAttributesProtoResponse(@NotNull CoapTestCallback callback) throws InvalidProtocolBufferException {
        assertNotNull(callback.getPayloadBytes());
        TransportProtos.AttributeUpdateNotificationMsg.Builder expectedCurrentStateNotificationMsgBuilder = TransportProtos.AttributeUpdateNotificationMsg.newBuilder();
        TransportProtos.TsKvProto tsKvProtoAttribute1 = getTsKvProto("sharedStr", "value", TransportProtos.KeyValueType.STRING_V);
        TransportProtos.TsKvProto tsKvProtoAttribute2 = getTsKvProto("sharedBool", "false", TransportProtos.KeyValueType.BOOLEAN_V);
        TransportProtos.TsKvProto tsKvProtoAttribute3 = getTsKvProto("sharedDbl", "41.0", TransportProtos.KeyValueType.DOUBLE_V);
        TransportProtos.TsKvProto tsKvProtoAttribute4 = getTsKvProto("sharedLong", "72", TransportProtos.KeyValueType.LONG_V);
        TransportProtos.TsKvProto tsKvProtoAttribute5 = getTsKvProto("sharedJson", "{\"someNumber\":41,\"someArray\":[],\"someNestedObject\":{\"key\":\"value\"}}", TransportProtos.KeyValueType.JSON_V);
        @NotNull List<TransportProtos.TsKvProto> tsKvProtoList = new ArrayList<>();
        tsKvProtoList.add(tsKvProtoAttribute1);
        tsKvProtoList.add(tsKvProtoAttribute2);
        tsKvProtoList.add(tsKvProtoAttribute3);
        tsKvProtoList.add(tsKvProtoAttribute4);
        tsKvProtoList.add(tsKvProtoAttribute5);
        TransportProtos.AttributeUpdateNotificationMsg expectedCurrentStateNotificationMsg = expectedCurrentStateNotificationMsgBuilder.addAllSharedUpdated(tsKvProtoList).build();
        TransportProtos.AttributeUpdateNotificationMsg actualCurrentStateNotificationMsg = TransportProtos.AttributeUpdateNotificationMsg.parseFrom(callback.getPayloadBytes());

        List<TransportProtos.KeyValueProto> expectedSharedUpdatedList = expectedCurrentStateNotificationMsg.getSharedUpdatedList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());
        List<TransportProtos.KeyValueProto> actualSharedUpdatedList = actualCurrentStateNotificationMsg.getSharedUpdatedList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());

        assertEquals(expectedSharedUpdatedList.size(), actualSharedUpdatedList.size());
        assertTrue(actualSharedUpdatedList.containsAll(expectedSharedUpdatedList));
    }

    protected void validateUpdateProtoAttributesResponse(@NotNull CoapTestCallback callback, int expectedObserveCnt) throws InvalidProtocolBufferException {
        assertNotNull(callback.getPayloadBytes());
        TransportProtos.AttributeUpdateNotificationMsg.Builder attributeUpdateNotificationMsgBuilder = TransportProtos.AttributeUpdateNotificationMsg.newBuilder();
        @NotNull List<TransportProtos.TsKvProto> tsKvProtoList = getTsKvProtoList("shared");
        attributeUpdateNotificationMsgBuilder.addAllSharedUpdated(tsKvProtoList);

        TransportProtos.AttributeUpdateNotificationMsg expectedAttributeUpdateNotificationMsg = attributeUpdateNotificationMsgBuilder.build();
        TransportProtos.AttributeUpdateNotificationMsg actualAttributeUpdateNotificationMsg = TransportProtos.AttributeUpdateNotificationMsg.parseFrom(callback.getPayloadBytes());

        List<TransportProtos.KeyValueProto> actualSharedUpdatedList = actualAttributeUpdateNotificationMsg.getSharedUpdatedList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());
        List<TransportProtos.KeyValueProto> expectedSharedUpdatedList = expectedAttributeUpdateNotificationMsg.getSharedUpdatedList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());

        assertEquals(expectedSharedUpdatedList.size(), actualSharedUpdatedList.size());
        assertTrue(actualSharedUpdatedList.containsAll(expectedSharedUpdatedList));
    }

    protected void validateDeleteProtoAttributesResponse(@NotNull CoapTestCallback callback, int expectedObserveCnt) throws InvalidProtocolBufferException {
        assertNotNull(callback.getPayloadBytes());
        TransportProtos.AttributeUpdateNotificationMsg.Builder attributeUpdateNotificationMsgBuilder = TransportProtos.AttributeUpdateNotificationMsg.newBuilder();
        attributeUpdateNotificationMsgBuilder.addSharedDeleted("sharedJson");

        TransportProtos.AttributeUpdateNotificationMsg expectedAttributeUpdateNotificationMsg = attributeUpdateNotificationMsgBuilder.build();
        TransportProtos.AttributeUpdateNotificationMsg actualAttributeUpdateNotificationMsg = TransportProtos.AttributeUpdateNotificationMsg.parseFrom(callback.getPayloadBytes());

        assertEquals(expectedAttributeUpdateNotificationMsg.getSharedDeletedList().size(), actualAttributeUpdateNotificationMsg.getSharedDeletedList().size());
        assertEquals("sharedJson", actualAttributeUpdateNotificationMsg.getSharedDeletedList().get(0));
    }

    private void awaitClientAfterCancelObserve() {
        Awaitility.await("awaitClientAfterCancelObserve")
                .pollInterval(10, TimeUnit.MILLISECONDS)
                .atMost(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .until(() -> {
                    log.trace("awaiting defaultTransportService.sessions is empty");
                    return defaultTransportService.sessions.isEmpty();
                });
    }

    private TransportProtos.GetAttributeResponseMsg getExpectedAttributeResponseMsg() {
        TransportProtos.GetAttributeResponseMsg.Builder result = TransportProtos.GetAttributeResponseMsg.newBuilder();
        @NotNull List<TransportProtos.TsKvProto> csTsKvProtoList = getTsKvProtoList("client");
        @NotNull List<TransportProtos.TsKvProto> shTsKvProtoList = getTsKvProtoList("shared");
        result.addAllClientAttributeList(csTsKvProtoList);
        result.addAllSharedAttributeList(shTsKvProtoList);
        result.setRequestId(0);
        return result.build();
    }
}

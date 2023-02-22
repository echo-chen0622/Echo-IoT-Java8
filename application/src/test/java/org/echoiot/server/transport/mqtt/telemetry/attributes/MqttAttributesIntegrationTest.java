package org.echoiot.server.transport.mqtt.telemetry.attributes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.Device;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.dao.service.DaoSqlTest;
import org.echoiot.server.transport.mqtt.AbstractMqttIntegrationTest;
import org.echoiot.server.transport.mqtt.MqttTestClient;
import org.echoiot.server.transport.mqtt.MqttTestConfigProperties;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.echoiot.server.common.data.device.profile.MqttTopics.*;
import static org.junit.Assert.*;

@Slf4j
@DaoSqlTest
public class MqttAttributesIntegrationTest extends AbstractMqttIntegrationTest {

    protected static final String PAYLOAD_VALUES_STR = "{\"key1\":\"value1\", \"key2\":true, \"key3\": 3.0, \"key4\": 4," +
            " \"key5\": {\"someNumber\": 42, \"someArray\": [1,2,3], \"someNestedObject\": {\"key\": \"value\"}}}";

    @Before
    public void beforeTest() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Post Attributes device")
                .gatewayName("Test Post Attributes gateway")
                .build();
        processBeforeTest(configProperties);
    }

    @Test
    public void testPushAttributes() throws Exception {
        List<String> expectedKeys = Arrays.asList("key1", "key2", "key3", "key4", "key5");
        processJsonPayloadAttributesTest(DEVICE_ATTRIBUTES_TOPIC, expectedKeys, PAYLOAD_VALUES_STR.getBytes());
    }

    @Test
    public void testPushAttributesOnShortTopic() throws Exception {
        List<String> expectedKeys = Arrays.asList("key1", "key2", "key3", "key4", "key5");
        processJsonPayloadAttributesTest(DEVICE_ATTRIBUTES_SHORT_TOPIC, expectedKeys, PAYLOAD_VALUES_STR.getBytes());
    }

    @Test
    public void testPushAttributesOnShortJsonTopic() throws Exception {
        List<String> expectedKeys = Arrays.asList("key1", "key2", "key3", "key4", "key5");
        processJsonPayloadAttributesTest(DEVICE_ATTRIBUTES_SHORT_JSON_TOPIC, expectedKeys, PAYLOAD_VALUES_STR.getBytes());
    }

    @Test
    public void testPushAttributesGateway() throws Exception {
        List<String> expectedKeys = Arrays.asList("key1", "key2", "key3", "key4", "key5");
        String deviceName1 = "Device A";
        String deviceName2 = "Device B";
        String payload = getGatewayAttributesJsonPayload(deviceName1, deviceName2);
        processGatewayAttributesTest(expectedKeys, payload.getBytes(), deviceName1, deviceName2);
    }

    protected void processJsonPayloadAttributesTest(String topic, List<String> expectedKeys, byte[] payload) throws Exception {
        processAttributesTest(topic, expectedKeys, payload, false);
    }

    protected void processAttributesTest(String topic, List<String> expectedKeys, byte[] payload, boolean presenceFieldsTest) throws Exception {
        MqttTestClient client = new MqttTestClient();
        client.connectAndWait(accessToken);

        client.publishAndWait(topic, payload);
        client.disconnect();

        DeviceId deviceId = savedDevice.getId();

        long start = System.currentTimeMillis();
        long end = System.currentTimeMillis() + 5000;

        @Nullable List<String> actualKeys = null;
        while (start <= end) {
            actualKeys = doGetAsyncTyped("/api/plugins/telemetry/DEVICE/" + deviceId + "/keys/attributes/CLIENT_SCOPE", new TypeReference<>() {});
            if (actualKeys.size() == expectedKeys.size()) {
                break;
            }
            Thread.sleep(100);
            start += 100;
        }
        assertNotNull(actualKeys);

        Set<String> actualKeySet = new HashSet<>(actualKeys);

        Set<String> expectedKeySet = new HashSet<>(expectedKeys);

        assertEquals(expectedKeySet, actualKeySet);

        String getAttributesValuesUrl = getAttributesValuesUrl(deviceId, actualKeySet);
        List<Map<String, Object>> values = doGetAsyncTyped(getAttributesValuesUrl, new TypeReference<>() {});
        if (presenceFieldsTest) {
            assertAttributesProtoValues(values, actualKeySet);
        } else {
            assertAttributesValues(values, actualKeySet);
        }
        String deleteAttributesUrl = "/api/plugins/telemetry/DEVICE/" + deviceId + "/CLIENT_SCOPE?keys=" + String.join(",", actualKeySet);
        doDelete(deleteAttributesUrl);
    }

    protected void processGatewayAttributesTest(List<String> expectedKeys, byte[] payload, String firstDeviceName, String secondDeviceName) throws Exception {
        MqttTestClient client = new MqttTestClient();
        client.connectAndWait(gatewayAccessToken);

        client.publishAndWait(GATEWAY_ATTRIBUTES_TOPIC, payload);
        client.disconnect();

        Device firstDevice = doExecuteWithRetriesAndInterval(() -> doGet("/api/tenant/devices?deviceName=" + firstDeviceName, Device.class),
                20,
                100);

        assertNotNull(firstDevice);

        Device secondDevice = doExecuteWithRetriesAndInterval(() -> doGet("/api/tenant/devices?deviceName=" + secondDeviceName, Device.class),
                20,
                100);

        assertNotNull(secondDevice);

        @Nullable List<String> firstDeviceActualKeys = getActualKeysList(firstDevice.getId(), expectedKeys);
        assertNotNull(firstDeviceActualKeys);
        Set<String> firstDeviceActualKeySet = new HashSet<>(firstDeviceActualKeys);

        @Nullable List<String> secondDeviceActualKeys = getActualKeysList(secondDevice.getId(), expectedKeys);
        assertNotNull(secondDeviceActualKeys);
        Set<String> secondDeviceActualKeySet = new HashSet<>(secondDeviceActualKeys);

        Set<String> expectedKeySet = new HashSet<>(expectedKeys);

        assertEquals(expectedKeySet, firstDeviceActualKeySet);
        assertEquals(expectedKeySet, secondDeviceActualKeySet);

        String getAttributesValuesUrlFirstDevice = getAttributesValuesUrl(firstDevice.getId(), firstDeviceActualKeySet);
        String getAttributesValuesUrlSecondDevice = getAttributesValuesUrl(firstDevice.getId(), secondDeviceActualKeySet);

        List<Map<String, Object>> firstDeviceValues = doGetAsyncTyped(getAttributesValuesUrlFirstDevice, new TypeReference<>() {});
        List<Map<String, Object>> secondDeviceValues = doGetAsyncTyped(getAttributesValuesUrlSecondDevice, new TypeReference<>() {});

        assertAttributesValues(firstDeviceValues, expectedKeySet);
        assertAttributesValues(secondDeviceValues, expectedKeySet);

    }

    @Nullable
    private List<String> getActualKeysList(DeviceId deviceId, List<String> expectedKeys) throws Exception {
        long start = System.currentTimeMillis();
        long end = System.currentTimeMillis() + 3000;
        @Nullable List<String> firstDeviceActualKeys = null;
        while (start <= end) {
            firstDeviceActualKeys = doGetAsyncTyped("/api/plugins/telemetry/DEVICE/" + deviceId + "/keys/attributes/CLIENT_SCOPE", new TypeReference<>() {});
            if (firstDeviceActualKeys.size() == expectedKeys.size()) {
                break;
            }
            Thread.sleep(100);
            start += 100;
        }
        return firstDeviceActualKeys;
    }

    @SuppressWarnings("unchecked")
    protected void assertAttributesValues(List<Map<String, Object>> deviceValues, Set<String> keySet) throws JsonProcessingException {
        for (Map<String, Object> map : deviceValues) {
            String key = (String) map.get("key");
            Object value = map.get("value");
            assertTrue(keySet.contains(key));
            switch (key) {
                case "key1":
                    assertEquals("value1", value);
                    break;
                case "key2":
                    assertEquals(true, value);
                    break;
                case "key3":
                    assertEquals(3.0, value);
                    break;
                case "key4":
                    assertEquals(4, value);
                    break;
                case "key5":
                    assertNotNull(value);
                    assertEquals(3, ((LinkedHashMap) value).size());
                    assertEquals(42, ((LinkedHashMap) value).get("someNumber"));
                    assertEquals(Arrays.asList(1, 2, 3), ((LinkedHashMap) value).get("someArray"));
                    LinkedHashMap<String, String> someNestedObject = (LinkedHashMap) ((LinkedHashMap) value).get("someNestedObject");
                    assertEquals("value", someNestedObject.get("key"));
                    break;
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void assertAttributesProtoValues(List<Map<String, Object>> values, Set<String> keySet) {
        for (Map<String, Object> map : values) {
            String key = (String) map.get("key");
            Object value = map.get("value");
            assertTrue(keySet.contains(key));
            switch (key) {
                case "key1":
                    assertEquals("", value);
                    break;
                case "key5":
                    assertNotNull(value);
                    assertEquals(2, ((LinkedHashMap) value).size());
                    assertEquals(Arrays.asList(1, 2, 3), ((LinkedHashMap) value).get("someArray"));
                    LinkedHashMap<String, String> someNestedObject = (LinkedHashMap) ((LinkedHashMap) value).get("someNestedObject");
                    assertEquals("value", someNestedObject.get("key"));
                    break;
            }
        }
    }

    protected String getGatewayAttributesJsonPayload(String deviceA, String deviceB) {
        return "{\"" + deviceA + "\": " + PAYLOAD_VALUES_STR + ",  \"" + deviceB + "\": " + PAYLOAD_VALUES_STR + "}";
    }

    private String getAttributesValuesUrl(DeviceId deviceId, Set<String> actualKeySet) {
        return "/api/plugins/telemetry/DEVICE/" + deviceId + "/values/attributes/CLIENT_SCOPE?keys=" + String.join(",", actualKeySet);
    }
}

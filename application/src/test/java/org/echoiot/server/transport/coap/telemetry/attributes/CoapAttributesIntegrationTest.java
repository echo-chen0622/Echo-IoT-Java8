package org.echoiot.server.transport.coap.telemetry.attributes;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.msg.session.FeatureType;
import org.echoiot.server.dao.service.DaoSqlTest;
import org.echoiot.server.transport.coap.AbstractCoapIntegrationTest;
import org.echoiot.server.transport.coap.CoapTestClient;
import org.echoiot.server.transport.coap.CoapTestConfigProperties;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

@Slf4j
@DaoSqlTest
public class CoapAttributesIntegrationTest extends AbstractCoapIntegrationTest {

    private static final String PAYLOAD_VALUES_STR = "{\"key1\":\"value1\", \"key2\":true, \"key3\": 3.0, \"key4\": 4," +
            " \"key5\": {\"someNumber\": 42, \"someArray\": [1,2,3], \"someNestedObject\": {\"key\": \"value\"}}}";

    @Before
    public void beforeTest() throws Exception {
        CoapTestConfigProperties configProperties = CoapTestConfigProperties.builder()
                .deviceName("Test Post Attributes device")
                .build();
        processBeforeTest(configProperties);
    }

    @After
    public void afterTest() throws Exception {
        processAfterTest();
    }

    @Test
    public void testPushAttributes() throws Exception {
        @NotNull List<String> expectedKeys = Arrays.asList("key1", "key2", "key3", "key4", "key5");
        processJsonPayloadAttributesTest(expectedKeys, PAYLOAD_VALUES_STR.getBytes());
    }

    protected void processJsonPayloadAttributesTest(@NotNull List<String> expectedKeys, byte[] payload) throws Exception {
        processAttributesTest(expectedKeys, payload, false);
    }

    protected void processAttributesTest(@NotNull List<String> expectedKeys, byte[] payload, boolean presenceFieldsTest) throws Exception {

        client = new CoapTestClient(accessToken, FeatureType.ATTRIBUTES);
        CoapResponse coapResponse = client.postMethod(payload);
        assertEquals(CoAP.ResponseCode.CREATED, coapResponse.getCode());

        DeviceId deviceId = savedDevice.getId();
        @Nullable List<String> actualKeys = getActualKeysList(deviceId, expectedKeys);
        assertNotNull(actualKeys);

        @NotNull Set<String> actualKeySet = new HashSet<>(actualKeys);
        @NotNull Set<String> expectedKeySet = new HashSet<>(expectedKeys);
        assertEquals(expectedKeySet, actualKeySet);

        @NotNull String getAttributesValuesUrl = getAttributesValuesUrl(deviceId, actualKeySet);
        List<Map<String, Object>> values = doGetAsyncTyped(getAttributesValuesUrl, new TypeReference<>() {});
        if (presenceFieldsTest) {
            assertAttributesProtoValues(values, actualKeySet);
        } else {
            assertAttributesValues(values, actualKeySet);
        }
        @NotNull String deleteAttributesUrl = "/api/plugins/telemetry/DEVICE/" + deviceId + "/CLIENT_SCOPE?keys=" + String.join(",", actualKeySet);
        doDelete(deleteAttributesUrl);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected void assertAttributesValues(@NotNull List<Map<String, Object>> deviceValues, @NotNull Set<String> keySet) {
        for (@NotNull Map<String, Object> map : deviceValues) {
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
    private void assertAttributesProtoValues(@NotNull List<Map<String, Object>> values, @NotNull Set<String> keySet) {
        for (@NotNull Map<String, Object> map : values) {
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

    @Nullable
    private List<String> getActualKeysList(DeviceId deviceId, @NotNull List<String> expectedKeys) throws Exception {
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
        return actualKeys;
    }

    @NotNull
    private String getAttributesValuesUrl(DeviceId deviceId, @NotNull Set<String> actualKeySet) {
        return "/api/plugins/telemetry/DEVICE/" + deviceId + "/values/attributes/CLIENT_SCOPE?keys=" + String.join(",", actualKeySet);
    }

}

package org.echoiot.server.transport.mqtt.attributes.request;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.TransportPayloadType;
import org.echoiot.server.common.data.device.profile.MqttTopics;
import org.echoiot.server.dao.service.DaoSqlTest;
import org.echoiot.server.transport.mqtt.MqttTestConfigProperties;
import org.echoiot.server.transport.mqtt.attributes.AbstractMqttAttributesIntegrationTest;
import org.junit.Before;
import org.junit.Test;

@Slf4j
@DaoSqlTest
public class MqttAttributesRequestJsonIntegrationTest extends AbstractMqttAttributesIntegrationTest {

    @Before
    public void beforeTest() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Request attribute values from the server json")
                .gatewayName("Gateway Test Request attribute values from the server json")
                .transportPayloadType(TransportPayloadType.JSON)
                .build();
        processBeforeTest(configProperties);
    }

    @Test
    public void testRequestAttributesValuesFromTheServer() throws Exception {
        processJsonTestRequestAttributesValuesFromTheServer(MqttTopics.DEVICE_ATTRIBUTES_TOPIC, MqttTopics.DEVICE_ATTRIBUTES_RESPONSES_TOPIC, MqttTopics.DEVICE_ATTRIBUTES_REQUEST_TOPIC_PREFIX);
    }

    @Test
    public void testRequestAttributesValuesFromTheServerOnShortTopic() throws Exception {
        processJsonTestRequestAttributesValuesFromTheServer(MqttTopics.DEVICE_ATTRIBUTES_SHORT_TOPIC, MqttTopics.DEVICE_ATTRIBUTES_RESPONSES_SHORT_TOPIC, MqttTopics.DEVICE_ATTRIBUTES_REQUEST_SHORT_TOPIC_PREFIX);
    }

    @Test
    public void testRequestAttributesValuesFromTheServerOnShortJsonTopic() throws Exception {
        processJsonTestRequestAttributesValuesFromTheServer(MqttTopics.DEVICE_ATTRIBUTES_SHORT_JSON_TOPIC, MqttTopics.DEVICE_ATTRIBUTES_RESPONSES_SHORT_JSON_TOPIC, MqttTopics.DEVICE_ATTRIBUTES_REQUEST_SHORT_JSON_TOPIC_PREFIX);
    }

    @Test
    public void testRequestAttributesValuesFromTheServerGateway() throws Exception {
        processJsonTestGatewayRequestAttributesValuesFromTheServer();
    }
}

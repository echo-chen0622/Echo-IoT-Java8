package org.thingsboard.server.transport.mqtt.attributes.request;

import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.device.profile.MqttTopics;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.transport.mqtt.MqttTestConfigProperties;
import org.thingsboard.server.transport.mqtt.attributes.AbstractMqttAttributesIntegrationTest;

@Slf4j
@DaoSqlTest
public class MqttAttributesRequestIntegrationTest extends AbstractMqttAttributesIntegrationTest {

    @Before
    public void beforeTest() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Request attribute values from the server")
                .gatewayName("Gateway Test Request attribute values from the server")
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

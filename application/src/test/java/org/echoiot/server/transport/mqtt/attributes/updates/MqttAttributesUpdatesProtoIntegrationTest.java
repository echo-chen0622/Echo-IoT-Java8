package org.echoiot.server.transport.mqtt.attributes.updates;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.TransportPayloadType;
import org.echoiot.server.dao.service.DaoSqlTest;
import org.echoiot.server.transport.mqtt.MqttTestConfigProperties;
import org.echoiot.server.transport.mqtt.attributes.AbstractMqttAttributesIntegrationTest;
import org.junit.Before;
import org.junit.Test;

import static org.echoiot.server.common.data.device.profile.MqttTopics.*;

@Slf4j
@DaoSqlTest
public class MqttAttributesUpdatesProtoIntegrationTest extends AbstractMqttAttributesIntegrationTest {

    @Before
    public void beforeTest() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Subscribe to attribute updates")
                .gatewayName("Gateway Test Subscribe to attribute updates")
                .transportPayloadType(TransportPayloadType.PROTOBUF)
                .build();
        processBeforeTest(configProperties);
    }

    @Test
    public void testProtoSubscribeToAttributesUpdatesFromTheServer() throws Exception {
        processProtoTestSubscribeToAttributesUpdates(DEVICE_ATTRIBUTES_TOPIC);
    }

    @Test
    public void testProtoSubscribeToAttributesUpdatesFromTheServerOnShortTopic() throws Exception {
        processProtoTestSubscribeToAttributesUpdates(DEVICE_ATTRIBUTES_SHORT_TOPIC);
    }

    @Test
    public void testProtoSubscribeToAttributesUpdatesFromTheServerOnShortJsonTopic() throws Exception {
        processJsonTestSubscribeToAttributesUpdates(DEVICE_ATTRIBUTES_SHORT_JSON_TOPIC);
    }

    @Test
    public void testProtoSubscribeToAttributesUpdatesFromTheServerOnShortProtoTopic() throws Exception {
        processProtoTestSubscribeToAttributesUpdates(DEVICE_ATTRIBUTES_SHORT_PROTO_TOPIC);
    }

    @Test
    public void testProtoSubscribeToAttributesUpdatesFromTheServerGateway() throws Exception {
        processProtoGatewayTestSubscribeToAttributesUpdates();
    }

}

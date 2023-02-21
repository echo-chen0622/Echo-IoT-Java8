package org.echoiot.server.transport.mqtt.rpc;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.TransportPayloadType;
import org.echoiot.server.dao.service.DaoSqlTest;
import org.echoiot.server.transport.mqtt.MqttTestConfigProperties;
import org.junit.Before;
import org.junit.Test;

import static org.echoiot.server.common.data.device.profile.MqttTopics.*;

@Slf4j
@DaoSqlTest
public class MqttServerSideRpcProtoIntegrationTest extends AbstractMqttServerSideRpcIntegrationTest {

    @Before
    public void beforeTest() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("RPC test device")
                .gatewayName("RPC test gateway")
                .transportPayloadType(TransportPayloadType.PROTOBUF)
                .rpcRequestProtoSchema(RPC_REQUEST_PROTO_SCHEMA)
                .build();
        processBeforeTest(configProperties);
    }

    @Test
    public void testServerMqttOneWayRpc() throws Exception {
        processOneWayRpcTest(DEVICE_RPC_REQUESTS_SUB_TOPIC);
    }

    @Test
    public void testServerMqttOneWayRpcOnShortTopic() throws Exception {
        processOneWayRpcTest(DEVICE_RPC_REQUESTS_SUB_SHORT_TOPIC);
    }

    @Test
    public void testServerMqttOneWayRpcOnShortProtoTopic() throws Exception {
        processOneWayRpcTest(DEVICE_RPC_REQUESTS_SUB_SHORT_PROTO_TOPIC);
    }

    @Test
    public void testServerMqttTwoWayRpc() throws Exception {
        processProtoTwoWayRpcTest(DEVICE_RPC_REQUESTS_SUB_TOPIC);
    }

    @Test
    public void testServerMqttTwoWayRpcOnShortTopic() throws Exception {
        processProtoTwoWayRpcTest(DEVICE_RPC_REQUESTS_SUB_SHORT_TOPIC);
    }

    @Test
    public void testServerMqttTwoWayRpcOnShortProtoTopic() throws Exception {
        processProtoTwoWayRpcTest(DEVICE_RPC_REQUESTS_SUB_SHORT_PROTO_TOPIC);
    }

    @Test
    public void testGatewayServerMqttOneWayRpc() throws Exception {
        processProtoOneWayRpcTestGateway("Gateway Device OneWay RPC Proto");
    }

    @Test
    public void testGatewayServerMqttTwoWayRpc() throws Exception {
        processProtoTwoWayRpcTestGateway("Gateway Device TwoWay RPC Proto");
    }

}

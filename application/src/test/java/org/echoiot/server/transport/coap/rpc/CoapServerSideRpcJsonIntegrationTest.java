package org.echoiot.server.transport.coap.rpc;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.CoapDeviceType;
import org.echoiot.server.common.data.TransportPayloadType;
import org.echoiot.server.dao.service.DaoSqlTest;
import org.echoiot.server.transport.coap.CoapTestConfigProperties;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@Slf4j
@DaoSqlTest
public class CoapServerSideRpcJsonIntegrationTest extends AbstractCoapServerSideRpcIntegrationTest {

    @Before
    public void beforeTest() throws Exception {
        CoapTestConfigProperties configProperties = CoapTestConfigProperties.builder()
                .deviceName("RPC test device")
                .coapDeviceType(CoapDeviceType.DEFAULT)
                .transportPayloadType(TransportPayloadType.JSON)
                .build();
        processBeforeTest(configProperties);
    }

    @After
    public void afterTest() throws Exception {
        processAfterTest();
    }

    @Test
    public void testServerCoapOneWayRpc() throws Exception {
        processOneWayRpcTest(false);
    }

    @Test
    public void testServerCoapTwoWayRpc() throws Exception {
        processTwoWayRpcTest("{\"value1\":\"A\",\"value2\":\"B\"}", false);
    }
}

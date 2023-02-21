package org.echoiot.server.transport.coap.attributes.updates;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.CoapDeviceType;
import org.echoiot.server.common.data.TransportPayloadType;
import org.echoiot.server.dao.service.DaoSqlTest;
import org.echoiot.server.transport.coap.CoapTestConfigProperties;
import org.echoiot.server.transport.coap.attributes.AbstractCoapAttributesIntegrationTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@Slf4j
@DaoSqlTest
public class CoapAttributesUpdatesProtoIntegrationTest extends AbstractCoapAttributesIntegrationTest {

    @Before
    public void beforeTest() throws Exception {
        CoapTestConfigProperties configProperties = CoapTestConfigProperties.builder()
                .deviceName("Test Subscribe to attribute updates")
                .coapDeviceType(CoapDeviceType.DEFAULT)
                .transportPayloadType(TransportPayloadType.PROTOBUF)
                .build();
        processBeforeTest(configProperties);
    }

    @After
    public void afterTest() throws Exception {
        processAfterTest();
    }

    @Test
    public void testSubscribeToAttributesUpdatesFromTheServer() throws Exception {
        processProtoTestSubscribeToAttributesUpdates(false);
    }

    @Test
    public void testSubscribeToAttributesUpdatesFromTheServerWithEmptyCurrentStateNotification() throws Exception {
        processProtoTestSubscribeToAttributesUpdates(true);
    }
}

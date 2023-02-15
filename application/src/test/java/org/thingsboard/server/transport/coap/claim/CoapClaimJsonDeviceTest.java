package org.thingsboard.server.transport.coap.claim;

import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.CoapDeviceType;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.transport.coap.CoapTestConfigProperties;

@Slf4j
@DaoSqlTest
public class CoapClaimJsonDeviceTest extends CoapClaimDeviceTest {

    @Before
    public void beforeTest() throws Exception {
        CoapTestConfigProperties configProperties = CoapTestConfigProperties.builder()
                .deviceName("Test Claim device Json")
                .coapDeviceType(CoapDeviceType.DEFAULT)
                .transportPayloadType(TransportPayloadType.JSON)
                .build();
        processBeforeTest(configProperties);
        createCustomerAndUser();
    }

    @After
    public void afterTest() throws Exception {
        super.afterTest();
    }

    @Test
    public void testClaimingDevice() throws Exception {
        super.testClaimingDevice();
    }

    @Test
    public void testClaimingDeviceWithoutSecretAndDuration() throws Exception {
        super.testClaimingDeviceWithoutSecretAndDuration();
    }
}

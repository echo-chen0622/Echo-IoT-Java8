package org.thingsboard.server.transport.coap.attributes.updates;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.server.resources.Resource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.coapserver.DefaultCoapServerService;
import org.thingsboard.server.common.transport.service.DefaultTransportService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.transport.coap.CoapTestConfigProperties;
import org.thingsboard.server.transport.coap.CoapTransportResource;
import org.thingsboard.server.transport.coap.attributes.AbstractCoapAttributesIntegrationTest;

import static org.mockito.Mockito.spy;

@Slf4j
@DaoSqlTest
public class CoapAttributesUpdatesIntegrationTest extends AbstractCoapAttributesIntegrationTest {

    CoapTransportResource coapTransportResource;

    @Autowired
    DefaultCoapServerService defaultCoapServerService;

    @Autowired
    DefaultTransportService defaultTransportService;

    @Before
    public void beforeTest() throws Exception {
        Resource api = defaultCoapServerService.getCoapServer().getRoot().getChild("api");
        coapTransportResource = spy( (CoapTransportResource) api.getChild("v1") );
        api.delete(api.getChild("v1") );
        api.add(coapTransportResource);
        CoapTestConfigProperties configProperties = CoapTestConfigProperties.builder()
                .deviceName("Test Subscribe to attribute updates")
                .build();
        processBeforeTest(configProperties);
    }

    @After
    public void afterTest() throws Exception {
        processAfterTest();
    }

    @Test
    public void testSubscribeToAttributesUpdatesFromTheServer() throws Exception {
        processJsonTestSubscribeToAttributesUpdates(false);
    }

    @Test
    public void testSubscribeToAttributesUpdatesFromTheServerWithEmptyCurrentStateNotification() throws Exception {
        processJsonTestSubscribeToAttributesUpdates(true);
    }
}

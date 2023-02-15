package org.thingsboard.server.transport.lwm2m.server.client;

import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.server.registration.Registration;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import java.net.InetSocketAddress;

public class LwM2mClientTest {

    @Test
    public void setRegistration() {
        LwM2mClient client = new LwM2mClient("nodeId", "testEndpoint");
        Registration registration = new Registration
                .Builder("test", "testEndpoint", Identity.unsecure(new InetSocketAddress(1000)))
                .objectLinks(new Link[0])
                .build();

        Assertions.assertDoesNotThrow(() -> client.setRegistration(registration));
    }
}

package org.echoiot.server.transport.lwm2m.security.sql;

import org.echoiot.server.common.data.device.credentials.lwm2m.LwM2MDeviceCredentials;
import org.echoiot.server.common.data.device.credentials.lwm2m.PSKClientCredential;
import org.echoiot.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.echoiot.server.transport.lwm2m.security.AbstractSecurityLwM2MIntegrationTest;
import org.eclipse.leshan.client.object.Security;
import org.eclipse.leshan.core.util.Hex;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.springframework.test.web.servlet.MvcResult;

import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;

import static org.echoiot.server.common.data.device.credentials.lwm2m.LwM2MSecurityMode.PSK;
import static org.echoiot.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_REGISTRATION_SUCCESS;
import static org.echoiot.server.transport.lwm2m.Lwm2mTestHelper.LwM2MProfileBootstrapConfigType.BOTH;
import static org.echoiot.server.transport.lwm2m.Lwm2mTestHelper.LwM2MProfileBootstrapConfigType.NONE;
import static org.eclipse.leshan.client.object.Security.psk;
import static org.eclipse.leshan.client.object.Security.pskBootstrap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PskLwm2mIntegrationTest extends AbstractSecurityLwM2MIntegrationTest {

    //Lwm2m only
    @Test
    public void testWithPskConnectLwm2mSuccess() throws Exception {
        @NotNull String clientEndpoint = CLIENT_ENDPOINT_PSK;
        @NotNull String identity = CLIENT_PSK_IDENTITY;
        @NotNull String keyPsk = CLIENT_PSK_KEY;
        @NotNull PSKClientCredential clientCredentials = new PSKClientCredential();
        clientCredentials.setEndpoint(clientEndpoint);
        clientCredentials.setIdentity(identity);
        clientCredentials.setKey(keyPsk);
        @NotNull Security security = psk(SECURE_URI,
                                         shortServerId,
                                         identity.getBytes(StandardCharsets.UTF_8),
                                         Hex.decodeHex(keyPsk.toCharArray()));
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = getTransportConfiguration(OBSERVE_ATTRIBUTES_WITHOUT_PARAMS, getBootstrapServerCredentialsSecure(PSK, NONE));
        LwM2MDeviceCredentials deviceCredentials = getDeviceCredentialsSecure(clientCredentials, null, null, PSK, false);
        this.basicTestConnection(security,
                deviceCredentials,
                COAP_CONFIG,
                clientEndpoint,
                transportConfiguration,
                "await on client state (Psk_Lwm2m)",
                expectedStatusesRegistrationLwm2mSuccess,
                false,
                ON_REGISTRATION_SUCCESS,
                true);
    }

    @Test
    public void testWithPskConnectLwm2mBadPskKeyByLength_BAD_REQUEST() throws Exception {
        @NotNull String clientEndpoint = CLIENT_ENDPOINT_PSK;
        @NotNull String identity = CLIENT_PSK_IDENTITY + "_BadLength";
        @NotNull String keyPsk = CLIENT_PSK_KEY + "05AC";
        @NotNull PSKClientCredential clientCredentials = new PSKClientCredential();
        clientCredentials.setEndpoint(clientEndpoint);
        clientCredentials.setIdentity(identity);
        clientCredentials.setKey(keyPsk);
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = getTransportConfiguration(OBSERVE_ATTRIBUTES_WITHOUT_PARAMS, getBootstrapServerCredentialsSecure(PSK, NONE));
        createDeviceProfile(transportConfiguration);
        LwM2MDeviceCredentials deviceCredentials = getDeviceCredentialsSecure(clientCredentials, null, null, PSK, false);
        MvcResult result = createDeviceWithMvcResult(deviceCredentials, clientEndpoint);
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, result.getResponse().getStatus());
        @NotNull String msgExpected = "Key must be HexDec format: 32, 64, 128 characters!";
        assertTrue(result.getResponse().getContentAsString().contains(msgExpected));
    }


    // Bootstrap + Lwm2m
    @Test
    public void testWithPskConnectBsSuccess_UpdateTwoSectionsBootstrapAndLm2m_ConnectLwm2mSuccess() throws Exception {
        @NotNull String clientEndpoint = CLIENT_ENDPOINT_PSK_BS;
        @NotNull String identity = CLIENT_PSK_IDENTITY_BS;
        @NotNull String keyPsk = CLIENT_PSK_KEY;
        @NotNull PSKClientCredential clientCredentials = new PSKClientCredential();
        clientCredentials.setEndpoint(clientEndpoint);
        clientCredentials.setIdentity(identity);
        clientCredentials.setKey(keyPsk);
        @NotNull Security securityBs = pskBootstrap(SECURE_URI_BS,
                                                    identity.getBytes(StandardCharsets.UTF_8),
                                                    Hex.decodeHex(keyPsk.toCharArray()));
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = getTransportConfiguration(OBSERVE_ATTRIBUTES_WITHOUT_PARAMS, getBootstrapServerCredentialsSecure(PSK, BOTH));
        LwM2MDeviceCredentials deviceCredentials = getDeviceCredentialsSecure(clientCredentials, null, null, PSK, false);
        this.basicTestConnection(securityBs,
                deviceCredentials,
                COAP_CONFIG_BS,
                clientEndpoint,
                transportConfiguration,
                "await on client state (PskBS two section)",
                expectedStatusesRegistrationBsSuccess,
                true,
                ON_REGISTRATION_SUCCESS,
                true);
    }
}

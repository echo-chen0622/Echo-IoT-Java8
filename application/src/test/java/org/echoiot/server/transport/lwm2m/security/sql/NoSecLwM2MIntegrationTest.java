package org.echoiot.server.transport.lwm2m.security.sql;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.echoiot.server.common.data.device.credentials.lwm2m.LwM2MDeviceCredentials;
import org.echoiot.server.transport.lwm2m.security.AbstractSecurityLwM2MIntegrationTest;

import static org.echoiot.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_BOOTSTRAP_SUCCESS;
import static org.echoiot.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_REGISTRATION_SUCCESS;
import static org.echoiot.server.transport.lwm2m.Lwm2mTestHelper.LwM2MProfileBootstrapConfigType.BOOTSTRAP_ONLY;
import static org.echoiot.server.transport.lwm2m.Lwm2mTestHelper.LwM2MProfileBootstrapConfigType.BOTH;
import static org.echoiot.server.transport.lwm2m.Lwm2mTestHelper.LwM2MProfileBootstrapConfigType.LWM2M_ONLY;
import static org.echoiot.server.transport.lwm2m.Lwm2mTestHelper.LwM2MProfileBootstrapConfigType.NONE;

public class NoSecLwM2MIntegrationTest extends AbstractSecurityLwM2MIntegrationTest {

    //Lwm2m only
    @Test
    public void testWithNoSecConnectLwm2mSuccessAndObserveTelemetry() throws Exception {
        @NotNull String clientEndpoint = CLIENT_ENDPOINT_NO_SEC;
        LwM2MDeviceCredentials clientCredentials = getDeviceCredentialsNoSec(createNoSecClientCredentials(clientEndpoint));
        super.basicTestConnectionObserveTelemetry(SECURITY_NO_SEC, clientCredentials, COAP_CONFIG, clientEndpoint);
    }

    // Bootstrap + Lwm2m
    @Test
    public void testWithNoSecConnectBsSuccess_UpdateTwoSectionsBootstrapAndLm2m_ConnectLwm2mSuccess() throws Exception {
        @NotNull String clientEndpoint = CLIENT_ENDPOINT_NO_SEC_BS;
        @NotNull String awaitAlias = "await on client state (NoSecBS two section)";
        basicTestConnectionBefore(clientEndpoint, awaitAlias, BOTH, expectedStatusesRegistrationBsSuccess, ON_REGISTRATION_SUCCESS);
    }

    @Test
    public void testWithNoSecConnectBsSuccess_UpdateLwm2mSectionAndLm2m_ConnectLwm2mSuccess() throws Exception {
        @NotNull String clientEndpoint = CLIENT_ENDPOINT_NO_SEC_BS + LWM2M_ONLY.name();
        @NotNull String awaitAlias = "await on client state (NoSecBS Lwm2m section)";
        basicTestConnectionBefore(clientEndpoint, awaitAlias, LWM2M_ONLY, expectedStatusesRegistrationBsSuccess, ON_REGISTRATION_SUCCESS);
    }

    @Test
    public void testWithNoSecConnectBsSuccess_UpdateBootstrapSectionAndLm2m_ConnectLwm2mSuccess() throws Exception {
        @NotNull String clientEndpoint = CLIENT_ENDPOINT_NO_SEC + BOOTSTRAP_ONLY.name();
        @NotNull String awaitAlias = "await on client state (NoSecBS Bootstrap section)";
        basicTestConnectionBefore(clientEndpoint, awaitAlias, BOOTSTRAP_ONLY, expectedStatusesBsSuccess, ON_BOOTSTRAP_SUCCESS);
    }

    @Test
    public void testWithNoSecConnectBsSuccess_UpdateNoneSectionAndLm2m_ConnectLwm2mSuccess() throws Exception {
        @NotNull String clientEndpoint = CLIENT_ENDPOINT_NO_SEC + NONE.name();
        @NotNull String awaitAlias = "await on client state (NoSecBS None section)";
        basicTestConnectionBefore(clientEndpoint, awaitAlias, NONE, expectedStatusesBsSuccess, ON_BOOTSTRAP_SUCCESS);
    }

    @Test
    public void testWithNoSecConnectLwm2mSuccessBootstrapRequestTriggerConnectBsSuccess_UpdateTwoSectionAndLm2m_ConnectLwm2mSuccess() throws Exception {
        @NotNull String clientEndpoint = CLIENT_ENDPOINT_NO_SEC_BS + "Trigger" + BOTH.name();
        @NotNull String awaitAlias = "await on client state (NoSecBS Trigger Two section)";
        basicTestConnectionBootstrapRequestTriggerBefore(clientEndpoint, awaitAlias, BOTH);
    }

    @Test
    public void testWithNoSecConnectLwm2mSuccessBootstrapRequestTriggerConnectBsSuccess_UpdateBootstrapSectionAndLm2m_ConnectLwm2mSuccess() throws Exception {
        @NotNull String clientEndpoint = CLIENT_ENDPOINT_NO_SEC_BS + "Trigger" + BOOTSTRAP_ONLY.name();
        @NotNull String awaitAlias = "await on client state (NoSecBS Trigger Bootstrap section)";
        basicTestConnectionBootstrapRequestTriggerBefore(clientEndpoint, awaitAlias, BOOTSTRAP_ONLY);
    }

    @Test
    public void testWithNoSecConnectLwm2mSuccessBootstrapRequestTriggerConnectBsSuccess_UpdateLwm2mSectionAndLm2m_ConnectLwm2mSuccess() throws Exception {
        @NotNull String clientEndpoint = CLIENT_ENDPOINT_NO_SEC_BS + "Trigger" + LWM2M_ONLY.name();
        @NotNull String awaitAlias = "await on client state (NoSecBS Trigger Lwm2m section)";
        basicTestConnectionBootstrapRequestTriggerBefore(clientEndpoint, awaitAlias, LWM2M_ONLY);
    }

    @Test
    public void testWithNoSecConnectLwm2mSuccessBootstrapRequestTriggerConnectBsSuccess_UpdateNoneSectionAndLm2m_ConnectLwm2mSuccess() throws Exception {
        @NotNull String clientEndpoint = CLIENT_ENDPOINT_NO_SEC_BS + "Trigger" + NONE.name();
        @NotNull String awaitAlias = "await on client state (NoSecBS Trigger None  section)";
        basicTestConnectionBootstrapRequestTriggerBefore(clientEndpoint, awaitAlias, NONE);
    }
}

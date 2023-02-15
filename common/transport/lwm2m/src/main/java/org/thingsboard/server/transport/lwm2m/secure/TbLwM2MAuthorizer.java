package org.thingsboard.server.transport.lwm2m.secure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.SecurityMode;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.request.UplinkRequest;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.security.Authorizer;
import org.eclipse.leshan.server.security.SecurityChecker;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.springframework.stereotype.Component;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2MAuthException;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClientContext;
import org.thingsboard.server.transport.lwm2m.server.store.TbLwM2MDtlsSessionStore;
import org.thingsboard.server.transport.lwm2m.server.store.TbMainSecurityStore;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
@TbLwM2mTransportComponent
@Slf4j
public class TbLwM2MAuthorizer implements Authorizer {

    private final TbLwM2MDtlsSessionStore sessionStorage;
    private final TbMainSecurityStore securityStore;
    private final SecurityChecker securityChecker = new SecurityChecker();
    private final LwM2mClientContext clientContext;

    @Override
    public Registration isAuthorized(UplinkRequest<?> request, Registration registration, Identity senderIdentity) {
        if (senderIdentity.isX509()) {
            TbX509DtlsSessionInfo sessionInfo = sessionStorage.get(registration.getEndpoint());
            if (sessionInfo != null) {
                if (sessionInfo.getX509CommonName().endsWith(senderIdentity.getX509CommonName())) {
                    clientContext.registerClient(registration, sessionInfo.getCredentials());
                    // X509 certificate is valid and matches endpoint.
                    return registration;
                } else {
                    // X509 certificate is not valid.
                    return null;
                }
            }
            // If session info is not found, this may be the trusted certificate, so we still need to check all other options below.
        }
        SecurityInfo expectedSecurityInfo;
            try {
                expectedSecurityInfo = securityStore.getByEndpoint(registration.getEndpoint());
                if (expectedSecurityInfo != null && expectedSecurityInfo.usePSK() && expectedSecurityInfo.getEndpoint().equals(SecurityMode.NO_SEC.toString())
                        && expectedSecurityInfo.getIdentity().equals(SecurityMode.NO_SEC.toString())
                        && Arrays.equals(SecurityMode.NO_SEC.toString().getBytes(), expectedSecurityInfo.getPreSharedKey())) {
                    expectedSecurityInfo = null;
                }
            } catch (LwM2MAuthException e) {
                log.info("Registration failed: FORBIDDEN, endpointId: [{}]", registration.getEndpoint());
                return null;
            }
        if (securityChecker.checkSecurityInfo(registration.getEndpoint(), senderIdentity, expectedSecurityInfo)) {
            return registration;
        } else {
            securityStore.remove(registration.getEndpoint(), registration.getId());
            return null;
        }
    }
}

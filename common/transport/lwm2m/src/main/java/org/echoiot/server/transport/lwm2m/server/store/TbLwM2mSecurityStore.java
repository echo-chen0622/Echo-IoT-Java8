package org.echoiot.server.transport.lwm2m.server.store;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.transport.lwm2m.server.client.LwM2MAuthException;
import org.echoiot.server.transport.lwm2m.server.uplink.LwM2mTypeServer;
import org.eclipse.leshan.core.SecurityMode;
import org.eclipse.leshan.server.security.NonUniqueSecurityInfoException;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.echoiot.server.transport.lwm2m.secure.LwM2mCredentialsSecurityInfoValidator;
import org.echoiot.server.transport.lwm2m.secure.TbLwM2MSecurityInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
public class TbLwM2mSecurityStore implements TbMainSecurityStore {

    private final TbEditableSecurityStore securityStore;
    private final LwM2mCredentialsSecurityInfoValidator validator;
    private final ConcurrentMap<String, Set<String>> endpointRegistrations = new ConcurrentHashMap<>();

    public TbLwM2mSecurityStore(TbEditableSecurityStore securityStore, LwM2mCredentialsSecurityInfoValidator validator) {
        this.securityStore = securityStore;
        this.validator = validator;
    }

    @Override
    public TbLwM2MSecurityInfo getTbLwM2MSecurityInfoByEndpoint(String endpoint) {
        return securityStore.getTbLwM2MSecurityInfoByEndpoint(endpoint);
    }

    /**
     * @param endpoint
     * @return : If SecurityMode == NO_SEC:
     * return SecurityInfo.newPreSharedKeyInfo(SecurityMode.NO_SEC.toString(), SecurityMode.NO_SEC.toString(),
     * SecurityMode.NO_SEC.toString().getBytes());
     */
    @Nullable
    @Override
    public SecurityInfo getByEndpoint(String endpoint) {
        @Nullable SecurityInfo securityInfo = securityStore.getByEndpoint(endpoint);
        if (securityInfo == null) {
            securityInfo = fetchAndPutSecurityInfo(endpoint);
        } else if (securityInfo.usePSK() && securityInfo.getEndpoint().equals(SecurityMode.NO_SEC.toString())
                && securityInfo.getIdentity().equals(SecurityMode.NO_SEC.toString())
                && Arrays.equals(SecurityMode.NO_SEC.toString().getBytes(), securityInfo.getPreSharedKey())) {
            return null;
        }
        return securityInfo;
    }

    @Nullable
    @Override
    public SecurityInfo getByIdentity(String pskIdentity) {
        @Nullable SecurityInfo securityInfo = securityStore.getByIdentity(pskIdentity);
        if (securityInfo == null) {
            try {
                securityInfo = fetchAndPutSecurityInfo(pskIdentity);
            } catch (LwM2MAuthException e) {
                log.trace("Registration failed: No pre-shared key found for [identity: {}]", pskIdentity);
                return null;
            }
        }
        return securityInfo;
    }

    @Nullable
    public SecurityInfo fetchAndPutSecurityInfo(String credentialsId) {
        @NotNull TbLwM2MSecurityInfo securityInfo = validator.getEndpointSecurityInfoByCredentialsId(credentialsId, LwM2mTypeServer.CLIENT);
        doPut(securityInfo);
        return securityInfo != null ? securityInfo.getSecurityInfo() : null;
    }

    private void doPut(@Nullable TbLwM2MSecurityInfo securityInfo) {
        if (securityInfo != null) {
            try {
                securityStore.put(securityInfo);
            } catch (NonUniqueSecurityInfoException e) {
                log.trace("Failed to add security info: {}", securityInfo, e);
            }
        }
    }

    @Override
    public void putX509(TbLwM2MSecurityInfo securityInfo) throws NonUniqueSecurityInfoException {
        securityStore.put(securityInfo);
    }

    @Override
    public void registerX509(String endpoint, String registrationId) {
        endpointRegistrations.computeIfAbsent(endpoint, ep -> new HashSet<>()).add(registrationId);
    }

    @Override
    public void remove(String endpoint, String registrationId) {
        Set<String> epRegistrationIds = endpointRegistrations.get(endpoint);
        boolean shouldRemove;
        if (epRegistrationIds == null) {
            shouldRemove = true;
        } else {
            epRegistrationIds.remove(registrationId);
            shouldRemove = epRegistrationIds.isEmpty();
        }
        if (shouldRemove) {
            securityStore.remove(endpoint);
        }
    }
}

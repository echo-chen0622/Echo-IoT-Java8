package org.echoiot.server.transport.lwm2m.bootstrap.secure;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.transport.TransportService;
import org.echoiot.server.transport.lwm2m.bootstrap.store.LwM2MBootstrapConfigStoreTaskProvider;
import org.echoiot.server.transport.lwm2m.bootstrap.store.LwM2MBootstrapSecurityStore;
import org.echoiot.server.transport.lwm2m.bootstrap.store.LwM2MBootstrapTaskProvider;
import org.echoiot.server.transport.lwm2m.server.client.LwM2MAuthException;
import org.eclipse.leshan.core.request.BootstrapDownlinkRequest;
import org.eclipse.leshan.core.request.BootstrapFinishRequest;
import org.eclipse.leshan.core.request.BootstrapRequest;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.server.bootstrap.*;
import org.eclipse.leshan.server.model.LwM2mBootstrapModelProvider;
import org.eclipse.leshan.server.model.StandardBootstrapModelProvider;
import org.eclipse.leshan.server.security.BootstrapSecurityStore;
import org.eclipse.leshan.server.security.SecurityChecker;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.echoiot.server.transport.lwm2m.utils.LwM2MTransportUtil.LOG_LWM2M_ERROR;
import static org.echoiot.server.transport.lwm2m.utils.LwM2MTransportUtil.LOG_LWM2M_INFO;

@Slf4j
public class LwM2mDefaultBootstrapSessionManager extends DefaultBootstrapSessionManager {

    private final BootstrapSecurityStore bsSecurityStore;
    private final SecurityChecker securityChecker;
    @NotNull
    private final LwM2MBootstrapTaskProvider tasksProvider;
    @NotNull
    private final LwM2mBootstrapModelProvider modelProvider;
    private TransportService transportService;

    /**
     * Create a {@link DefaultBootstrapSessionManager} using a default {@link SecurityChecker} to accept or refuse new
     * {@link BootstrapSession}.
     *
     * @param bsSecurityStore the {@link BootstrapSecurityStore} used by default {@link SecurityChecker}.
     */
    public LwM2mDefaultBootstrapSessionManager(BootstrapSecurityStore bsSecurityStore, BootstrapConfigStore configStore, TransportService transportService) {
        this(bsSecurityStore, new SecurityChecker(), new LwM2MBootstrapConfigStoreTaskProvider(configStore),
                new StandardBootstrapModelProvider());
        this.transportService = transportService;
    }

    /**
     * Create a {@link DefaultBootstrapSessionManager}.
     *
     * @param bsSecurityStore the {@link BootstrapSecurityStore} used by {@link SecurityChecker}.
     * @param securityChecker used to accept or refuse new {@link BootstrapSession}.
     */
    public LwM2mDefaultBootstrapSessionManager(BootstrapSecurityStore bsSecurityStore, SecurityChecker securityChecker,
                                               @NotNull LwM2MBootstrapTaskProvider tasksProvider, @NotNull LwM2mBootstrapModelProvider modelProvider) {
        super(bsSecurityStore, securityChecker, tasksProvider, modelProvider);
        this.bsSecurityStore = bsSecurityStore;
        this.securityChecker = securityChecker;
        this.tasksProvider = tasksProvider;
        this.modelProvider = modelProvider;
    }

    @NotNull
    @Override
    public BootstrapSession begin(@NotNull BootstrapRequest request, @NotNull Identity clientIdentity) {
        boolean authorized = true;
        @Nullable Iterator<SecurityInfo> securityInfos = null;
        try {
            if (bsSecurityStore != null && securityChecker != null) {
                if (clientIdentity.isPSK()) {
                    SecurityInfo securityInfo = bsSecurityStore.getByIdentity(clientIdentity.getPskIdentity());
                    securityInfos = Collections.singletonList(securityInfo).iterator();
                } else if (!clientIdentity.isX509()) {
                    securityInfos = bsSecurityStore.getAllByEndpoint(request.getEndpointName());
                }
                authorized = this.checkSecurityInfo(request.getEndpointName(), clientIdentity, securityInfos);
            }
        } catch (LwM2MAuthException e) {
            authorized = false;
        }
        @NotNull DefaultBootstrapSession session = new DefaultBootstrapSession(request, clientIdentity, authorized);
        if (authorized) {
            try {
                this.tasksProvider.put(session.getEndpoint());
            } catch (InvalidConfigurationException e){
                log.error("Failed put to lwM2MBootstrapSessionClients by endpoint [{}]", request.getEndpointName(), e);
            }
            this.sendLogs(request.getEndpointName(),
                    String.format("%s: Bootstrap session started...", LOG_LWM2M_INFO, request.getEndpointName()));
        }
        return session;
    }

    @Override
    public boolean hasConfigFor(BootstrapSession session) {
        BootstrapTaskProvider.Tasks firstTasks = this.tasksProvider.getTasks(session, null);
        if (firstTasks == null) {
            return false;
        }
        initTasks(session, firstTasks);
        return true;
    }

    protected void initTasks(BootstrapSession bssession, @NotNull BootstrapTaskProvider.Tasks tasks) {
        DefaultBootstrapSession session = (DefaultBootstrapSession) bssession;
        // set models
        if (tasks.supportedObjects != null)
            session.setModel(modelProvider.getObjectModel(session, tasks.supportedObjects));

        // set Requests to Send
        log.info("tasks.requestsToSend = [{}]", tasks.requestsToSend);
        session.setRequests(tasks.requestsToSend);

        // prepare list where we will store Responses
        session.setResponses(new ArrayList<LwM2mResponse>(tasks.requestsToSend.size()));

        // is last Tasks ?
        session.setMoreTasks(!tasks.last);
    }

    @Override
    public BootstrapDownlinkRequest<? extends LwM2mResponse> getFirstRequest(BootstrapSession bsSession) {
        return nextRequest(bsSession);
    }

    protected BootstrapDownlinkRequest<? extends LwM2mResponse> nextRequest(BootstrapSession bsSession) {
        DefaultBootstrapSession session = (DefaultBootstrapSession) bsSession;
        List<BootstrapDownlinkRequest<? extends LwM2mResponse>> requestsToSend = session.getRequests();

        if (!requestsToSend.isEmpty()) {
            // get next requests
            return requestsToSend.remove(0);
        } else {
            if (session.hasMoreTasks()) {
                BootstrapTaskProvider.Tasks nextTasks = this.tasksProvider.getTasks(session, session.getResponses());
                if (nextTasks == null) {
                    session.setMoreTasks(false);
                    return new BootstrapFinishRequest();
                }

                initTasks(session, nextTasks);
                return nextRequest(bsSession);
            } else {
                return new BootstrapFinishRequest();
            }
        }
    }

    @NotNull
    @Override
    public BootstrapPolicy onResponseSuccess(@NotNull BootstrapSession bsSession,
                                             BootstrapDownlinkRequest<? extends LwM2mResponse> request, @NotNull LwM2mResponse response) {
        if (!(request instanceof BootstrapFinishRequest)) {
            // store response
            DefaultBootstrapSession session = (DefaultBootstrapSession) bsSession;
            session.getResponses().add(response);
            String msg = String.format("%s: receives success response for:  %s  %s %s", LOG_LWM2M_INFO,
                                       request.getClass().getSimpleName(), request.getPath().toString(), response
                                      );
            this.sendLogs(bsSession.getEndpoint(), msg);

            // on success for NOT bootstrap finish request we send next request
            return BootstrapPolicy.continueWith(nextRequest(bsSession));
        } else {
            // on success for bootstrap finish request we stop the session
            this.sendLogs(bsSession.getEndpoint(),
                    String.format("%s: receives success response for bootstrap finish.", LOG_LWM2M_INFO));
            this.tasksProvider.remove(bsSession.getEndpoint());
            return BootstrapPolicy.finished();
        }
    }

    @NotNull
    @Override
    public BootstrapPolicy onResponseError(@NotNull BootstrapSession bsSession,
                                           BootstrapDownlinkRequest<? extends LwM2mResponse> request, @NotNull LwM2mResponse response) {
        if (!(request instanceof BootstrapFinishRequest)) {
            // store response
            DefaultBootstrapSession session = (DefaultBootstrapSession) bsSession;
            session.getResponses().add(response);
            this.sendLogs(bsSession.getEndpoint(),
                    String.format("%s: %s %s receives error response %s ", LOG_LWM2M_INFO,
                                  request.getClass().getSimpleName(),
                                  request.getPath().toString(), response
                                 ));
            // on response error for NOT bootstrap finish request we continue any sending next request
            return BootstrapPolicy.continueWith(nextRequest(bsSession));
        } else {
            // on response error for bootstrap finish request we stop the session
            this.sendLogs(bsSession.getEndpoint(),
                    String.format("%s: error response for request bootstrap finish. Stop the session: %s", LOG_LWM2M_ERROR, bsSession));
            this.tasksProvider.remove(bsSession.getEndpoint());
            return BootstrapPolicy.failed();
        }
    }

    @NotNull
    @Override
    public BootstrapPolicy onRequestFailure(@NotNull BootstrapSession bsSession,
                                            @NotNull BootstrapDownlinkRequest<? extends LwM2mResponse> request, @NotNull Throwable cause) {
        this.sendLogs(bsSession.getEndpoint(),
                String.format("%s: %s %s failed because of %s", LOG_LWM2M_ERROR, request.getClass().getSimpleName(),
                              request.getPath().toString(), cause
                             ));
        return BootstrapPolicy.failed();
    }

    @Override
    public void end(@NotNull BootstrapSession bsSession) {
        this.sendLogs(bsSession.getEndpoint(), String.format("%s: Bootstrap session finished.", LOG_LWM2M_INFO));
        this.tasksProvider.remove(bsSession.getEndpoint());
    }

    @Override
    public void failed(@NotNull BootstrapSession bsSession, @NotNull BootstrapFailureCause cause) {
        this.sendLogs(bsSession.getEndpoint(), String.format("%s: Bootstrap session failed because of %s", LOG_LWM2M_ERROR, cause));
        this.tasksProvider.remove(bsSession.getEndpoint());
    }

    private void sendLogs(String endpointName, String logMsg) {
        log.info("Endpoint: [{}] [{}]", endpointName, logMsg);
        transportService.log(((LwM2MBootstrapSecurityStore) bsSecurityStore).getSessionByEndpoint(endpointName), logMsg);
    }

    private boolean checkSecurityInfo(String endpoint, @NotNull Identity clientIdentity, Iterator<SecurityInfo> securityInfos) {
        if (clientIdentity.isX509()) {
            return clientIdentity.getX509CommonName().equals(endpoint)
                    & ((LwM2MBootstrapSecurityStore) bsSecurityStore).getBootstrapConfigByEndpoint(endpoint) != null;
        } else {
            return securityChecker.checkSecurityInfos(endpoint, clientIdentity, securityInfos);
        }
    }
}

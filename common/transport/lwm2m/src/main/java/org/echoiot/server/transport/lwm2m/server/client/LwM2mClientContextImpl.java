package org.echoiot.server.transport.lwm2m.server.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.DeviceProfile;
import org.echoiot.server.common.data.device.data.PowerMode;
import org.echoiot.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.echoiot.server.common.data.device.profile.lwm2m.OtherConfiguration;
import org.echoiot.server.common.data.id.DeviceProfileId;
import org.echoiot.server.common.transport.TransportDeviceProfileCache;
import org.echoiot.server.common.transport.TransportServiceCallback;
import org.echoiot.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.echoiot.server.gen.transport.TransportProtos;
import org.echoiot.server.queue.util.AfterStartUp;
import org.echoiot.server.queue.util.TbLwM2mTransportComponent;
import org.echoiot.server.transport.lwm2m.config.LwM2MTransportServerConfig;
import org.echoiot.server.transport.lwm2m.secure.TbLwM2MSecurityInfo;
import org.echoiot.server.transport.lwm2m.server.LwM2mTransportContext;
import org.echoiot.server.transport.lwm2m.server.model.LwM2MModelConfigService;
import org.echoiot.server.transport.lwm2m.server.ota.LwM2MOtaUpdateService;
import org.echoiot.server.transport.lwm2m.server.session.LwM2MSessionManager;
import org.echoiot.server.transport.lwm2m.server.store.TbLwM2MClientStore;
import org.echoiot.server.transport.lwm2m.server.store.TbMainSecurityStore;
import org.echoiot.server.transport.lwm2m.server.uplink.DefaultLwM2mUplinkMsgHandler;
import org.echoiot.server.transport.lwm2m.utils.LwM2MTransportUtil;
import org.eclipse.leshan.core.SecurityMode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.server.registration.Registration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static org.echoiot.server.transport.lwm2m.utils.LwM2MTransportUtil.convertObjectIdToVersionedId;
import static org.eclipse.leshan.core.SecurityMode.NO_SEC;

@Slf4j
@Service
@TbLwM2mTransportComponent
@RequiredArgsConstructor
public class LwM2mClientContextImpl implements LwM2mClientContext {

    @NotNull
    private final LwM2mTransportContext context;
    @NotNull
    private final LwM2MTransportServerConfig config;
    @NotNull
    private final TbMainSecurityStore securityStore;
    @NotNull
    private final TbLwM2MClientStore clientStore;
    @NotNull
    private final LwM2MSessionManager sessionManager;
    @NotNull
    private final TransportDeviceProfileCache deviceProfileCache;
    @NotNull
    private final LwM2MModelConfigService modelConfigService;

    @Resource
    @Lazy
    private DefaultLwM2mUplinkMsgHandler defaultLwM2MUplinkMsgHandler;
    @Resource
    @Lazy
    private LwM2MOtaUpdateService otaUpdateService;

    private final Map<String, LwM2mClient> lwM2mClientsByEndpoint = new ConcurrentHashMap<>();
    private final Map<String, LwM2mClient> lwM2mClientsByRegistrationId = new ConcurrentHashMap<>();
    private final Map<UUID, Lwm2mDeviceProfileTransportConfiguration> profiles = new ConcurrentHashMap<>();

    @AfterStartUp(order = AfterStartUp.BEFORE_TRANSPORT_SERVICE)
    public void init() {
        String nodeId = context.getNodeId();
        Set<LwM2mClient> fetchedClients = clientStore.getAll();
        log.debug("Fetched clients from store: {}", fetchedClients);
        fetchedClients.forEach(client -> {
            lwM2mClientsByEndpoint.put(client.getEndpoint(), client);
            try {
                client.lock();
                updateFetchedClient(nodeId, client);
            } finally {
                client.unlock();
            }
        });
    }

    @NotNull
    @Override
    public LwM2mClient getClientByEndpoint(String endpoint) {
        return lwM2mClientsByEndpoint.computeIfAbsent(endpoint, ep -> {
            LwM2mClient client = clientStore.get(ep);
            String nodeId = context.getNodeId();
            if (client == null) {
                log.info("[{}] initialized new client.", endpoint);
                client = new LwM2mClient(nodeId, ep);
            } else {
                log.debug("[{}] fetched client from store: {}", endpoint, client);
                updateFetchedClient(nodeId, client);
            }
            return client;
        });
    }

    private void updateFetchedClient(String nodeId, @NotNull LwM2mClient client) {
        boolean updated = false;
        if (client.getRegistration() != null) {
            lwM2mClientsByRegistrationId.put(client.getRegistration().getId(), client);
        }
        if (client.getSession() != null) {
            client.refreshSessionId(nodeId);
            sessionManager.register(client.getSession());
            updated = true;
        }
        if (updated) {
            clientStore.put(client);
        }
    }

    @NotNull
    @Override
    public Optional<TransportProtos.SessionInfoProto> register(@NotNull LwM2mClient client, @NotNull Registration registration) throws LwM2MClientStateException {
        TransportProtos.SessionInfoProto oldSession;
        client.lock();
        try {
            if (LwM2MClientState.UNREGISTERED.equals(client.getState())) {
                throw new LwM2MClientStateException(client.getState(), "Client is in invalid state.");
            }
            oldSession = client.getSession();
            TbLwM2MSecurityInfo securityInfo = securityStore.getTbLwM2MSecurityInfoByEndpoint(client.getEndpoint());
            if (securityInfo.getSecurityMode() != null) {
                if (SecurityMode.X509.equals(securityInfo.getSecurityMode())) {
                    securityStore.registerX509(registration.getEndpoint(), registration.getId());
                }
                if (securityInfo.getDeviceProfile() != null) {
                    profileUpdate(securityInfo.getDeviceProfile());
                    if (securityInfo.getSecurityInfo() != null) {
                        client.init(securityInfo.getMsg(), UUID.randomUUID());
                    } else if (NO_SEC.equals(securityInfo.getSecurityMode())) {
                        client.init(securityInfo.getMsg(), UUID.randomUUID());
                    } else {
                        throw new RuntimeException(String.format("Registration failed: device %s not found.", client.getEndpoint()));
                    }
                } else {
                    throw new RuntimeException(String.format("Registration failed: device %s not found.", client.getEndpoint()));
                }
            } else {
                throw new RuntimeException(String.format("Registration failed: FORBIDDEN, endpointId: %s", client.getEndpoint()));
            }
            client.setRegistration(registration);
            this.lwM2mClientsByRegistrationId.put(registration.getId(), client);
            client.setState(LwM2MClientState.REGISTERED);
            onUplink(client);
            if (!compareAndSetSleepFlag(client, false)) {
                clientStore.put(client);
            }
        } finally {
            client.unlock();
        }
        return Optional.ofNullable(oldSession);
    }

    @Override
    public boolean asleep(@NotNull LwM2mClient client) {
        boolean changed = compareAndSetSleepFlag(client, true);
        if (changed) {
            log.debug("[{}] client is sleeping", client.getEndpoint());
            context.getTransportService().log(client.getSession(), "Info : Client is sleeping!");
        }
        return changed;
    }

    @Override
    public boolean awake(@NotNull LwM2mClient client) {
        onUplink(client);
        boolean changed = compareAndSetSleepFlag(client, false);
        if (changed) {
            log.debug("[{}] client is awake", client.getEndpoint());
            context.getTransportService().log(client.getSession(), "Info : Client is awake!");
            sendMsgsAfterSleeping(client);
        }
        return changed;
    }

    private boolean compareAndSetSleepFlag(@NotNull LwM2mClient client, boolean sleeping) {
        if (sleeping == client.isAsleep()) {
            log.trace("[{}] Client is already at sleeping: {}, ignoring event: {}", client.getEndpoint(), client.isAsleep(), sleeping);
            return false;
        }
        client.lock();
        try {
            if (sleeping == client.isAsleep()) {
                log.trace("[{}] Client is already at sleeping: {}, ignoring event: {}", client.getEndpoint(), client.isAsleep(), sleeping);
                return false;
            } else {
                PowerMode powerMode = getPowerMode(client);
                if (PowerMode.PSM.equals(powerMode) || PowerMode.E_DRX.equals(powerMode)) {
                    log.trace("[{}] Switch sleeping from: {} to: {}", client.getEndpoint(), client.isAsleep(), sleeping);
                    client.setAsleep(sleeping);
                    update(client);
                    return true;
                } else {
                    return false;
                }
            }
        } finally {
            client.unlock();
        }
    }

    @Override
    public void updateRegistration(@NotNull LwM2mClient client, @NotNull Registration registration) throws LwM2MClientStateException {
        client.lock();
        try {
            if (!LwM2MClientState.REGISTERED.equals(client.getState())) {
                throw new LwM2MClientStateException(client.getState(), "Client is in invalid state.");
            }
            client.setRegistration(registration);
            if (!awake(client)) {
                clientStore.put(client);
            }
        } finally {
            client.unlock();
        }
    }

    @Override
    public void unregister(@NotNull LwM2mClient client, @NotNull Registration registration) throws LwM2MClientStateException {
        client.lock();
        try {
            if (!LwM2MClientState.REGISTERED.equals(client.getState())) {
                throw new LwM2MClientStateException(client.getState(), "Client is in invalid state.");
            }
            lwM2mClientsByRegistrationId.remove(registration.getId());
            Registration currentRegistration = client.getRegistration();
            if (currentRegistration.getId().equals(registration.getId())) {
                client.setState(LwM2MClientState.UNREGISTERED);
                lwM2mClientsByEndpoint.remove(client.getEndpoint());
//                TODO: change tests to use new certificate.
//                this.securityStore.remove(client.getEndpoint(), registration.getId());
                clientStore.remove(client.getEndpoint());
                modelConfigService.removeUpdates(client.getEndpoint());
                UUID profileId = client.getProfileId();
                if (profileId != null) {
                    @NotNull Optional<LwM2mClient> otherClients = lwM2mClientsByRegistrationId.values().stream().filter(e -> e.getProfileId().equals(profileId)).findFirst();
                    if (otherClients.isEmpty()) {
                        profiles.remove(profileId);
                    }
                }
            } else {
                throw new LwM2MClientStateException(client.getState(), "Client has different registration.");
            }
        } finally {
            client.unlock();
        }
    }

    @Nullable
    @Override
    public LwM2mClient getClientBySessionInfo(@NotNull TransportProtos.SessionInfoProto sessionInfo) {
        @Nullable LwM2mClient lwM2mClient = null;
        @NotNull UUID sessionId = new UUID(sessionInfo.getSessionIdMSB(), sessionInfo.getSessionIdLSB());
        @NotNull Predicate<LwM2mClient> isClientFilter =
                c -> c.getSession() != null && sessionId.equals((new UUID(c.getSession().getSessionIdMSB(), c.getSession().getSessionIdLSB())));
        if (this.lwM2mClientsByEndpoint.size() > 0) {
            lwM2mClient = this.lwM2mClientsByEndpoint.values().stream().filter(isClientFilter).findAny().orElse(null);
        }
        if (lwM2mClient == null && this.lwM2mClientsByRegistrationId.size() > 0) {
            lwM2mClient = this.lwM2mClientsByRegistrationId.values().stream().filter(isClientFilter).findAny().orElse(null);
        }
        if (lwM2mClient == null) {
            log.error("[{}] Failed to lookup client by session id.", sessionId);
        }
        return lwM2mClient;
    }

    @Override
    public String getObjectIdByKeyNameFromProfile(@NotNull LwM2mClient client, String keyName) {
        Lwm2mDeviceProfileTransportConfiguration profile = getProfile(client.getProfileId());
        for (@NotNull Map.Entry<String, String> entry : profile.getObserveAttr().getKeyName().entrySet()) {
            String k = entry.getKey();
            String v = entry.getValue();
            if (v.equals(keyName) && client.isValidObjectVersion(k).isEmpty()) {
                return k;
            }
        }
        throw new IllegalArgumentException(keyName + " is not configured in the device profile!");
    }

    public Registration getRegistration(String registrationId) {
        return this.lwM2mClientsByRegistrationId.get(registrationId).getRegistration();
    }

    @Override
    public void registerClient(@NotNull Registration registration, @NotNull ValidateDeviceCredentialsResponse credentials) {
        @NotNull LwM2mClient client = getClientByEndpoint(registration.getEndpoint());
        client.init(credentials, UUID.randomUUID());
        lwM2mClientsByRegistrationId.put(registration.getId(), client);
        profileUpdate(credentials.getDeviceProfile());
    }

    @Override
    public void update(@NotNull LwM2mClient client) {
        client.lock();
        try {
            if (client.getState().equals(LwM2MClientState.REGISTERED)) {
                clientStore.put(client);
            } else {
                log.error("[{}] Client is in invalid state: {}!", client.getEndpoint(), client.getState());
            }
        } finally {
            client.unlock();
        }
    }

    @Override
    public void sendMsgsAfterSleeping(@NotNull LwM2mClient lwM2MClient) {
        if (LwM2MClientState.REGISTERED.equals(lwM2MClient.getState())) {
            PowerMode powerMode = getPowerMode(lwM2MClient);
            if (PowerMode.PSM.equals(powerMode) || PowerMode.E_DRX.equals(powerMode)) {
                modelConfigService.sendUpdates(lwM2MClient);
                defaultLwM2MUplinkMsgHandler.initAttributes(lwM2MClient, false);
                TransportProtos.TransportToDeviceActorMsg persistentRpcRequestMsg = TransportProtos.TransportToDeviceActorMsg
                        .newBuilder()
                        .setSessionInfo(lwM2MClient.getSession())
                        .setSendPendingRPC(TransportProtos.SendPendingRPCMsg.newBuilder().build())
                        .build();
                context.getTransportService().process(persistentRpcRequestMsg, TransportServiceCallback.EMPTY);
                otaUpdateService.init(lwM2MClient);
            }
        }
    }

    private PowerMode getPowerMode(@NotNull LwM2mClient lwM2MClient) {
        PowerMode powerMode = lwM2MClient.getPowerMode();
        if (powerMode == null) {
            Lwm2mDeviceProfileTransportConfiguration deviceProfile = getProfile(lwM2MClient.getProfileId());
            powerMode = deviceProfile.getClientLwM2mSettings().getPowerMode();
        }
        return powerMode;
    }

    @NotNull
    @Override
    public Collection<LwM2mClient> getLwM2mClients() {
        return lwM2mClientsByEndpoint.values();
    }

    @Override
    public Lwm2mDeviceProfileTransportConfiguration getProfile(UUID profileId) {
        return doGetAndCache(profileId);
    }

    @Override
    public Lwm2mDeviceProfileTransportConfiguration getProfile(@NotNull Registration registration) {
        UUID profileId = getClientByEndpoint(registration.getEndpoint()).getProfileId();
        return doGetAndCache(profileId);
    }

    @Nullable
    private Lwm2mDeviceProfileTransportConfiguration doGetAndCache(UUID profileId) {

        Lwm2mDeviceProfileTransportConfiguration result = profiles.get(profileId);
        if (result == null) {
            log.debug("Fetching profile [{}]", profileId);
            DeviceProfile deviceProfile = deviceProfileCache.get(new DeviceProfileId(profileId));
            if (deviceProfile != null) {
                result = profileUpdate(deviceProfile);
            } else {
                log.warn("Device profile was not found! Most probably device profile [{}] has been removed from the database.", profileId);
            }
        }
        return result;
    }

    @NotNull
    @Override
    public Lwm2mDeviceProfileTransportConfiguration profileUpdate(@NotNull DeviceProfile deviceProfile) {
        @NotNull Lwm2mDeviceProfileTransportConfiguration clientProfile = LwM2MTransportUtil.toLwM2MClientProfile(deviceProfile);
        profiles.put(deviceProfile.getUuidId(), clientProfile);
        return clientProfile;
    }

    @Nullable
    @Override
    public Set<String> getSupportedIdVerInClient(@NotNull LwM2mClient client) {
        @NotNull Set<String> clientObjects = ConcurrentHashMap.newKeySet();
        Arrays.stream(client.getRegistration().getObjectLinks()).forEach(link -> {
            @NotNull LwM2mPath pathIds = new LwM2mPath(link.getUriReference());
            if (!pathIds.isRoot()) {
                clientObjects.add(convertObjectIdToVersionedId(link.getUriReference(), client.getRegistration()));
            }
        });
        return (clientObjects.size() > 0) ? clientObjects : null;
    }

    @Nullable
    @Override
    public LwM2mClient getClientByDeviceId(@NotNull UUID deviceId) {
        return lwM2mClientsByRegistrationId.values().stream().filter(e -> deviceId.equals(e.getDeviceId())).findFirst().orElse(null);
    }

    @Override
    public boolean isDownlinkAllowed(@NotNull LwM2mClient client) {
        PowerMode powerMode = client.getPowerMode();
        @Nullable OtherConfiguration profileSettings = null;
        if (powerMode == null) {
            var clientProfile = getProfile(client.getProfileId());
            profileSettings = clientProfile.getClientLwM2mSettings();
            powerMode = profileSettings.getPowerMode();
            if (powerMode == null) {
                powerMode = PowerMode.DRX;
            }
        }
        if (PowerMode.DRX.equals(powerMode) || otaUpdateService.isOtaDownloading(client)) {
            return true;
        }
        client.lock();
        long timeSinceLastUplink = System.currentTimeMillis() - client.getLastUplinkTime();
        try {
            if (PowerMode.PSM.equals(powerMode)) {
                Long psmActivityTimer = client.getPsmActivityTimer();
                if (psmActivityTimer == null && profileSettings != null) {
                    psmActivityTimer = profileSettings.getPsmActivityTimer();

                }
                if (psmActivityTimer == null || psmActivityTimer == 0L) {
                    psmActivityTimer = config.getPsmActivityTimer();
                }
                return timeSinceLastUplink <= psmActivityTimer;
            } else {
                Long pagingTransmissionWindow = client.getPagingTransmissionWindow();
                if (pagingTransmissionWindow == null && profileSettings != null) {
                    pagingTransmissionWindow = profileSettings.getPagingTransmissionWindow();

                }
                if (pagingTransmissionWindow == null || pagingTransmissionWindow == 0L) {
                    pagingTransmissionWindow = config.getPagingTransmissionWindow();
                }
                boolean allowed = timeSinceLastUplink <= pagingTransmissionWindow;
                if (!allowed) {
                    return client.checkFirstDownlink();
                } else {
                    return true;
                }
            }
        } finally {
            client.unlock();
        }
    }

    @Override
    public void onUplink(@NotNull LwM2mClient client) {
        PowerMode powerMode = client.getPowerMode();
        @Nullable OtherConfiguration profileSettings = null;
        if (powerMode == null) {
            var clientProfile = getProfile(client.getProfileId());
            profileSettings = clientProfile.getClientLwM2mSettings();
            powerMode = profileSettings.getPowerMode();
            if (powerMode == null) {
                powerMode = PowerMode.DRX;
            }
        }
        if (PowerMode.DRX.equals(powerMode)) {
            client.updateLastUplinkTime();
            return;
        }
        client.lock();
        try {
            long uplinkTime = client.updateLastUplinkTime();
            long timeout;
            if (PowerMode.PSM.equals(powerMode)) {
                Long psmActivityTimer = client.getPsmActivityTimer();
                if (psmActivityTimer == null && profileSettings != null) {
                    psmActivityTimer = profileSettings.getPsmActivityTimer();

                }
                if (psmActivityTimer == null || psmActivityTimer == 0L) {
                    psmActivityTimer = config.getPsmActivityTimer();
                }

                timeout = psmActivityTimer;
            } else {
                Long pagingTransmissionWindow = client.getPagingTransmissionWindow();
                if (pagingTransmissionWindow == null && profileSettings != null) {
                    pagingTransmissionWindow = profileSettings.getPagingTransmissionWindow();

                }
                if (pagingTransmissionWindow == null || pagingTransmissionWindow == 0L) {
                    pagingTransmissionWindow = config.getPagingTransmissionWindow();
                }
                timeout = pagingTransmissionWindow;
            }
            Future<Void> sleepTask = client.getSleepTask();
            if (sleepTask != null) {
                sleepTask.cancel(false);
            }
            Future<Void> task = context.getScheduler().schedule(() -> {
                if (uplinkTime == client.getLastUplinkTime() && !otaUpdateService.isOtaDownloading(client)) {
                    asleep(client);
                }
                return null;
            }, timeout, TimeUnit.MILLISECONDS);
            client.setSleepTask(task);
        } finally {
            client.unlock();
        }
    }

    @Override
    public Long getRequestTimeout(@NotNull LwM2mClient client) {
        @Nullable Long timeout = null;
        if (PowerMode.E_DRX.equals(client.getPowerMode()) && client.getEdrxCycle() != null) {
            timeout = client.getEdrxCycle();
        } else {
            var clientProfile = getProfile(client.getProfileId());
            OtherConfiguration clientLwM2mSettings = clientProfile.getClientLwM2mSettings();
            if (PowerMode.E_DRX.equals(clientLwM2mSettings.getPowerMode())) {
                timeout = clientLwM2mSettings.getEdrxCycle();
            }
        }
        if (timeout == null || timeout == 0L) {
            timeout = this.config.getTimeout();
        }
        return timeout;
    }

}

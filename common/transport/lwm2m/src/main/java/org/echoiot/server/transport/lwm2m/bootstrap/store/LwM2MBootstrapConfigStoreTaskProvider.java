package org.echoiot.server.transport.lwm2m.bootstrap.store;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.request.*;
import org.eclipse.leshan.core.response.BootstrapDiscoverResponse;
import org.eclipse.leshan.core.response.BootstrapReadResponse;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.server.bootstrap.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import static org.echoiot.server.transport.lwm2m.utils.LwM2MTransportUtil.BOOTSTRAP_DEFAULT_SHORT_ID;
import static org.eclipse.leshan.core.model.ResourceModel.Type.OPAQUE;
import static org.eclipse.leshan.server.bootstrap.BootstrapUtil.toWriteRequest;

@Slf4j
public class LwM2MBootstrapConfigStoreTaskProvider implements LwM2MBootstrapTaskProvider {

    @NotNull
    protected final ReadWriteLock readWriteLock;
    @NotNull
    protected final Lock writeLock;

    private final BootstrapConfigStore store;

    private Map<Integer, String> supportedObjects;

    /**
     * Map<sEndpoint, LwM2MBootstrapClientInstanceIds: securityInstances, serverInstances>
     */
    protected Map<String, LwM2MBootstrapClientInstanceIds> lwM2MBootstrapSessionClients;

    public LwM2MBootstrapConfigStoreTaskProvider(BootstrapConfigStore store) {
        this.store = store;
        this.lwM2MBootstrapSessionClients = new ConcurrentHashMap<>();
        readWriteLock = new ReentrantReadWriteLock();
        writeLock = readWriteLock.writeLock();
    }

    @Nullable
    @Override
    public Tasks getTasks(@NotNull BootstrapSession session, @Nullable List<LwM2mResponse> previousResponse) {
        BootstrapConfig config = store.get(session.getEndpoint(), session.getIdentity(), session);
        if (config == null) {
            return null;
        }
        if (previousResponse == null && shouldStartWithDiscover(config)) {
            @NotNull Tasks tasks = new Tasks();
            tasks.requestsToSend = new ArrayList<>(1);
            tasks.requestsToSend.add(new BootstrapDiscoverRequest());
            tasks.last = false;
            return tasks;
        } else {
            @NotNull Tasks tasks = new Tasks();
            if (this.supportedObjects == null) {
                initSupportedObjectsDefault();
            }
            // add supportedObjects
            tasks.supportedObjects = this.supportedObjects;
            // handle bootstrap discover response
            if (previousResponse != null) {
                if (previousResponse.get(0) instanceof BootstrapDiscoverResponse) {
                    BootstrapDiscoverResponse discoverResponse = (BootstrapDiscoverResponse) previousResponse.get(0);
                    if (discoverResponse.isSuccess()) {
                        this.initAfterBootstrapDiscover(discoverResponse);
                        findSecurityInstanceId(discoverResponse.getObjectLinks(), session.getEndpoint());
                    } else {
                        log.warn(
                                "Bootstrap Discover return error {} : to continue bootstrap session without autoIdForSecurityObject mode. {}",
                                discoverResponse, session);
                    }
                    if (this.lwM2MBootstrapSessionClients.get(session.getEndpoint()).getSecurityInstances().get(BOOTSTRAP_DEFAULT_SHORT_ID) == null) {
                        log.error(
                                "Unable to find bootstrap server instance in Security Object (0) in response {}: unable to continue bootstrap session with autoIdForSecurityObject mode. {}",
                                discoverResponse, session);
                        return null;
                    }
                    tasks.requestsToSend = new ArrayList<>(1);
                    tasks.requestsToSend.add(new BootstrapReadRequest("/1"));
                    tasks.last = false;
                    return tasks;
                }
                BootstrapReadResponse readResponse = (BootstrapReadResponse) previousResponse.get(0);
                @Nullable Integer bootstrapServerIdOld = null;
                if (readResponse.isSuccess()) {
                    findServerInstanceId(readResponse, session.getEndpoint());
                    if (this.lwM2MBootstrapSessionClients.get(session.getEndpoint()).getSecurityInstances().size() > 0 && this.lwM2MBootstrapSessionClients.get(session.getEndpoint()).getServerInstances().size() > 0) {
                        bootstrapServerIdOld = this.findBootstrapServerId(session.getEndpoint());
                    }
                } else {
                    log.warn(
                            "Bootstrap ReadResponse return error {} : to continue bootstrap session without find Server Instance Id. {}",
                            readResponse, session);
                }
                // create requests from config
                tasks.requestsToSend = this.toRequests(config,
                        config.contentFormat != null ? config.contentFormat : session.getContentFormat(),
                        bootstrapServerIdOld, session.getEndpoint());
            } else {
                // create requests from config
                tasks.requestsToSend = BootstrapUtil.toRequests(config,
                        config.contentFormat != null ? config.contentFormat : session.getContentFormat());
            }
            return tasks;
        }
    }

    protected boolean shouldStartWithDiscover(@NotNull BootstrapConfig config) {
        return config.autoIdForSecurityObject;
    }

    /**
     * "Short Server ID": This Resource MUST be set when the Bootstrap-Server Resource has a value of 'false'.
     * The values ID:0 and ID:65535 values MUST NOT be used for identifying the LwM2M Server.
     * "Short Server ID":
     * - Link Instance (lwm2m Server) hase linkParams with key = "ssid" value = "shortId" (ver lvm2m = 1.1).
     * - Link Instance (bootstrap Server) hase not linkParams with key = "ssid" (ver lvm2m = 1.1).
     */
    protected void findSecurityInstanceId(@NotNull Link[] objectLinks, String endpoint) {
        log.info("Object after discover: [{}]", objectLinks);
        for (@NotNull Link link : objectLinks) {
            if (link.getUriReference().startsWith("/0/")) {
                try {
                    @NotNull LwM2mPath path = new LwM2mPath(link.getUriReference());
                    if (path.isObjectInstance()) {
                        if (link.getLinkParams().containsKey("ssid")) {
                            int serverId = Integer.parseInt(link.getLinkParams().get("ssid").getUnquoted());
                            if (!lwM2MBootstrapSessionClients.get(endpoint).getSecurityInstances().containsKey(serverId)) {
                                lwM2MBootstrapSessionClients.get(endpoint).getSecurityInstances().put(serverId, path.getObjectInstanceId());
                            } else {
                                log.error("Invalid lwm2mSecurityInstance by [{}]", path.getObjectInstanceId());
                            }
                            lwM2MBootstrapSessionClients.get(endpoint).getSecurityInstances().put(Integer.valueOf(link.getLinkParams().get("ssid").getUnquoted()), path.getObjectInstanceId());
                        } else {
                            if (!this.lwM2MBootstrapSessionClients.get(endpoint).getSecurityInstances().containsKey(0)) {
                                this.lwM2MBootstrapSessionClients.get(endpoint).getSecurityInstances().put(BOOTSTRAP_DEFAULT_SHORT_ID, path.getObjectInstanceId());
                            } else {
                                log.error("Invalid bootstrapSecurityInstance by [{}]", path.getObjectInstanceId());
                            }
                        }
                    }
                } catch (Exception e) {
                    // ignore if this is not a LWM2M path
                    log.error("Invalid LwM2MPath starting by \"/0/\"");
                }
            }
        }
    }

    protected void findServerInstanceId(@NotNull BootstrapReadResponse readResponse, String endpoint) {
        try {
            ((LwM2mObject) readResponse.getContent()).getInstances().values().forEach(instance -> {
                var shId = OPAQUE.equals(instance.getResource(0).getType()) ? new BigInteger((byte[]) instance.getResource(0).getValue()).intValue() : instance.getResource(0).getValue();
                int shortId;
                if (shId instanceof Long) {
                    shortId = ((Long) shId).intValue();
                } else {
                    shortId = (int) shId;
                }
                this.lwM2MBootstrapSessionClients.get(endpoint).getServerInstances().put(shortId, instance.getId());
            });
        } catch (Exception e) {
            log.error("Failed find Server Instance Id. ", e);
        }
    }

    @Nullable
    protected Integer findBootstrapServerId(String endpoint) {
        @Nullable Integer bootstrapServerIdOld = null;
        @NotNull Map<Integer, Integer> filteredMap = this.lwM2MBootstrapSessionClients.get(endpoint).getServerInstances().entrySet()
                                                                                      .stream().filter(x -> !this.lwM2MBootstrapSessionClients.get(endpoint).getSecurityInstances().containsKey(x.getKey()))
                                                                                      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (filteredMap.size() > 0) {
            bootstrapServerIdOld = filteredMap.keySet().stream().findFirst().get();
        }
        return bootstrapServerIdOld;
    }

    public BootstrapConfigStore getStore() {
        return this.store;
    }

    private void initAfterBootstrapDiscover(@NotNull BootstrapDiscoverResponse response) {
        Link[] links = response.getObjectLinks();
        Arrays.stream(links).forEach(link -> {
            @NotNull LwM2mPath path = new LwM2mPath(link.getUriReference());
            if (!path.isRoot() && path.getObjectId() < 3) {
                if (path.isObject()) {
                    String ver = link.getLinkParams().get("ver") != null ? link.getLinkParams().get("ver").getUnquoted() : "1.0";
                    this.supportedObjects.put(path.getObjectId(), ver);
                }
            }
        });
    }


    @NotNull
    public List<BootstrapDownlinkRequest<? extends LwM2mResponse>> toRequests(@NotNull BootstrapConfig bootstrapConfig,
                                                                              ContentFormat contentFormat,
                                                                              Integer bootstrapServerIdOld,
                                                                              String endpoint) {
        @NotNull List<BootstrapDownlinkRequest<? extends LwM2mResponse>> requests = new ArrayList<>();
        @NotNull Set<String> pathsDelete = new HashSet<>();
        @NotNull List<BootstrapDownlinkRequest<? extends LwM2mResponse>> requestsWrite = new ArrayList<>();
        boolean isBsServer = false;
        boolean isLwServer = false;
        /** Map<serverId ("Short Server ID"), InstanceId> */
        @NotNull Map<Integer, Integer> instances = new HashMap<>();
        @Nullable Integer bootstrapServerIdNew = null;
        // handle security
        int lwm2mSecurityInstanceId = 0;
        int bootstrapSecurityInstanceId = this.lwM2MBootstrapSessionClients.get(endpoint).getSecurityInstances().get(BOOTSTRAP_DEFAULT_SHORT_ID);
        for (@NotNull BootstrapConfig.ServerSecurity security : new TreeMap<>(bootstrapConfig.security).values()) {
            if (security.bootstrapServer) {
                requestsWrite.add(toWriteRequest(bootstrapSecurityInstanceId, security, contentFormat));
                isBsServer = true;
                bootstrapServerIdNew = security.serverId;
                instances.put(security.serverId, bootstrapSecurityInstanceId);
            } else {
                if (lwm2mSecurityInstanceId == bootstrapSecurityInstanceId) {
                    lwm2mSecurityInstanceId++;
                }
                requestsWrite.add(toWriteRequest(lwm2mSecurityInstanceId, security, contentFormat));
                instances.put(security.serverId, lwm2mSecurityInstanceId);
                isLwServer = true;
                if (!isBsServer && this.lwM2MBootstrapSessionClients.get(endpoint).getSecurityInstances().containsKey(security.serverId) &&
                        lwm2mSecurityInstanceId != this.lwM2MBootstrapSessionClients.get(endpoint).getSecurityInstances().get(security.serverId)) {
                    pathsDelete.add("/0/" + this.lwM2MBootstrapSessionClients.get(endpoint).getSecurityInstances().get(security.serverId));
                }
                /**
                 * If there is an instance in the serverInstances with serverId which we replace in the securityInstances
                 */
                // find serverId in securityInstances by id (instance)
                @Nullable Integer serverIdOld = null;
                for (@NotNull Map.Entry<Integer, Integer> entry : this.lwM2MBootstrapSessionClients.get(endpoint).getSecurityInstances().entrySet()) {
                    if (entry.getValue().equals(lwm2mSecurityInstanceId)) {
                        serverIdOld = entry.getKey();
                    }
                }
                if (!isBsServer && serverIdOld != null && this.lwM2MBootstrapSessionClients.get(endpoint).getServerInstances().containsKey(serverIdOld)) {
                    pathsDelete.add("/1/" + this.lwM2MBootstrapSessionClients.get(endpoint).getServerInstances().get(serverIdOld));
                }
                lwm2mSecurityInstanceId++;
            }
        }
        // handle server
        for (@NotNull Map.Entry<Integer, BootstrapConfig.ServerConfig> server : bootstrapConfig.servers.entrySet()) {
            int securityInstanceId = instances.get(server.getValue().shortId);
            requestsWrite.add(toWriteRequest(securityInstanceId, server.getValue(), contentFormat));
            if (!isBsServer) {
                /** Delete instance if bootstrapServerIdNew not equals bootstrapServerIdOld or securityInstanceBsIdNew not equals serverInstanceBsIdOld */
                if (bootstrapServerIdNew != null && server.getValue().shortId == bootstrapServerIdNew &&
                        (bootstrapServerIdNew != bootstrapServerIdOld || securityInstanceId != this.lwM2MBootstrapSessionClients.get(endpoint).getServerInstances().get(bootstrapServerIdOld))) {
                    pathsDelete.add("/1/" + this.lwM2MBootstrapSessionClients.get(endpoint).getServerInstances().get(bootstrapServerIdOld));
                    /** Delete instance if serverIdNew is present in serverInstances and  securityInstanceIdOld by serverIdNew not equals serverInstanceIdOld */
                } else if (this.lwM2MBootstrapSessionClients.get(endpoint).getServerInstances().containsKey(server.getValue().shortId) &&
                        securityInstanceId != this.lwM2MBootstrapSessionClients.get(endpoint).getServerInstances().get(server.getValue().shortId)) {
                    pathsDelete.add("/1/" + this.lwM2MBootstrapSessionClients.get(endpoint).getServerInstances().get(server.getValue().shortId));
                }
            }
        }
        // handle acl
        for (@NotNull Map.Entry<Integer, BootstrapConfig.ACLConfig> acl : bootstrapConfig.acls.entrySet()) {
            requestsWrite.add(toWriteRequest(acl.getKey(), acl.getValue(), contentFormat));
        }
        // handle delete
        if (isBsServer && isLwServer) {
            requests.add(new BootstrapDeleteRequest("/0"));
            requests.add(new BootstrapDeleteRequest("/1"));
        } else {
            pathsDelete.forEach(pathDelete -> requests.add(new BootstrapDeleteRequest(pathDelete)));
        }
        // handle write
        if (requestsWrite.size() > 0) {
            requests.addAll(requestsWrite);
        }
        return (requests);
    }


    private void initSupportedObjectsDefault() {
        this.supportedObjects = new HashMap<>();
        this.supportedObjects.put(0, "1.1");
        this.supportedObjects.put(1, "1.1");
        this.supportedObjects.put(2, "1.0");
    }

    @Override
    public void remove(String endpoint) {
        writeLock.lock();
        try {
            this.lwM2MBootstrapSessionClients.remove(endpoint);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void put(String endpoint) throws InvalidConfigurationException {
        writeLock.lock();
        try {
            this.lwM2MBootstrapSessionClients.put(endpoint, new LwM2MBootstrapClientInstanceIds());
        } finally {
            writeLock.unlock();
        }
    }
}

package org.echoiot.server.transport.lwm2m.server;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.transport.lwm2m.server.ota.DefaultLwM2MOtaUpdateService;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.observe.ObserveRelation;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.californium.core.server.resources.ResourceObserver;
import org.echoiot.server.cache.ota.OtaPackageDataCache;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class LwM2mTransportCoapResource extends AbstractLwM2mTransportResource {
    private final ConcurrentMap<String, ObserveRelation> tokenToObserveRelationMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, AtomicInteger> tokenToObserveNotificationSeqMap = new ConcurrentHashMap<>();
    private final OtaPackageDataCache otaPackageDataCache;

    public LwM2mTransportCoapResource(OtaPackageDataCache otaPackageDataCache, String name) {
        super(name);
        this.otaPackageDataCache = otaPackageDataCache;
        this.setObservable(true); // enable observing
        this.addObserver(new CoapResourceObserver());
    }


    @Override
    public void checkObserveRelation(@NotNull Exchange exchange, @NotNull Response response) {
        @NotNull String token = getTokenFromRequest(exchange.getRequest());
        final ObserveRelation relation = exchange.getRelation();
        if (relation == null || relation.isCanceled()) {
            return; // because request did not try to establish a relation
        }
        if (response.getCode().isSuccess()) {

            if (!relation.isEstablished()) {
                relation.setEstablished();
                addObserveRelation(relation);
            }
            @NotNull AtomicInteger notificationCounter = tokenToObserveNotificationSeqMap.computeIfAbsent(token, s -> new AtomicInteger(0));
            response.getOptions().setObserve(notificationCounter.getAndIncrement());
        } // ObserveLayer takes care of the else case
    }


    @Override
    protected void processHandleGet(@NotNull CoapExchange exchange) {
        log.debug("processHandleGet [{}]", exchange);
        List<String> uriPath = exchange.getRequestOptions().getUriPath();
        if (uriPath.size() >= 2 &&
                (DefaultLwM2MOtaUpdateService.FIRMWARE_UPDATE_COAP_RESOURCE.equals(uriPath.get(uriPath.size() - 2)) ||
                 DefaultLwM2MOtaUpdateService.SOFTWARE_UPDATE_COAP_RESOURCE.equals(uriPath.get(uriPath.size() - 2)))) {
            this.sendOtaData(exchange);
        }
    }

    @Override
    protected void processHandlePost(CoapExchange exchange) {
        log.debug("processHandlePost [{}]", exchange);
    }

    /**
     * Override the default behavior so that requests to sub resources (typically /{name}/{token}) are handled by
     * /name resource.
     */
    @NotNull
    @Override
    public Resource getChild(String name) {
        return this;
    }


    @NotNull
    private String getTokenFromRequest(@NotNull Request request) {
        return (request.getSourceContext() != null ? request.getSourceContext().getPeerAddress().getAddress().getHostAddress() : "null")
                + ":" + (request.getSourceContext() != null ? request.getSourceContext().getPeerAddress().getPort() : -1) + ":" + request.getTokenString();
    }

    public class CoapResourceObserver implements ResourceObserver {

        @Override
        public void changedName(String old) {

        }

        @Override
        public void changedPath(String old) {

        }

        @Override
        public void addedChild(Resource child) {

        }

        @Override
        public void removedChild(Resource child) {

        }

        @Override
        public void addedObserveRelation(ObserveRelation relation) {

        }

        @Override
        public void removedObserveRelation(ObserveRelation relation) {

        }
    }

    private void sendOtaData(@NotNull CoapExchange exchange) {
        String idStr = exchange.getRequestOptions().getUriPath().get(exchange.getRequestOptions().getUriPath().size() - 1
        );
        @NotNull UUID currentId = UUID.fromString(idStr);
        @NotNull Response response = new Response(CoAP.ResponseCode.CONTENT);
        byte[] otaData = this.getOtaData(currentId);
        if (otaData != null && otaData.length > 0) {
            log.debug("Read ota data (length): [{}]", otaData.length);
            response.setPayload(otaData);
            if (exchange.getRequestOptions().getBlock2() != null) {
                int chunkSize = exchange.getRequestOptions().getBlock2().getSzx();
                boolean lastFlag = otaData.length <= chunkSize;
                response.getOptions().setBlock2(chunkSize, lastFlag, 0);
                log.trace("With block2 Send currentId: [{}], length: [{}], chunkSize [{}], moreFlag [{}]", currentId, otaData.length, chunkSize, lastFlag);
            } else {
                log.trace("With block1 Send currentId: [{}], length: [{}], ", currentId, otaData.length);
            }
            exchange.respond(response);
        } else {
            log.trace("Ota packaged currentId: [{}] is not found.", currentId);
        }
    }

    private byte[] getOtaData(@NotNull UUID currentId) {
        return otaPackageDataCache.get(currentId.toString());
    }

}

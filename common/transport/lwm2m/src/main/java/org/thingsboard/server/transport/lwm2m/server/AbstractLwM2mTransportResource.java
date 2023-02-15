package org.thingsboard.server.transport.lwm2m.server;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.leshan.core.californium.LwM2mCoapResource;

@Slf4j
public abstract class AbstractLwM2mTransportResource extends LwM2mCoapResource {

    public AbstractLwM2mTransportResource(String name) {
        super(name);
    }

    @Override
    public void handleGET(CoapExchange exchange) {
        processHandleGet(exchange);
    }

    @Override
    public void handlePOST(CoapExchange exchange) {
        processHandlePost(exchange);
    }

    protected abstract void processHandleGet(CoapExchange exchange);

    protected abstract void processHandlePost(CoapExchange exchange);

}

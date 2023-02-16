package org.echoiot.server.transport.coap.callback;

import org.echoiot.server.common.transport.TransportServiceCallback;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.server.resources.CoapExchange;

public class CoapNoOpCallback implements TransportServiceCallback<Void> {
    private final CoapExchange exchange;

    public CoapNoOpCallback(CoapExchange exchange) {
        this.exchange = exchange;
    }

    @Override
    public void onSuccess(Void msg) {
    }

    @Override
    public void onError(Throwable e) {
        exchange.respond(CoAP.ResponseCode.INTERNAL_SERVER_ERROR);
    }
}

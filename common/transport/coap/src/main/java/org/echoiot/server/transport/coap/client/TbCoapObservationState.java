package org.echoiot.server.transport.coap.client;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.eclipse.californium.core.observe.ObserveRelation;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicInteger;

@Data
@RequiredArgsConstructor
public class TbCoapObservationState {

    @NotNull
    private final CoapExchange exchange;
    @NotNull
    private final String token;
    private final AtomicInteger observeCounter = new AtomicInteger(0);
    private volatile ObserveRelation observeRelation;

}

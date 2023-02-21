package org.echoiot.server.common.transport.limits;

import org.jetbrains.annotations.NotNull;

public class DummyTransportRateLimit implements TransportRateLimit {

    @NotNull
    @Override
    public String getConfiguration() {
        return "";
    }

    @Override
    public boolean tryConsume(long number) {
        return true;
    }

    @Override
    public boolean tryConsume() {
        return true;
    }

}

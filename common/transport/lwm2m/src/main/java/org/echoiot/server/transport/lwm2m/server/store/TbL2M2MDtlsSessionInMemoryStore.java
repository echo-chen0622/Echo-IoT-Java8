package org.echoiot.server.transport.lwm2m.server.store;

import org.echoiot.server.transport.lwm2m.secure.TbX509DtlsSessionInfo;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;

public class TbL2M2MDtlsSessionInMemoryStore implements TbLwM2MDtlsSessionStore {

    private final ConcurrentHashMap<String, TbX509DtlsSessionInfo> store = new ConcurrentHashMap<>();

    @Override
    public void put(@NotNull String endpoint, @NotNull TbX509DtlsSessionInfo msg) {
        store.put(endpoint, msg);
    }

    @Override
    public TbX509DtlsSessionInfo get(String endpoint) {
        return store.get(endpoint);
    }

    @Override
    public void remove(@NotNull String endpoint) {
        store.remove(endpoint);
    }
}

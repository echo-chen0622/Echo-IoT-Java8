package org.echoiot.server.transport.lwm2m.server.store;

import org.echoiot.server.transport.lwm2m.server.client.LwM2mClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Set;

public class TbDummyLwM2MClientStore implements TbLwM2MClientStore {
    @Nullable
    @Override
    public LwM2mClient get(String endpoint) {
        return null;
    }

    @NotNull
    @Override
    public Set<LwM2mClient> getAll() {
        return Collections.emptySet();
    }

    @Override
    public void put(LwM2mClient client) {

    }

    @Override
    public void remove(String endpoint) {

    }
}

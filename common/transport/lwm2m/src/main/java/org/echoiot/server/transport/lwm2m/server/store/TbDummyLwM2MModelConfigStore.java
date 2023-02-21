package org.echoiot.server.transport.lwm2m.server.store;

import org.echoiot.server.transport.lwm2m.server.model.LwM2MModelConfig;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class TbDummyLwM2MModelConfigStore implements TbLwM2MModelConfigStore {
    @NotNull
    @Override
    public List<LwM2MModelConfig> getAll() {
        return Collections.emptyList();
    }

    @Override
    public void put(LwM2MModelConfig modelConfig) {

    }

    @Override
    public void remove(String endpoint) {

    }
}

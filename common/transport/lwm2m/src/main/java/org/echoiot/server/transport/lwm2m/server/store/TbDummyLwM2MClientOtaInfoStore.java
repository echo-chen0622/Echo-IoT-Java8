package org.echoiot.server.transport.lwm2m.server.store;

import org.echoiot.server.transport.lwm2m.server.ota.firmware.LwM2MClientFwOtaInfo;
import org.echoiot.server.transport.lwm2m.server.ota.software.LwM2MClientSwOtaInfo;
import org.jetbrains.annotations.Nullable;

public class TbDummyLwM2MClientOtaInfoStore implements TbLwM2MClientOtaInfoStore {

    @Nullable
    @Override
    public LwM2MClientFwOtaInfo getFw(String endpoint) {
        return null;
    }

    @Nullable
    @Override
    public LwM2MClientSwOtaInfo getSw(String endpoint) {
        return null;
    }

    @Override
    public void putFw(LwM2MClientFwOtaInfo info) {

    }

    @Override
    public void putSw(LwM2MClientSwOtaInfo info) {

    }
}

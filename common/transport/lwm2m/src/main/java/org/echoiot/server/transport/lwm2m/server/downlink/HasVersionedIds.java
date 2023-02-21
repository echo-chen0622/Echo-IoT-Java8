package org.echoiot.server.transport.lwm2m.server.downlink;

import org.echoiot.server.transport.lwm2m.utils.LwM2MTransportUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public interface HasVersionedIds {

    String[] getVersionedIds();

    default String[] getObjectIds() {
        @NotNull Set<String> objectIds = ConcurrentHashMap.newKeySet();
        for (String versionedId : getVersionedIds()) {
            objectIds.add(LwM2MTransportUtil.fromVersionedIdToObjectId(versionedId));
        }
        return objectIds.toArray(String[]::new);
    }

}

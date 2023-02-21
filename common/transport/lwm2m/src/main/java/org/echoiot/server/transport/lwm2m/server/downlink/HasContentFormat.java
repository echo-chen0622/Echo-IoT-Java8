package org.echoiot.server.transport.lwm2m.server.downlink;

import org.eclipse.leshan.core.request.ContentFormat;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public interface HasContentFormat {

    Optional<ContentFormat> getRequestContentFormat();

    @Nullable
    default ContentFormat getResponseContentFormat() {
        return null;
    }
}

package org.echoiot.server.transport.coap.adaptors;

import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.transport.adaptor.AdaptorException;
import org.echoiot.server.gen.transport.TransportProtos;
import org.eclipse.californium.core.coap.Request;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CoapAdaptorUtils {

    public static TransportProtos.GetAttributeRequestMsg toGetAttributeRequestMsg(@NotNull Request inbound) throws AdaptorException {
        List<String> queryElements = inbound.getOptions().getUriQuery();
        TransportProtos.GetAttributeRequestMsg.Builder result = TransportProtos.GetAttributeRequestMsg.newBuilder();
        if (queryElements != null && queryElements.size() > 0) {
            @Nullable Set<String> clientKeys = toKeys(queryElements, "clientKeys");
            @Nullable Set<String> sharedKeys = toKeys(queryElements, "sharedKeys");
            if (clientKeys != null) {
                result.addAllClientAttributeNames(clientKeys);
            }
            if (sharedKeys != null) {
                result.addAllSharedAttributeNames(sharedKeys);
            }
        }
        result.setOnlyShared(false);
        return result.build();
    }

    @Nullable
    private static Set<String> toKeys(@NotNull List<String> queryElements, String attributeName) throws AdaptorException {
        @Nullable String keys = null;
        for (@NotNull String queryElement : queryElements) {
            @NotNull String[] queryItem = queryElement.split("=");
            if (queryItem.length == 2 && queryItem[0].equals(attributeName)) {
                keys = queryItem[1];
            }
        }
        if (keys != null && !StringUtils.isEmpty(keys)) {
            return new HashSet<>(Arrays.asList(keys.split(",")));
        } else {
            return null;
        }
    }
}

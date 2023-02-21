package org.echoiot.server.coapserver;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.coap.OptionSet;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.server.DelivererException;
import org.eclipse.californium.core.server.ServerMessageDeliverer;
import org.eclipse.californium.core.server.resources.Resource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Slf4j
public class TbCoapServerMessageDeliverer extends ServerMessageDeliverer {

    public TbCoapServerMessageDeliverer(Resource root) {
        super(root);
    }

    @Override
    protected Resource findResource(@NotNull Exchange exchange) throws DelivererException {
        validateUriPath(exchange);
        return findResource(exchange.getRequest().getOptions().getUriPath());
    }

    private void validateUriPath(@NotNull Exchange exchange) {
        OptionSet options = exchange.getRequest().getOptions();
        List<String> uriPathList = options.getUriPath();
        @Nullable String path = toPath(uriPathList);
        if (path != null) {
            options.setUriPath(path);
            exchange.getRequest().setOptions(options);
        }
    }

    @Nullable
    private String toPath(@NotNull List<String> list) {
        if (!CollectionUtils.isEmpty(list) && list.size() == 1) {
            @NotNull final String slash = "/";
            String path = list.get(0);
            if (path.startsWith(slash)) {
                path = path.substring(slash.length());
            }
            return path;
        }
        return null;
    }

}

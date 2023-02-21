package org.echoiot.server.transport.lwm2m.server.downlink;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.transport.lwm2m.server.client.LwM2mClient;
import org.echoiot.server.transport.lwm2m.server.log.LwM2MTelemetryLogService;
import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

import static org.echoiot.server.transport.lwm2m.utils.LwM2MTransportUtil.LOG_LWM2M_ERROR;
import static org.echoiot.server.transport.lwm2m.utils.LwM2MTransportUtil.LOG_LWM2M_INFO;

@Slf4j
public abstract class TbLwM2MTargetedCallback<R, T> extends AbstractTbLwM2MRequestCallback<R, T> {

    @Nullable
    protected final String versionedId;
    @Nullable
    protected final String[] versionedIds;

    public TbLwM2MTargetedCallback(LwM2MTelemetryLogService logService, LwM2mClient client, String versionedId) {
        super(logService, client);
        this.versionedId = versionedId;
        this.versionedIds = null;
    }

    public TbLwM2MTargetedCallback(LwM2MTelemetryLogService logService, LwM2mClient client, String[] versionedIds) {
        super(logService, client);
        this.versionedId = null;
        this.versionedIds = versionedIds;
    }

    @Override
    public void onSuccess(@NotNull R request, T response) {
        //TODO convert camelCase to "camel case" using .split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])")
        if (response instanceof LwM2mResponse) {
            logForBadResponse(((LwM2mResponse) response).getCode().getCode(), response.toString(), request.getClass().getSimpleName());
        }
    }

    public void logForBadResponse(int code, String responseStr, String requestName) {
        if (code > ResponseCode.CONTENT_CODE) {
            log.error("[{}] [{}] [{}] failed to process successful response [{}] ", client.getEndpoint(), requestName,
                    versionedId != null ? versionedId : Arrays.toString(versionedIds), responseStr);
            logService.log(client, String.format("[%s]: %s [%s] failed to process successful. Result: %s", LOG_LWM2M_ERROR,
                    requestName, versionedId != null ? versionedId : Arrays.toString(versionedIds), responseStr));
        } else {
            log.trace("[{}] {} [{}] successful: {}", client.getEndpoint(), requestName, versionedId != null ?
                    versionedId : versionedIds, responseStr);
            logService.log(client, String.format("[%s]: %s [%s] successful. Result: %s", LOG_LWM2M_INFO, requestName,
                    versionedId != null ? versionedId : Arrays.toString(versionedIds), responseStr));
        }
    }
}

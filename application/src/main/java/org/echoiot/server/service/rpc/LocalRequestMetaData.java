package org.echoiot.server.service.rpc;

import lombok.Data;
import org.echoiot.server.service.security.model.SecurityUser;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;
import org.echoiot.server.common.msg.rpc.ToDeviceRpcRequest;

/**
 * Created by Echo on 16.04.18.
 */
@Data
public class LocalRequestMetaData {
    @NotNull
    private final ToDeviceRpcRequest request;
    @NotNull
    private final SecurityUser user;
    @NotNull
    private final DeferredResult<ResponseEntity> responseWriter;
}

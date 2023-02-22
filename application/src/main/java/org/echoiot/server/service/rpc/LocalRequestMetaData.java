package org.echoiot.server.service.rpc;

import lombok.Data;
import org.echoiot.server.common.msg.rpc.ToDeviceRpcRequest;
import org.echoiot.server.service.security.model.SecurityUser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;

/**
 * Created by Echo on 16.04.18.
 */
@Data
public class LocalRequestMetaData {
    private final ToDeviceRpcRequest request;
    private final SecurityUser user;
    private final DeferredResult<ResponseEntity> responseWriter;
}

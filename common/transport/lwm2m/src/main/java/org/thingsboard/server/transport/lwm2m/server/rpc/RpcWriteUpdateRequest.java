package org.thingsboard.server.transport.lwm2m.server.rpc;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RpcWriteUpdateRequest extends LwM2MRpcRequestHeader {

    private Object value;
    private String contentFormat;

}

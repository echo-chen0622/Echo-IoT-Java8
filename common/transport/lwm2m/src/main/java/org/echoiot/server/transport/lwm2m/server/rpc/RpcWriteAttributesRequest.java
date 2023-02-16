package org.echoiot.server.transport.lwm2m.server.rpc;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.echoiot.server.common.data.device.profile.lwm2m.ObjectAttributes;

@Data
@EqualsAndHashCode(callSuper = true)
public class RpcWriteAttributesRequest extends LwM2MRpcRequestHeader {

    private ObjectAttributes attributes;

}

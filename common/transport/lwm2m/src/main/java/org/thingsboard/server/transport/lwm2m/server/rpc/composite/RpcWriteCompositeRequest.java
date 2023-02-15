package org.thingsboard.server.transport.lwm2m.server.rpc.composite;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RpcWriteCompositeRequest {

    private Map<String, Object> nodes;

}

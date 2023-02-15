package org.thingsboard.server.transport.lwm2m.server.rpc.composite;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RpcReadCompositeRequest {

    private String[] keys;
    private String[] ids;

}

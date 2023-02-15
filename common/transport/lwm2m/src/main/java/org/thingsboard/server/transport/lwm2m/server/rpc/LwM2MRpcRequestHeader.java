package org.thingsboard.server.transport.lwm2m.server.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LwM2MRpcRequestHeader {

    private String key;
    private String id;
    private String contentFormat;
}

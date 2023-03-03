package org.echoiot.server.common.data.rpc;

import lombok.Data;

import java.io.Serializable;

/**
 * @author Echo
 */
@Data
public class ToDeviceRpcRequestBody implements Serializable {
    private final String method;
    private final String params;
}

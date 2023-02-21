package org.echoiot.server.common.data.rpc;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * @author Andrew Shvayka
 */
@Data
public class ToDeviceRpcRequestBody implements Serializable {
    @NotNull
    private final String method;
    @NotNull
    private final String params;
}

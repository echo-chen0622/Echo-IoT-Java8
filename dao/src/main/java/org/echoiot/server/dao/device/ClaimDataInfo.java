package org.echoiot.server.dao.device;

import lombok.Data;
import org.echoiot.server.dao.device.claim.ClaimData;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Data
public class ClaimDataInfo {

    private final boolean fromCache;
    @NotNull
    private final List<Object> key;
    @NotNull
    private final ClaimData data;

}

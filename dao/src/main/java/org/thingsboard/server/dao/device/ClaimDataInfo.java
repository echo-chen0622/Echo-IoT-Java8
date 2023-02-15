package org.thingsboard.server.dao.device;

import lombok.Data;
import org.thingsboard.server.dao.device.claim.ClaimData;

import java.util.List;

@Data
public class ClaimDataInfo {

    private final boolean fromCache;
    private final List<Object> key;
    private final ClaimData data;

}

package org.thingsboard.server.service.telemetry.cmd.v2;

import lombok.Data;
import org.thingsboard.server.common.data.query.EntityKey;

import java.util.List;

@Data
public class LatestValueCmd {

    private List<EntityKey> keys;

}

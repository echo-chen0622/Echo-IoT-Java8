package org.echoiot.server.service.telemetry.cmd.v2;

import lombok.Data;
import org.echoiot.server.common.data.query.EntityKey;

import java.util.List;

@Data
public class LatestValueCmd {

    private List<EntityKey> keys;

}

package org.echoiot.server.common.data.sync.vc.request.load;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.echoiot.server.common.data.id.EntityId;
import org.jetbrains.annotations.NotNull;

@Data
@EqualsAndHashCode(callSuper = true)
public class SingleEntityVersionLoadRequest extends VersionLoadRequest {

    private EntityId externalEntityId;

    private VersionLoadConfig config;

    @NotNull
    @Override
    public VersionLoadRequestType getType() {
        return VersionLoadRequestType.SINGLE_ENTITY;
    }

}

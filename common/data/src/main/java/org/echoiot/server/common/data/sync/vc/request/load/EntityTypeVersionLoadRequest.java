package org.echoiot.server.common.data.sync.vc.request.load;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.echoiot.server.common.data.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class EntityTypeVersionLoadRequest extends VersionLoadRequest {

    private Map<EntityType, EntityTypeVersionLoadConfig> entityTypes;

    @NotNull
    @Override
    public VersionLoadRequestType getType() {
        return VersionLoadRequestType.ENTITY_TYPE;
    }

}

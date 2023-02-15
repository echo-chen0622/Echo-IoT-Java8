package org.thingsboard.server.common.data.sync.vc.request.load;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.EntityType;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class EntityTypeVersionLoadRequest extends VersionLoadRequest {

    private Map<EntityType, EntityTypeVersionLoadConfig> entityTypes;

    @Override
    public VersionLoadRequestType getType() {
        return VersionLoadRequestType.ENTITY_TYPE;
    }

}

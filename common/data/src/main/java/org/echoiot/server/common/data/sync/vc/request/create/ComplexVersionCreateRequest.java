package org.echoiot.server.common.data.sync.vc.request.create;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.echoiot.server.common.data.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class ComplexVersionCreateRequest extends VersionCreateRequest {

    // Default sync strategy
    private SyncStrategy syncStrategy;
    private Map<EntityType, EntityTypeVersionCreateConfig> entityTypes;

    @NotNull
    @Override
    public VersionCreateRequestType getType() {
        return VersionCreateRequestType.COMPLEX;
    }

}

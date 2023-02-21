package org.echoiot.server.common.data.sync.vc.request.create;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.echoiot.server.common.data.id.EntityId;
import org.jetbrains.annotations.NotNull;

@Data
@EqualsAndHashCode(callSuper = true)
public class SingleEntityVersionCreateRequest extends VersionCreateRequest {

    private EntityId entityId;
    private VersionCreateConfig config;

    @NotNull
    @Override
    public VersionCreateRequestType getType() {
        return VersionCreateRequestType.SINGLE_ENTITY;
    }

}

package org.echoiot.server.actors.device;

import org.echoiot.server.actors.TbActor;
import org.echoiot.server.actors.TbActorId;
import org.echoiot.server.actors.TbEntityActorId;
import org.echoiot.server.actors.service.ContextBasedCreator;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.actors.ActorSystemContext;

public class DeviceActorCreator extends ContextBasedCreator {

    private final TenantId tenantId;
    private final DeviceId deviceId;

    public DeviceActorCreator(ActorSystemContext context, TenantId tenantId, DeviceId deviceId) {
        super(context);
        this.tenantId = tenantId;
        this.deviceId = deviceId;
    }

    @Override
    public TbActorId createActorId() {
        return new TbEntityActorId(deviceId);
    }

    @Override
    public TbActor createActor() {
        return new DeviceActor(context, tenantId, deviceId);
    }

}

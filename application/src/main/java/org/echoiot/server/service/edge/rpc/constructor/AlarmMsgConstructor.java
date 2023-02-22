package org.echoiot.server.service.edge.rpc.constructor;

import org.echoiot.common.util.JacksonUtil;
import org.echoiot.server.common.data.alarm.Alarm;
import org.echoiot.server.common.data.id.AssetId;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.EntityViewId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.dao.asset.AssetService;
import org.echoiot.server.dao.device.DeviceService;
import org.echoiot.server.dao.entityview.EntityViewService;
import org.echoiot.server.gen.edge.v1.AlarmUpdateMsg;
import org.echoiot.server.gen.edge.v1.UpdateMsgType;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@TbCoreComponent
public class AlarmMsgConstructor {

    @Resource
    private DeviceService deviceService;

    @Resource
    private AssetService assetService;

    @Resource
    private EntityViewService entityViewService;

    public AlarmUpdateMsg constructAlarmUpdatedMsg(TenantId tenantId, UpdateMsgType msgType, Alarm alarm) {
        @Nullable String entityName = null;
        switch (alarm.getOriginator().getEntityType()) {
            case DEVICE:
                entityName = deviceService.findDeviceById(tenantId, new DeviceId(alarm.getOriginator().getId())).getName();
                break;
            case ASSET:
                entityName = assetService.findAssetById(tenantId, new AssetId(alarm.getOriginator().getId())).getName();
                break;
            case ENTITY_VIEW:
                entityName = entityViewService.findEntityViewById(tenantId, new EntityViewId(alarm.getOriginator().getId())).getName();
                break;
        }
        AlarmUpdateMsg.Builder builder = AlarmUpdateMsg.newBuilder()
                                                                .setMsgType(msgType)
                                                                .setIdMSB(alarm.getId().getId().getMostSignificantBits())
                                                                .setIdLSB(alarm.getId().getId().getLeastSignificantBits())
                                                                .setName(alarm.getName())
                                                                .setType(alarm.getType())
                                                                .setOriginatorName(entityName)
                                                                .setOriginatorType(alarm.getOriginator().getEntityType().name())
                                                                .setSeverity(alarm.getSeverity().name())
                                                                .setStatus(alarm.getStatus().name())
                                                                .setStartTs(alarm.getStartTs())
                                                                .setEndTs(alarm.getEndTs())
                                                                .setAckTs(alarm.getAckTs())
                                                                .setClearTs(alarm.getClearTs())
                                                                .setDetails(JacksonUtil.toString(alarm.getDetails()))
                                                                .setPropagate(alarm.isPropagate())
                                                                .setPropagateToOwner(alarm.isPropagateToOwner())
                                                                .setPropagateToTenant(alarm.isPropagateToTenant());
        return builder.build();
    }

}

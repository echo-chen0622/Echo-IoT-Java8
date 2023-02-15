package org.thingsboard.server.service.state;

import org.springframework.context.ApplicationListener;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.common.msg.queue.TbCallback;

/**
 * Created by ashvayka on 01.05.18.
 */
public interface DeviceStateService extends ApplicationListener<PartitionChangeEvent> {

    void onDeviceConnect(TenantId tenantId, DeviceId deviceId);

    void onDeviceActivity(TenantId tenantId, DeviceId deviceId, long lastReportedActivityTime);

    void onDeviceDisconnect(TenantId tenantId, DeviceId deviceId);

    void onDeviceInactivityTimeoutUpdate(TenantId tenantId, DeviceId deviceId, long inactivityTimeout);

    void onQueueMsg(TransportProtos.DeviceStateServiceMsgProto proto, TbCallback bytes);

}
